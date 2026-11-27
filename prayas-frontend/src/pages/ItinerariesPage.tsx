import { StaffLayout, StaffNotice } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { ItinerariesPanel } from '../components/staff/ItinerariesPanel';
import { useStaffSession } from '../hooks/useStaffSession';

/** Plan where each approved tour goes and when. */
export function ItinerariesPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }

  return (
    <StaffLayout me={session.me} active="itineraries" title="ITINERARIES">
      {session.me.canPlanItineraries ? (
        <ItinerariesPanel />
      ) : (
        <StaffNotice
          title="Itineraries"
          message="Itineraries are planned by leads, the faculty in-charge and the Campus Tour team."
        />
      )}
    </StaffLayout>
  );
}
