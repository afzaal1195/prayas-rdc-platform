import { StaffLayout, StaffNotice } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { TeamsPanel } from '../components/staff/TeamsPanel';
import { useStaffSession } from '../hooks/useStaffSession';

/** The chain of command: who looks after each domain's jobs on each upcoming tour. */
export function TeamsPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }

  return (
    <StaffLayout me={session.me} active="teams" title="TEAMS">
      {session.me.canWorkTeams ? (
        <TeamsPanel />
      ) : (
        <StaffNotice
          title="Teams"
          message="Domain teams are set up by leads, the faculty in-charge, domain heads and coordinators."
        />
      )}
    </StaffLayout>
  );
}
