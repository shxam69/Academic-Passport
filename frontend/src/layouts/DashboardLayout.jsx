import React, { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  Building2, 
  BookOpen, 
  FileCheck, 
  Bell, 
  LogOut, 
  Menu,
  X,
  ShieldCheck,
  GraduationCap
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { cn } from '../components/ui/Button';

export default function DashboardLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await logout();
    navigate('/login');
  };

  const navItems = [
    { name: 'Overview', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Institutions', href: '/dashboard/institutions', icon: Building2 },
    { name: 'Students', href: '/dashboard/students', icon: Users },
    { name: 'Marksheets', href: '/dashboard/marksheets', icon: BookOpen },
    { name: 'Verification', href: '/dashboard/verification', icon: FileCheck },
    { name: 'Notifications', href: '/dashboard/notifications', icon: Bell },
  ];

  const formatRole = (role) => {
    if (!role) return '';
    return role.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  };

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      {/* Mobile sidebar backdrop */}
      {isMobileMenuOpen && (
        <div 
          className="fixed inset-0 z-40 bg-gray-900/80 transition-opacity lg:hidden"
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside 
        className={cn(
          "fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-gray-200 transform transition-transform duration-200 ease-in-out lg:translate-x-0 lg:static lg:flex lg:flex-col",
          isMobileMenuOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex h-16 shrink-0 items-center px-6 border-b border-gray-200">
          <GraduationCap className="h-8 w-8 text-primary-600 mr-3" />
          <span className="text-lg font-semibold text-gray-900 tracking-tight">Academic Passport</span>
          <button 
            className="ml-auto lg:hidden text-gray-500 hover:text-gray-700"
            onClick={() => setIsMobileMenuOpen(false)}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex flex-1 flex-col overflow-y-auto px-4 py-4">
          <nav className="flex-1 space-y-1">
            {navItems.map((item) => (
              <NavLink
                key={item.name}
                to={item.href}
                end={item.href === '/dashboard'}
                onClick={() => setIsMobileMenuOpen(false)}
                className={({ isActive }) => cn(
                  "group flex items-center px-2 py-2 text-sm font-medium rounded-md transition-colors",
                  isActive 
                    ? "bg-primary-50 text-primary-700" 
                    : "text-gray-600 hover:bg-gray-50 hover:text-gray-900"
                )}
              >
                <item.icon 
                  className={cn(
                    "mr-3 h-5 w-5 flex-shrink-0 transition-colors",
                    "text-gray-400 group-hover:text-gray-500" // active state color handled by tailwind implicitly or add explicitly if needed
                  )} 
                />
                {item.name}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="border-t border-gray-200 p-4">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="h-9 w-9 rounded-full bg-primary-100 flex items-center justify-center border border-primary-200">
                <ShieldCheck className="h-5 w-5 text-primary-700" />
              </div>
            </div>
            <div className="ml-3 truncate">
              <p className="text-sm font-medium text-gray-900 truncate">{user?.email}</p>
              <p className="text-xs font-medium text-gray-500 truncate">{formatRole(user?.role)}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            disabled={isLoggingOut}
            className="mt-4 flex w-full items-center justify-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50"
          >
            <LogOut className="mr-2 h-4 w-4 text-gray-500" />
            {isLoggingOut ? 'Signing out...' : 'Sign out'}
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Mobile header */}
        <header className="flex h-16 shrink-0 items-center border-b border-gray-200 bg-white px-4 lg:hidden">
          <button
            type="button"
            className="text-gray-500 hover:text-gray-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary-500"
            onClick={() => setIsMobileMenuOpen(true)}
          >
            <span className="sr-only">Open sidebar</span>
            <Menu className="h-6 w-6" aria-hidden="true" />
          </button>
          <div className="ml-4 flex items-center">
            <GraduationCap className="h-6 w-6 text-primary-600 mr-2" />
            <span className="text-lg font-semibold text-gray-900">Academic Passport</span>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto bg-gray-50 focus:outline-none">
          <div className="py-6 sm:py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
