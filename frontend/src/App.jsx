import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './routes/ProtectedRoute';
import DashboardLayout from './layouts/DashboardLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';

function PlaceholderPage({ title }) {
  return (
    <div className="flex h-[60vh] items-center justify-center border-2 border-dashed border-gray-200 rounded-lg bg-white">
      <div className="text-center">
        <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
        <p className="mt-2 text-sm text-gray-500">This module is pending implementation.</p>
      </div>
    </div>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/login" element={<Login />} />
      
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/dashboard/institutions" element={<PlaceholderPage title="Institutions Management" />} />
          <Route path="/dashboard/students" element={<PlaceholderPage title="Student Directory" />} />
          <Route path="/dashboard/marksheets" element={<PlaceholderPage title="Marksheets & Records" />} />
          <Route path="/dashboard/verification" element={<PlaceholderPage title="Verification Requests" />} />
          <Route path="/dashboard/notifications" element={<PlaceholderPage title="Notifications" />} />
        </Route>
      </Route>

      <Route path="*" element={
        <div className="flex min-h-screen items-center justify-center bg-gray-50">
          <div className="text-center max-w-md bg-white p-8 rounded-lg shadow-sm border border-gray-200">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">404</h2>
            <p className="text-gray-600 mb-6">The page you are looking for does not exist.</p>
            <a href="/dashboard" className="text-primary-600 hover:text-primary-700 font-medium">Return to Dashboard</a>
          </div>
        </div>
      } />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}
