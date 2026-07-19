import React, { useState, useEffect } from 'react';
import { getDashboardStats } from '../../api/universityAdmin';
import { useAuth } from '../../context/AuthContext';
import { Users, BookOpen, LayoutDashboard } from 'lucide-react';

const StatCard = ({ title, value, icon, bgColor, textColor }) => (
  <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 flex items-center gap-4 transition-transform hover:-translate-y-1">
    <div className={`p-4 rounded-lg ${bgColor} ${textColor}`}>
      {icon}
    </div>
    <div>
      <p className="text-sm font-medium text-gray-500 mb-1">{title}</p>
      <h3 className="text-2xl font-bold text-gray-900">{value}</h3>
    </div>
  </div>
);

const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({ totalDepartments: 0, totalStaff: 0, totalStudents: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await getDashboardStats();
        setStats(response.data);
      } catch (error) {
        console.error('Failed to fetch dashboard stats', error);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Welcome, {user?.email}</h1>
        <p className="text-gray-500">Here's an overview of your college.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatCard
          title="Total Departments"
          value={stats.totalDepartments}
          icon={<LayoutDashboard className="w-6 h-6" />}
          bgColor="bg-indigo-50"
          textColor="text-indigo-600"
        />
        <StatCard
          title="Total Staff"
          value={stats.totalStaff}
          icon={<BookOpen className="w-6 h-6" />}
          bgColor="bg-emerald-50"
          textColor="text-emerald-600"
        />
        <StatCard
          title="Total Students"
          value={stats.totalStudents}
          icon={<Users className="w-6 h-6" />}
          bgColor="bg-blue-50"
          textColor="text-blue-600"
        />
      </div>
    </div>
  );
};

export default Dashboard;
