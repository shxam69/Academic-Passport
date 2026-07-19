import { apiClient } from './axios';
import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// We need an unauthenticated client for the public onboarding endpoints
const publicClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getInvitations = async (params) => {
  const response = await apiClient.get('/api/v1/admin/invitations', { params });
  return response.data;
};

export const generateInvitation = async (data) => {
  const response = await apiClient.post('/api/v1/admin/invitations', data);
  return response.data;
};

export const revokeInvitation = async (id) => {
  const response = await apiClient.post(`/api/v1/admin/invitations/${id}/revoke`);
  return response.data;
};

export const manualOnboard = async (data) => {
  const response = await apiClient.post('/api/v1/admin/colleges/onboard', data);
  return response.data;
};

export const validatePublicInvitation = async (token) => {
  const response = await publicClient.get(`/api/v1/onboarding/invitations/${token}`);
  return response.data;
};

export const submitPublicOnboarding = async (token, data) => {
  const response = await publicClient.post(`/api/v1/onboarding/submit?token=${token}`, data);
  return response.data;
};
