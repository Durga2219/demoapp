// src/App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';
import { Toaster } from 'react-hot-toast';
import { store, persistor } from './app/store';
import { useAuth } from './app/hooks/useAuth';
import { useGetCurrentUserQuery } from './app/api/authApi';

import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import EmailVerificationPage from './pages/EmailVerificationPage';
import Dashboard from './pages/Dashboard';
import CreateRidePage from './pages/CreateRidePage';
import SearchRidesPage from './pages/SearchRidesPage';
import MyRidesPage from './pages/MyRidesPage';
import MyBookingsPage from './pages/MyBookingsPage';
import ProfilePage from './pages/ProfilePage';
import DriverVerificationPage from './pages/DriverVerificationPage';
import VehiclesPage from './pages/VehiclesPage';
import RideDetailsPage from './pages/RideDetailsPage';
import UnauthorizedPage from './pages/UnauthorizedPage';
import DriverBookingsPage from './pages/DriverBookingsPage';
import UpdateRidePage from './pages/UpdateRidePage';
// Import your page components here
// import LoginPage from './pages/LoginPage';
// import RegisterPage from './pages/RegisterPage';
// import EmailVerificationPage from './pages/EmailVerificationPage';
// import Dashboard from './pages/Dashboard';
// import CreateRidePage from './pages/CreateRidePage';
// import SearchRidesPage from './pages/SearchRidesPage';
// import MyRidesPage from './pages/MyRidesPage';
// import MyBookingsPage from './pages/MyBookingsPage';
// import ProfilePage from './pages/ProfilePage';
// import DriverVerificationPage from './pages/DriverVerificationPage';
// import VehiclesPage from './pages/VehiclesPage';

// Protected Route Component
const ProtectedRoute = ({ children, requireDriver = false, requireVerification = false }) => {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  // Only enforce email verification when we actually have a loaded user object
  if (requireVerification && user && !user.emailVerified) return <Navigate to="/verify-email" replace />;

  if (requireDriver && user?.role === 'DRIVER' && !user?.driverVerified) {
    return <Navigate to="/driver/verify" replace />;
  }

  return children;
};

// Main App Routes Component
const AppRoutes = () => {
  const { isAuthenticated, user } = useAuth();
  // Bootstrap current user after tokens are set; skip when unauthenticated or already loaded
  const { isFetching } = useGetCurrentUserQuery(undefined, { skip: !isAuthenticated || !!user });

  // Prevent premature redirects while user profile is being fetched
  if (isAuthenticated && !user && isFetching) {
    return <div className="w-full h-screen flex items-center justify-center">Loading...</div>;
  }

  return (
    <Routes>
      {/* Public Routes */}
      <Route
        path="/login"
        element={
          isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />
        }
      />
      <Route
        path="/register"
        element={
          isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />
        }
      />
      <Route path="/verify-email" element={<EmailVerificationPage />} />
      {/* Support direct links that point to backend path on frontend origin */}
      <Route path="/api/v1/auth/verify-email" element={<EmailVerificationPage />} />

      {/* Protected Routes */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute requireVerification={true}>
            <Dashboard />
          </ProtectedRoute>
        }
      />

      {/* Driver Routes */}
      <Route
        path="/driver/verify"
        element={
          <ProtectedRoute requireVerification={true}>
            <DriverVerificationPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/rides/create"
        element={
          <ProtectedRoute requireDriver={true}>
            <CreateRidePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/rides/my-rides"
        element={
          <ProtectedRoute requireDriver={true}>
            <MyRidesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/rides/edit/:rideId"
        element={
          <ProtectedRoute requireDriver={true}>
            <UpdateRidePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/bookings/driver-bookings"
        element={
          <ProtectedRoute requireDriver={true}>
            <DriverBookingsPage />
          </ProtectedRoute>
        }
      />

      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route
        path="/vehicles"
        element={
          <ProtectedRoute requireDriver={true}>
            <VehiclesPage />
          </ProtectedRoute>
        }
      />

      {/* Passenger/General Routes */}
      <Route
        path="/rides/search"
        element={
          <ProtectedRoute requireVerification={true}>
            <SearchRidesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/rides/:rideId"
        element={
          <ProtectedRoute requireVerification={true}>
            <RideDetailsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/bookings/my-bookings"
        element={
          <ProtectedRoute requireVerification={true}>
            <MyBookingsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute requireVerification={true}>
            <ProfilePage />
          </ProtectedRoute>
        }
      />

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

// Main App Component
const App = () => {
  return (
    <Provider store={store}>
      <PersistGate loading={<div>Loading...</div>} persistor={persistor}>
        <Router>
          <div className="min-h-screen bg-gray-50">
            <AppRoutes />

            {/* Global Toast Notifications */}
            <Toaster
              position="top-right"
              toastOptions={{
                duration: 4000,
                style: {
                  background: '#363636',
                  color: '#fff',
                },
                success: {
                  iconTheme: {
                    primary: '#10B981',
                    secondary: '#fff',
                  },
                },
                error: {
                  iconTheme: {
                    primary: '#EF4444',
                    secondary: '#fff',
                  },
                },
              }}
            />
          </div>
        </Router>
      </PersistGate>
    </Provider>
  );
};

export default App;