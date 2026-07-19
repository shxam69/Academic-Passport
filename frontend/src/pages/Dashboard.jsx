import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Building2, Users, LayoutDashboard, ArrowRight } from 'lucide-react';
import { getColleges } from '../api/colleges';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({ totalColleges: null, activeColleges: null });
  const [loading, setLoading] = useState(false);

  const formatRole = (role) => {
    if (!role) return '';
    return role.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  };

  useEffect(() => {
    if (user?.role === 'SUPER_ADMIN') {
      const fetchStats = async () => {
        setLoading(true);
        try {
          const [allColleges, activeCollegesRes] = await Promise.all([
            getColleges({ page: 0, size: 1 }),
            getColleges({ page: 0, size: 1, isActive: true })
          ]);
          setStats({
            totalColleges: allColleges.totalElements,
            activeColleges: activeCollegesRes.totalElements
          });
        } catch (error) {
          console.error("Failed to load stats", error);
        } finally {
          setLoading(false);
        }
      };
      fetchStats();
    }
  }, [user]);

  const StatCard = ({ title, value, icon: Icon, loading }) => (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center">
          <div className="p-3 rounded-full bg-primary-50 text-primary-600">
            <Icon className="h-6 w-6" />
          </div>
          <div className="ml-5">
            <p className="text-sm font-medium text-gray-500 truncate">{title}</p>
            <div className="mt-1 flex items-baseline">
              {loading ? (
                <div className="h-6 w-16 bg-gray-200 animate-pulse rounded"></div>
              ) : (
                <p className="text-2xl font-semibold text-gray-900">{value ?? '-'}</p>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900">Dashboard Overview</h1>
          <p className="mt-1 text-sm text-gray-500">
            Welcome back, {formatRole(user?.role)}.
          </p>
        </div>
      </div>

      {user?.role === 'SUPER_ADMIN' ? (
        <>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <StatCard 
              title="Total Institutions" 
              value={stats.totalColleges} 
              icon={Building2} 
              loading={loading} 
            />
            <StatCard 
              title="Active Institutions" 
              value={stats.activeColleges} 
              icon={Building2} 
              loading={loading} 
            />
            {/* System Admins stat could go here, but we don't have an endpoint for it yet */}
          </div>

          <Card className="mt-8">
            <CardContent className="p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-2">Institution Management</h3>
              <p className="text-sm text-gray-500 mb-6">
                Onboard new colleges, manage their active status, and provision initial university administrators.
              </p>
              <Button onClick={() => navigate('/admin/colleges')} className="flex items-center">
                Go to Directory
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        </>
      ) : (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <div className="px-6 py-8 sm:p-10 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-primary-50 mb-4">
              <LayoutDashboard className="h-8 w-8 text-primary-600" />
            </div>
            <h3 className="mt-2 text-lg font-semibold text-gray-900">Platform Foundation Complete</h3>
            <p className="mt-2 text-sm text-gray-500 max-w-sm mx-auto">
              You have successfully authenticated using the real backend integration. Feature modules for managing institutions, students, and academic records are pending implementation.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
