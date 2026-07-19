import { apiClient as api } from './axios';

export const getVerificationQueue = async () => {
  const response = await api.get('/api/v1/staff/verifications/marksheets');
  return response.data;
};

export const getVerificationDetails = async (id) => {
  const response = await api.get(`/api/v1/staff/verifications/marksheets/${id}`);
  return response.data;
};

export const getMarksheetFileBlob = async (id) => {
  const response = await api.get(`/api/v1/staff/verifications/marksheets/${id}/file`, {
    responseType: 'blob'
  });
  return response.data;
};

export const updateSubjectMarks = async (id, subjectId, marks) => {
  const response = await api.put(`/api/v1/staff/verifications/marksheets/${id}/subjects/${subjectId}`, { marks });
  return response.data;
};

export const approveMarksheet = async (id) => {
  const response = await api.post(`/api/v1/staff/verifications/marksheets/${id}/approve`);
  return response.data;
};

export const rejectMarksheet = async (id, reason) => {
  const response = await api.post(`/api/v1/staff/verifications/marksheets/${id}/reject`, { reason });
  return response.data;
};
