import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  createAdminVenue,
  deleteAdminVenue,
  fetchAdminVenues,
  fetchAuthorities,
  updateAdminVenue,
} from '../../api/adminApi';
import { extractErrorMessage } from '../../api/errors';
import type { AdminVenue, AdminVenueInput, AuthorityContact, VenueType } from '../../api/types';
import { DeleteControl } from './DeleteControl';

const VENUE_TYPE_LABELS: Record<VenueType, string> = {
  LEGACY_ROOM: 'Legacy room',
  LIBRARY: 'Library',
  LAB: 'Lab',
  SPORTS: 'Sports',
  CLASSROOM: 'Classroom',
  OTHER: 'Other',
};
const VENUE_TYPES = Object.keys(VENUE_TYPE_LABELS) as VenueType[];

interface VenueForm {
  name: string;
  venueType: VenueType;
  department: string;
  capacity: string;
  requiresApproval: boolean;
  publicVisible: boolean;
  active: boolean;
  description: string;
  authorityContactId: string; // '' = none
}

const emptyForm: VenueForm = {
  name: '',
  venueType: 'LAB',
  department: '',
  capacity: '',
  requiresApproval: false,
  publicVisible: true,
  active: true,
  description: '',
  authorityContactId: '',
};

function toForm(v: AdminVenue): VenueForm {
  return {
    name: v.name,
    venueType: v.venueType,
    department: v.department ?? '',
    capacity: v.capacity != null ? String(v.capacity) : '',
    requiresApproval: v.requiresApproval,
    publicVisible: v.publicVisible,
    active: v.active,
    description: v.description ?? '',
    authorityContactId: v.authorityContactId != null ? String(v.authorityContactId) : '',
  };
}

function toInput(f: VenueForm): AdminVenueInput {
  return {
    name: f.name.trim(),
    venueType: f.venueType,
    department: f.department.trim() || null,
    capacity: f.capacity.trim() ? Number(f.capacity) : null,
    requiresApproval: f.requiresApproval,
    publicVisible: f.publicVisible,
    active: f.active,
    description: f.description.trim() || null,
    authorityContactId: f.authorityContactId ? Number(f.authorityContactId) : null,
  };
}

export function VenuesPanel() {
  const [venues, setVenues] = useState<AdminVenue[]>([]);
  const [authorities, setAuthorities] = useState<AuthorityContact[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [editing, setEditing] = useState<'new' | number | null>(null);
  const [form, setForm] = useState<VenueForm>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setLoadError(null);
    Promise.all([fetchAdminVenues(), fetchAuthorities()])
      .then(([v, a]) => {
        setVenues(v);
        setAuthorities(a);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const patch = (changes: Partial<VenueForm>) => setForm((prev) => ({ ...prev, ...changes }));

  const startNew = () => {
    setEditing('new');
    setForm(emptyForm);
    setSaveError(null);
  };

  const startEdit = (v: AdminVenue) => {
    setEditing(v.id);
    setForm(toForm(v));
    setSaveError(null);
  };

  const cancel = () => {
    setEditing(null);
    setSaveError(null);
  };

  const save = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      setSaveError('Venue name is required.');
      return;
    }
    if (form.capacity.trim() && (!Number.isInteger(Number(form.capacity)) || Number(form.capacity) < 1)) {
      setSaveError('Capacity must be a whole number, 1 or more (or leave it blank).');
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      if (editing === 'new') {
        await createAdminVenue(toInput(form));
      } else if (typeof editing === 'number') {
        await updateAdminVenue(editing, toInput(form));
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
      await deleteAdminVenue(editing);
      setEditing(null);
      load();
    } catch (err) {
      setSaveError(extractErrorMessage(err));
      throw err;
    }
  };

  // Offer active contacts, plus the one already chosen even if it was deactivated since.
  const selectableAuthorities = authorities.filter(
    (a) => a.active || String(a.id) === form.authorityContactId,
  );

  return (
    <div>
      <div className="admin-toolbar">
        <p>
          Places a school can be shown. Mark a venue &ldquo;needs approval&rdquo; and choose who approves it, and an
          approval task is raised for Hospitality &amp; Logistics whenever it&apos;s added to a tour.
        </p>
        {editing === null && (
          <button type="button" className="btn btn-primary" onClick={startNew}>
            Add venue
          </button>
        )}
      </div>

      {editing !== null && (
        <form className="editor-card" onSubmit={save}>
          <h2>{editing === 'new' ? 'Add a venue' : 'Edit venue'}</h2>
          <div className="field-grid">
            <div className="field">
              <label htmlFor="v-name">Name *</label>
              <input id="v-name" maxLength={150} value={form.name} onChange={(e) => patch({ name: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="v-type">Type</label>
              <select
                id="v-type"
                value={form.venueType}
                onChange={(e) => patch({ venueType: e.target.value as VenueType })}
              >
                {VENUE_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {VENUE_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="v-dept">Department</label>
              <input
                id="v-dept"
                maxLength={120}
                placeholder="e.g. Computer Science"
                value={form.department}
                onChange={(e) => patch({ department: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="v-cap">Capacity (people)</label>
              <input
                id="v-cap"
                type="number"
                min={1}
                value={form.capacity}
                onChange={(e) => patch({ capacity: e.target.value })}
              />
            </div>
            <div className="field full">
              <label htmlFor="v-desc">Description (shown to schools)</label>
              <textarea
                id="v-desc"
                rows={2}
                maxLength={2000}
                value={form.description}
                onChange={(e) => patch({ description: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="v-auth">Who approves a visit here</label>
              <select
                id="v-auth"
                value={form.authorityContactId}
                onChange={(e) => patch({ authorityContactId: e.target.value })}
              >
                <option value="">— no contact —</option>
                {selectableAuthorities.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                    {a.office ? ` (${a.office})` : ''}
                  </option>
                ))}
              </select>
              {form.requiresApproval && !form.authorityContactId && (
                <span className="hint">Add a contact so the volunteer knows who to reach.</span>
              )}
            </div>
            <div className="field">
              <label>Settings</label>
              <label className="check-row">
                <input
                  type="checkbox"
                  checked={form.requiresApproval}
                  onChange={(e) => patch({ requiresApproval: e.target.checked })}
                />
                Needs approval before a school can visit
              </label>
              <label className="check-row">
                <input
                  type="checkbox"
                  checked={form.publicVisible}
                  onChange={(e) => patch({ publicVisible: e.target.checked })}
                />
                Schools can choose it on the request form
              </label>
              <label className="check-row">
                <input type="checkbox" checked={form.active} onChange={(e) => patch({ active: e.target.checked })} />
                Active
              </label>
            </div>
          </div>
          {saveError && <div className="submit-error">{saveError}</div>}
          <div className="editor-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save venue'}
            </button>
            <button type="button" className="btn btn-secondary" onClick={cancel} disabled={saving}>
              Cancel
            </button>
            {typeof editing === 'number' && (
              <span className="push-right">
                <DeleteControl what="this venue" onConfirm={remove} />
              </span>
            )}
          </div>
        </form>
      )}

      {loading && <p>Loading venues…</p>}
      {loadError && <p className="field-error">{loadError}</p>}

      {!loading && !loadError && (
        <div style={{ overflowX: 'auto' }}>
          <table className="request-table admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Department</th>
                <th>Capacity</th>
                <th>Needs approval</th>
                <th>Approver</th>
                <th>Schools can pick</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {venues.map((v) => (
                <tr key={v.id}>
                  <td>{v.name}</td>
                  <td>{VENUE_TYPE_LABELS[v.venueType]}</td>
                  <td>{v.department ?? '—'}</td>
                  <td>{v.capacity ?? '—'}</td>
                  <td>{v.requiresApproval ? 'Yes' : '—'}</td>
                  <td>{v.authorityName ?? <span className="muted-text">—</span>}</td>
                  <td>{v.publicVisible ? 'Yes' : 'No'}</td>
                  <td>
                    <span className={`status-badge ${v.active ? 'approved' : 'neutral'}`}>
                      {v.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <button type="button" className="btn-text" onClick={() => startEdit(v)}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
              {venues.length === 0 && (
                <tr>
                  <td colSpan={9} className="muted-text">
                    No venues yet.
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
