import { StaffLayout } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { TasksPanel } from '../components/staff/TasksPanel';
import { useStaffSession } from '../hooks/useStaffSession';

/** Where each domain's tasks for upcoming tours stand. Leads see everything; others see their own domains. */
export function TasksPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }

  return (
    <StaffLayout me={session.me} active="tasks" title="TASKS">
      <TasksPanel />
    </StaffLayout>
  );
}
