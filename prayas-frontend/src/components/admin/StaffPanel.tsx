import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  createAdminUser,
  deleteAdminUser,
  fetchAdminUsers,
  fetchDomains,
  updateAdminUser,
} from '../../api/adminApi';
import { extractErrorMessage } from '../../api/errors';
import type { AdminUser, AdminUserInput, DomainRef, DomainRole, GlobalRole, Me } from '../../api/types';
import { DOMAIN_ROLE_LABELS, GLOBAL_ROLE_LABELS } from '../../lib/roleLabels';
import { DeleteControl } from './DeleteControl';

const GLOBAL_ROLES: GlobalRole[] = ['MEMBER', 'LEAD', 'FACULTY_INCHARGE'];
const DOMAIN_ROLES: DomainRole[] = ['VOLUNTEER', 'COORDINATOR', 'HEAD'];

interface StaffForm {
  email: string;
  fullName: string;
  phone: string;
  globalRole: GlobalRole;
  canFillVolunteerSlots: boolean;
  active: boolean;
  // A person belongs to ONE domain at most, with one role in it.
  domainCode: string; // '' = not in any domain
  domainRole: DomainRole | '';
}

function toForm(u: AdminUser): StaffForm {
  const first = u.memberships[0];
  return {
    email: u.email,
    fullName: u.fullName,
    phone: u.phone ?? '',
    globalRole: u.globalRole,
    canFillVolunteerSlots: u.canFillVolunteerSlots,
    active: u.active,
    domainCode: first ? first.domainCode : '',
    domainRole: first ? first.role : '',
  };
}

function toInput(f: StaffForm): AdminUserInput {
  const memberships: { domainCode: string; role: DomainRole }[] =
    f.domainCode && f.domainRole ? [{ domainCode: f.domainCode, role: f.domainRole }] : [];
  return {
    email: f.email.trim(),
    fullName: f.fullName.trim(),
    phone: f.phone.trim() || null,
    globalRole: f.globalRole,
    canFillVolunteerSlots: f.canFillVolunteerSlots,
    active: f.active,
    memberships,
  };
}

export function StaffPanel({ me }: { me: Me }) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [domains, setDomains] = useState<DomainRef[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [editing, setEditing] = useState<'new' | number | null>(null);
  const [form, setForm] = useState<StaffForm | null>(null);
  // How many domain roles the person being edited had before the one-domain rule
  // (only ever more than 1 for accounts set up by hand in the database).
  const [existingDomainCount, setExistingDomainCount] = useState(0);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setLoadError(null);
    Promise.all([fetchAdminUsers(), fetchDomains()])
      .then(([u, d]) => {
        setUsers(u);
        setDomains(d);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const patch = (changes: Partial<StaffForm>) =>
    setForm((prev) => (prev ? { ...prev, ...changes } : prev));

  // Picking a domain defaults the role to the least powerful one; clearing the domain clears the role.
  const chooseDomain = (domainCode: string) =>
    setForm((prev) =>
      prev
        ? { ...prev, domainCode, domainRole: domainCode ? prev.domainRole || 'VOLUNTEER' : '' }
        : prev,
    );

  const startNew = () => {
    setEditing('new');
    setForm({
      email: '',
      fullName: '',
      phone: '',
      globalRole: 'MEMBER',
      canFillVolunteerSlots: false,
      active: true,
      domainCode: '',
      domainRole: '',
    });
    setExistingDomainCount(0);
    setSaveError(null);
  };

  const startEdit = (u: AdminUser) => {
    setEditing(u.id);
    setForm(toForm(u));
    setExistingDomainCount(u.memberships.length);
    setSaveError(null);
  };

  const cancel = () => {
    setEditing(null);
    setForm(null);
    setSaveError(null);
  };

  const save = async (e: FormEvent) => {
    e.preventDefault();
    if (!form) return;
    if (!form.fullName.trim()) {
      setSaveError('Name is required.');
      return;
    }
    if (editing === 'new' && !form.email.trim()) {
      setSaveError('Email is required.');
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      if (editing === 'new') {
        await createAdminUser(toInput(form));
      } else if (typeof editing === 'number') {
        await updateAdminUser(editing, toInput(form));
      }
      cancel();
      load();
    } catch (err) {
      setSaveError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (typeof editing !== 'number') return;
    setSaveError(null);
    try {
      await deleteAdminUser(editing);
      cancel();
      load();
    } catch (err) {
      setSaveError(extractErrorMessage(err));
      throw err;
    }
  };

  // You can't change your own role or deactivate yourself (the server enforces
  // this too) -- so nobody can lock themselves out of the admin screen.
  const editingSelf =
    typeof editing === 'number' && users.some((u) => u.id === editing && u.email.toLowerCase() === me.email.toLowerCase());

  return (
    <div>
      <div className="admin-toolbar">
        <p>
          Everyone who can sign in. <strong>Role</strong> is organisation-wide (lead, faculty in-charge). Each person
          can also belong to <strong>one domain</strong> with one role in it, which decides which task queues they see.
          Accounts must use an institute email address. A person can be deleted only if nothing refers to them yet;
          otherwise mark them inactive.
        </p>
        {editing === null && (
          <button type="button" className="btn btn-primary" onClick={startNew}>
            Add staff member
          </button>
        )}
      </div>

      {editing !== null && form && (
        <form className="editor-card" onSubmit={save}>
          <h2>{editing === 'new' ? 'Add a staff member' : 'Edit staff member'}</h2>
          <div className="field-grid">
            <div className="field">
              <label htmlFor="s-name">Full name *</label>
              <input
                id="s-name"
                maxLength={150}
                value={form.fullName}
                onChange={(e) => patch({ fullName: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="s-email">Institute email *</label>
              <input
                id="s-email"
                type="email"
                maxLength={255}
                disabled={editing !== 'new'}
                value={form.email}
                onChange={(e) => patch({ email: e.target.value })}
              />
              {editing !== 'new' && <span className="hint">An email can&apos;t be changed after the account exists.</span>}
            </div>
            <div className="field">
              <label htmlFor="s-phone">Phone</label>
              <input id="s-phone" maxLength={20} value={form.phone} onChange={(e) => patch({ phone: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="s-role">Role</label>
              <select
                id="s-role"
                disabled={editingSelf}
                value={form.globalRole}
                onChange={(e) => patch({ globalRole: e.target.value as GlobalRole })}
              >
                {GLOBAL_ROLES.map((r) => (
                  <option key={r} value={r}>
                    {GLOBAL_ROLE_LABELS[r]}
                  </option>
                ))}
              </select>
              {editingSelf && <span className="hint">You can&apos;t change your own role.</span>}
            </div>
            <div className="field full">
              <label className="check-row">
                <input
                  type="checkbox"
                  checked={form.canFillVolunteerSlots}
                  onChange={(e) => patch({ canFillVolunteerSlots: e.target.checked })}
                />
                Can be allocated to volunteer slots when volunteers are short
              </label>
              <label className="check-row">
                <input
                  type="checkbox"
                  disabled={editingSelf}
                  checked={form.active}
                  onChange={(e) => patch({ active: e.target.checked })}
                />
                Active (inactive accounts can&apos;t sign in)
                {editingSelf && <span className="hint">&nbsp;&mdash; you can&apos;t deactivate yourself</span>}
              </label>
            </div>
            <div className="field">
              <label htmlFor="s-domain">Domain</label>
              <select id="s-domain" value={form.domainCode} onChange={(e) => chooseDomain(e.target.value)}>
                <option value="">— no domain —</option>
                {domains.map((d) => (
                  <option key={d.code} value={d.code}>
                    {d.name}
                  </option>
                ))}
              </select>
              {existingDomainCount > 1 && (
                <span className="hint">
                  This person currently has {existingDomainCount} domain roles. A person belongs to one domain, so
                  saving keeps only the one shown here.
                </span>
              )}
            </div>
            <div className="field">
              <label htmlFor="s-domain-role">Role in domain</label>
              <select
                id="s-domain-role"
                disabled={!form.domainCode}
                value={form.domainRole}
                onChange={(e) => setForm((prev) => (prev ? { ...prev, domainRole: e.target.value as DomainRole } : prev))}
              >
                {!form.domainCode && <option value="">— choose a domain first —</option>}
                {DOMAIN_ROLES.map((r) => (
                  <option key={r} value={r}>
                    {DOMAIN_ROLE_LABELS[r]}
                  </option>
                ))}
              </select>
            </div>
          </div>
          {saveError && <div className="submit-error">{saveError}</div>}
          <div className="editor-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
            <button type="button" className="btn btn-secondary" onClick={cancel} disabled={saving}>
              Cancel
            </button>
            {typeof editing === 'number' && (
              <span className="push-right">
                <DeleteControl
                  what="this person"
                  disabled={editingSelf}
                  disabledReason="You can't delete your own account."
                  onConfirm={remove}
                />
              </span>
            )}
          </div>
        </form>
      )}

      {loading && <p>Loading staff…</p>}
      {loadError && <p className="field-error">{loadError}</p>}

      {!loading && !loadError && (
        <div style={{ overflowX: 'auto' }}>
          <table className="request-table admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Domain &amp; role</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{GLOBAL_ROLE_LABELS[u.globalRole] ?? u.globalRole}</td>
                  <td className="wrap">
                    {u.memberships.length === 0 ? (
                      <span className="muted-text">—</span>
                    ) : (
                      u.memberships.map((m) => (
                        <span className="chip" key={m.domainCode}>
                          {m.domainName} · {DOMAIN_ROLE_LABELS[m.role] ?? m.role}
                        </span>
                      ))
                    )}
                  </td>
                  <td>
                    <span className={`status-badge ${u.active ? 'approved' : 'neutral'}`}>
                      {u.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <button type="button" className="btn-text" onClick={() => startEdit(u)}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted-text">
                    No staff yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
