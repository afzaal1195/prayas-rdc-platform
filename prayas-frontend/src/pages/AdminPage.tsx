import { useState } from 'react';
import { AuthoritiesPanel } from '../components/admin/AuthoritiesPanel';
import { StaffPanel } from '../components/admin/StaffPanel';
import { VenuesPanel } from '../components/admin/VenuesPanel';
import { StaffLayout, StaffNotice } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { useStaffSession } from '../hooks/useStaffSession';

type Tab = 'venues' | 'authorities' | 'staff';

const TABS: { id: Tab; label: string }[] = [
  { id: 'venues', label: 'Venues' },
  { id: 'authorities', label: 'Approval contacts' },
  { id: 'staff', label: 'Staff & roles' },
];

export function AdminPage() {
  const session = useStaffSession();
  const [tab, setTab] = useState<Tab>('venues');

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }
  const me = session.me;

  return (
    <StaffLayout me={me} active="admin" title="Admin">
      {!me.canAdmin ? (
        <StaffNotice
          title="Admin is for leads and the faculty in-charge"
          message="Your account doesn't have access to this page. If you think it should, ask a lead to update your role."
        />
      ) : (
        <div className="admin-wrap">
          <div className="admin-tabs" role="tablist">
            {TABS.map((t) => (
              <button
                key={t.id}
                type="button"
                role="tab"
                aria-selected={tab === t.id}
                className={`admin-tab ${tab === t.id ? 'active' : ''}`}
                onClick={() => setTab(t.id)}
              >
                {t.label}
              </button>
            ))}
          </div>
          {tab === 'venues' && <VenuesPanel />}
          {tab === 'authorities' && <AuthoritiesPanel />}
          {tab === 'staff' && <StaffPanel me={me} />}
        </div>
      )}
    </StaffLayout>
  );
}
