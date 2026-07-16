import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { apiClient } from '../api/axios';
import { setAccessToken, clearAccessToken } from '../auth/tokenManager';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  
  const handleLogoutState = useCallback(() => {
    setUser(null);
    clearAccessToken();
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      handleLogoutState();
    };
    
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [handleLogoutState]);

  const fetchMe = async () => {
    try {
      const response = await apiClient.get('/api/auth/me');
      setUser(response.data);
    } catch (error) {
      handleLogoutState();
    }
  };

  const login = async (email, password) => {
    const response = await apiClient.post('/api/auth/login', { email, password });
    const { accessToken, user: userData } = response.data;
    setAccessToken(accessToken);
    setUser(userData);
  };

  const logout = async () => {
    try {
      await apiClient.post('/api/auth/logout');
    } catch (error) {
      console.error("Logout failed on server, cleaning up locally.", error);
    } finally {
      handleLogoutState();
    }
  };

  const restoreSession = useCallback(async () => {
    setIsLoading(true);
    try {
      const { data } = await apiClient.post('/api/auth/refresh');
      setAccessToken(data.accessToken);
      // Wait for fetchMe to successfully get user data
      const meResponse = await apiClient.get('/api/auth/me');
      setUser(meResponse.data);
    } catch (error) {
      // 401 on refresh is normal if the cookie is expired/absent
      handleLogoutState();
    } finally {
      setIsLoading(false);
    }
  }, [handleLogoutState]);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  const value = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
