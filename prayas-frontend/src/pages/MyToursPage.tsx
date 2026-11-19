import { MyToursPanel } from '../components/staff/MyToursPanel';
import { StaffLayout } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { useStaffSession } from '../hooks/useStaffSession';

/** Every signed-in staff member's own list of tours they are helping with. */
export function MyToursPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }

  return (
    <StaffLayout me={session.me} active="my-tours" title="MY TOURS">
      <MyToursPanel />
    </StaffLayout>
  );
}
