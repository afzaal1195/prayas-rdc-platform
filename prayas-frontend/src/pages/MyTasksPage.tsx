import { MyTasksPanel } from '../components/staff/MyTasksPanel';
import { StaffLayout } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { useStaffSession } from '../hooks/useStaffSession';

/** The signed-in person's own jobs on upcoming tours, where they tick off their part. */
export function MyTasksPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }

  return (
    <StaffLayout me={session.me} active="my-tasks" title="MY TASKS">
      <MyTasksPanel />
    </StaffLayout>
  );
}
