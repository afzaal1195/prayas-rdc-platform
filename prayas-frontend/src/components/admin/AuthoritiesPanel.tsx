import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createAuthority, deleteAuthority, fetchAuthorities, updateAuthority } from '../../api/adminApi';
import { extractErrorMessage } from '../../api/errors';
import type { AuthorityContact, AuthorityContactInput } from '../../api/types';
import { DeleteControl } from './DeleteControl';

interface AuthorityForm {
  name: string;
  office: string;
  phone: string;
  email: string;
  officeHours: string;
  preferredContact: string;
  notes: string;
  active: boolean;
}

const emptyForm: AuthorityForm = {
  name: '',
  office: '',
  phone: '',
  email: '',
  officeHours: '',
  preferredContact: '',
  notes: '',
  active: true,
};

function toForm(a: AuthorityContact): AuthorityForm {
  return {
    name: a.name,
    office: a.office ?? '',
    phone: a.phone ?? '',
    email: a.email ?? '',
    officeHours: a.officeHours ?? '',
    preferredContact: a.preferredContact ?? '',
    notes: a.notes ?? '',
    active: a.active,
  };
}

function toInput(f: AuthorityForm): AuthorityContactInput {
  return {
    name: f.name.trim(),
    office: f.office.trim() || null,
    phone: f.phone.trim() || null,
    email: f.email.trim() || null,
    officeHours: f.officeHours.trim() || null,
    preferredContact: f.preferredContact.trim() || null,
    notes: f.notes.trim() || null,
    active: f.active,
  };
}

export function AuthoritiesPanel() {
  const [authorities, setAuthorities] = useState<AuthorityContact[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [editing, setEditing] = useState<'new' | number | null>(null);
  const [form, setForm] = useState<AuthorityForm>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setLoadError(null);
    fetchAuthorities()
      .then(setAuthorities)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const patch = (changes: Partial<AuthorityForm>) => setForm((prev) => ({ ...prev, ...changes }));

  const startNew = () => {
    setEditing('new');
    setForm(emptyForm);
    setSaveError(null);
  };

  const startEdit = (a: AuthorityContact) => {
    setEditing(a.id);
    setForm(toForm(a));
    setSaveError(null);
  };

  const cancel = () => {
    setEditing(null);
    setSaveError(null);
  };

  const save = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      setSaveError('Name is required.');
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      if (editing === 'new') {
        await createAuthority(toInput(form));
      } else if (typeof editing === 'number') {
        await updateAuthority(editing, toInput(form));
      }
      setEditing(null);
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
      await deleteAuthority(editing);
      setEditing(null);
      load();
    } catch (err) {
      setSaveError(extractErrorMessage(err));
      throw err;
    }
  };

  return (
    <div>
      <div className="admin-toolbar">
        <p>
          The people and offices that have to say yes before a visit &mdash; department heads, the booking office,
          the hostel office, the mess in-charge. A volunteer picking up an approval task works from this directory.
          A contact can be deleted only while no venue relies on it as an approver; otherwise mark it inactive.
        </p>
        {editing === null && (
          <button type="button" className="btn btn-primary" onClick={startNew}>
            Add contact
          </button>
        )}
      </div>

      {editing !== null && (
        <form className="editor-card" onSubmit={save}>
          <h2>{editing === 'new' ? 'Add a contact' : 'Edit contact'}</h2>
          <div className="field-grid">
            <div className="field">
              <label htmlFor="a-name">Name *</label>
              <input
                id="a-name"
                maxLength={150}
                placeholder="e.g. Head of Department, CSE"
                value={form.name}
                onChange={(e) => patch({ name: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="a-office">Office</label>
              <input
                id="a-office"
                maxLength={150}
                placeholder="e.g. Department of Computer Science"
                value={form.office}
                onChange={(e) => patch({ office: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="a-phone">Phone</label>
              <input id="a-phone" maxLength={30} value={form.phone} onChange={(e) => patch({ phone: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="a-email">Email</label>
              <input
                id="a-email"
                type="email"
                maxLength={255}
                value={form.email}
                onChange={(e) => patch({ email: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="a-hours">Office hours</label>
              <input
                id="a-hours"
                maxLength={150}
                placeholder="e.g. Mon–Fri 10:00–17:00"
                value={form.officeHours}
                onChange={(e) => patch({ officeHours: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="a-pref">Best way to reach them</label>
              <input
                id="a-pref"
                list="preferred-contact-options"
                maxLength={30}
                value={form.preferredContact}
                onChange={(e) => patch({ preferredContact: e.target.value })}
              />
              <datalist id="preferred-contact-options">
                <option value="Phone" />
                <option value="In person" />
                <option value="Email" />
                <option value="WhatsApp" />
              </datalist>
            </div>
            <div className="field full">
              <label htmlFor="a-notes">Notes</label>
              <textarea
                id="a-notes"
                rows={2}
                maxLength={2000}
                value={form.notes}
                onChange={(e) => patch({ notes: e.target.value })}
              />
            </div>
            <div className="field full">
              <label className="check-row">
                <input type="checkbox" checked={form.active} onChange={(e) => patch({ active: e.target.checked })} />
                Active
              </label>
            </div>
          </div>
          {saveError && <div className="submit-error">{saveError}</div>}
          <div className="editor-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save contact'}
            </button>
            <button type="button" className="btn btn-secondary" onClick={cancel} disabled={saving}>
              Cancel
            </button>
            {typeof editing === 'number' && (
              <span className="push-right">
                <DeleteControl what="this contact" onConfirm={remove} />
              </span>
            )}
          </div>
        </form>
      )}

      {loading && <p>Loading contacts…</p>}
      {loadError && <p className="field-error">{loadError}</p>}

      {!loading && !loadError && (
        <div style={{ overflowX: 'auto' }}>
          <table className="request-table admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Office</th>
                <th>Phone</th>
                <th>Email</th>
                <th>Hours</th>
                <th>Best way</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {authorities.map((a) => (
                <tr key={a.id}>
                  <td>{a.name}</td>
                  <td>{a.office ?? '—'}</td>
                  <td>{a.phone ?? '—'}</td>
                  <td>{a.email ?? '—'}</td>
                  <td>{a.officeHours ?? '—'}</td>
                  <td>{a.preferredContact ?? '—'}</td>
                  <td>
                    <span className={`status-badge ${a.active ? 'approved' : 'neutral'}`}>
                      {a.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <button type="button" className="btn-text" onClick={() => startEdit(a)}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
              {authorities.length === 0 && (
                <tr>
                  <td colSpan={8} className="muted-text">
                    No contacts yet. Add the department heads and offices you usually need approval from.
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
