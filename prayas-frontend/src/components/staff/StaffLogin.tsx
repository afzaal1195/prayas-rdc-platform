import { loginUrl } from '../../api/staffApi';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { StaffHeaderLogos } from './StaffHeaderLogos';
import '../../styles/form.css';
import '../../styles/dashboard.css';

export function StaffLogin({ authError }: { authError: string | null }) {
  useDocumentTitle('Sign in — PRAYAS Staff');
  return (
    <div className="page">
      <header className="page-header">
        <StaffHeaderLogos />
        <h1>Staff dashboard</h1>
      </header>
      <div className="dashboard-login">
        <p>Sign in with your @iith.ac.in Google account to review campus tour requests.</p>
        {authError && <p className="field-error">{authError}</p>}
        <a className="btn btn-primary" href={loginUrl()}>
          Sign in with Google
        </a>
      </div>
    </div>
  );
}
