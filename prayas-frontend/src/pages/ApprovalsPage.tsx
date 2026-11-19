import { ApprovalsPanel } from '../components/staff/ApprovalsPanel';
import { StaffLayout, StaffNotice } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { useStaffSession } from '../hooks/useStaffSession';

/** Venue and mess approvals: who still has to say yes before upcoming tours can go ahead. */
export function ApprovalsPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }

  return (
    <StaffLayout me={session.me} active="approvals" title="APPROVALS">
      {session.me.canWorkApprovals ? (
        <ApprovalsPanel />
      ) : (
        <StaffNotice
          title="Approvals"
          message="Venue and mess approvals are handled by Hospitality & Logistics, the Campus Tour team and leads."
        />
      )}
    </StaffLayout>
  );
}
