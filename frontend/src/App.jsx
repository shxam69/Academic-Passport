import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { RoleProtectedRoute } from './routes/RoleProtectedRoute';
import DashboardLayout from './layouts/DashboardLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Colleges from './pages/admin/Colleges';
import CollegeDetails from './pages/admin/CollegeDetails';
import PublicOnboarding from './pages/onboarding/PublicOnboarding';
import AcademicRecords from './pages/student/AcademicRecords';
import StaffVerificationQueue from './pages/staff/StaffVerificationQueue';
import StaffVerificationWorkspace from './pages/staff/StaffVerificationWorkspace';

// University Admin pages
import UADashboard from './pages/university-admin/Dashboard';
import UADepartments from './pages/university-admin/Departments';
import UAAcademicStructure from './pages/university-admin/AcademicStructure';
import UAStaff from './pages/university-admin/Staff';
import UAStudents from './pages/university-admin/Students';

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
      <Route path="/onboard/:token" element={<PublicOnboarding />} />
      
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/dashboard/institutions" element={<PlaceholderPage title="Institutions Management" />} />
          <Route path="/dashboard/students" element={<PlaceholderPage title="Student Directory" />} />
          <Route path="/dashboard/marksheets" element={<AcademicRecords />} />
          <Route path="/dashboard/verification" element={<PlaceholderPage title="Verification Requests" />} />
          <Route path="/dashboard/notifications" element={<PlaceholderPage title="Notifications" />} />
          
          <Route path="student/marksheets" element={<RoleProtectedRoute allowedRoles={['STUDENT']}><AcademicRecords /></RoleProtectedRoute>} />
              
          {/* STAFF Routes */}
          <Route path="/dashboard/verifications" element={<RoleProtectedRoute allowedRoles={['STAFF']}><StaffVerificationQueue /></RoleProtectedRoute>} />
          <Route path="/dashboard/verifications/:id" element={<RoleProtectedRoute allowedRoles={['STAFF']}><StaffVerificationWorkspace /></RoleProtectedRoute>} />
          
          <Route path="/admin/colleges" element={<Colleges />} />
          <Route path="/admin/colleges/:id" element={<CollegeDetails />} />

          {/* University Admin Routes */}
          <Route path="/university-admin/dashboard" element={<UADashboard />} />
          <Route path="/university-admin/departments" element={<UADepartments />} />
          <Route path="/university-admin/academic-structure" element={<UAAcademicStructure />} />
          <Route path="/university-admin/staff" element={<UAStaff />} />
          <Route path="/university-admin/students" element={<UAStudents />} />
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
