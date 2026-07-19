import { apiClient } from './axios';

export const getColleges = async (params) => {
  const response = await apiClient.get('/api/v1/admin/colleges', { params });
  return response.data;
};

export const getCollegeDetails = async (id) => {
  const response = await apiClient.get(`/api/v1/admin/colleges/${id}`);
  return response.data;
};

export const createCollege = async (data) => {
  const response = await apiClient.post('/api/v1/admin/colleges', data);
  return response.data;
};

export const updateCollegeStatus = async (id, isActive) => {
  const response = await apiClient.patch(`/api/v1/admin/colleges/${id}/status`, { isActive });
  return response.data;
};

export const createUniversityAdmin = async (id, data) => {
  const response = await apiClient.post(`/api/v1/admin/colleges/${id}/admins`, data);
  return response.data;
};
