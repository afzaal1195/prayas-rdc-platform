import { useEffect, useState } from 'react';
import { actAs, fetchDevUsers, stopActing } from '../../api/adminApi';
import type { DevUser, Me } from '../../api/types';

/**
 * LOCAL TESTING ONLY (the backend only exposes this under the "local"
 * profile). Lets a lead/faculty member view the app as another staff member
 * -- a volunteer, a Hospitality coordinator -- without several Google
 * accounts. Switching reloads the page so every screen starts fresh as the
 * new identity.
 */
export function DevSwitcher({ me }: { me: Me }) {
  const [users, setUsers] = useState<DevUser[]>([]);
  const [error, setError] = useState<string | null>(null);

  const realEmail = (me.actingAs ? me.actingAs.realEmail : me.email).toLowerCase();

  useEffect(() => {
    fetchDevUsers()
      .then(setUsers)
      .catch(() => setError("Couldn't load the list"));
  }, []);

  const switchTo = async (email: string) => {
    try {
      if (email) {
        await actAs(email);
      } else {
        await stopActing();
      }
      window.location.reload();
    } catch {
      setError("Couldn't switch");
    }
  };

  return (
    <label className="dev-switch">
      <span>Test as</span>
      <select value={me.actingAs ? me.email : ''} onChange={(e) => switchTo(e.target.value)}>
        <option value="">Myself</option>
        {users
          .filter((u) => u.email.toLowerCase() !== realEmail)
          .map((u) => (
            <option key={u.email} value={u.email}>
              {u.fullName}
              {u.domainRoles.length > 0 ? ` — ${u.domainRoles.join(', ')}` : ` — ${u.globalRole}`}
            </option>
          ))}
      </select>
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}
