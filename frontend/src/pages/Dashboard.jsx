import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/Button';

export default function Dashboard() {
  const { user } = useAuth();
  
  const formatRole = (role) => {
    if (!role) return '';
    return role.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900">Dashboard Overview</h1>
          <p className="mt-1 text-sm text-gray-500">
            Welcome back to Academic Passport, {formatRole(user?.role)}.
          </p>
        </div>
      </div>

      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
        <div className="px-6 py-8 sm:p-10 text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-primary-50 mb-4">
            <LayoutDashboard className="h-8 w-8 text-primary-600" />
          </div>
          <h3 className="mt-2 text-lg font-semibold text-gray-900">Platform Foundation Complete</h3>
          <p className="mt-2 text-sm text-gray-500 max-w-sm mx-auto">
            You have successfully authenticated using the real backend integration. Feature modules for managing institutions, students, and academic records are pending implementation.
          </p>
          
          <div className="mt-6 flex justify-center gap-3">
            <Button variant="secondary" disabled>
              Configure Profile
            </Button>
            <Button disabled>
              Add Institution
            </Button>
          </div>
        </div>
        <div className="bg-gray-50 px-6 py-4 border-t border-gray-200">
          <div className="flex items-start">
            <div className="flex-shrink-0">
              <AlertCircle className="h-5 w-5 text-blue-400" aria-hidden="true" />
            </div>
            <div className="ml-3 flex-1 md:flex md:justify-between">
              <p className="text-sm text-blue-700">
                Your current session is actively managed via HttpOnly secure refresh cookies and in-memory access tokens.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
