import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { stopActing } from '../../api/adminApi';
import type { Me } from '../../api/types';
import { DOMAIN_ROLE_LABELS, GLOBAL_ROLE_LABELS } from '../../lib/roleLabels';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { DevSwitcher } from './DevSwitcher';
import { StaffHeaderLogos } from './StaffHeaderLogos';
import '../../styles/form.css';
import '../../styles/dashboard.css';
import '../../styles/admin.css';

/** e.g. "Lead", or "Member · Coordinator, Hospitality & Logistics". */
export function describeRoles(me: Me): string {
  const parts: string[] = [GLOBAL_ROLE_LABELS[me.globalRole] ?? me.globalRole];
  for (const r of me.domainRoles) {
    parts.push(`${DOMAIN_ROLE_LABELS[r.role] ?? r.role}, ${r.domainName}`);
  }
  return parts.join(' · ');
}

interface Props {
  me: Me;
  active: 'requests' | 'admin';
  title: string;
  children: ReactNode;
}

/** The frame shared by every signed-in staff page: header, who-am-I bar, navigation. */
export function StaffLayout({ me, active, title, children }: Props) {
  useDocumentTitle(`${title} — PRAYAS Staff`);
  const showDevSwitch = me.devTools && (me.canAdmin || me.actingAs !== null);

  const stopTesting = async () => {
    try {
      await stopActing();
    } finally {
      window.location.reload();
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <StaffHeaderLogos />
        <h1>{title}</h1>
        <button type="button" className="header-logout" onClick={() => (window.location.href = '/logout')}>
          Log out
        </button>
      </header>

      <div className="dashboard-toolbar">
        <span className="who">
          Signed in as <strong>{me.name}</strong> ({describeRoles(me)})
        </span>
        <nav className="staff-nav" aria-label="Staff sections">
          <Link to="/staff" className={active === 'requests' ? 'active' : ''}>
            Tour requests
          </Link>
          {me.canAdmin && (
            <Link to="/staff/admin" className={active === 'admin' ? 'active' : ''}>
              Admin
            </Link>
          )}
        </nav>
        {showDevSwitch ? <DevSwitcher me={me} /> : <span />}
      </div>

      {me.actingAs && (
        <div className="acting-banner" role="status">
          <span>
            Testing as <strong>{me.name}</strong> — you&apos;re really signed in as{' '}
            <strong>{me.actingAs.realName}</strong>.
          </span>
          <button type="button" className="btn-reset" onClick={stopTesting}>
            Back to myself
          </button>
        </div>
      )}

      {children}
    </div>
  );
}

/** A friendly "you can't see this" panel, for staff whose role doesn't include a page. */
export function StaffNotice({ title, message }: { title: string; message: string }) {
  return (
    <div className="notice-card">
      <h2>{title}</h2>
      <p>{message}</p>
    </div>
  );
}
