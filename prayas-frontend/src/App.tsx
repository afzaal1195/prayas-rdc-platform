import { Routes, Route, Navigate } from 'react-router-dom';
import { RequestTourPage } from './pages/RequestTourPage';
import { StaffDashboardPage } from './pages/StaffDashboardPage';
import { AdminPage } from './pages/AdminPage';
import { TourStatusPage } from './pages/TourStatusPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<RequestTourPage />} />
      <Route path="/staff" element={<StaffDashboardPage />} />
      <Route path="/staff/admin" element={<AdminPage />} />
      <Route path="/status/:token" element={<TourStatusPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
