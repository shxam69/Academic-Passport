import { apiClient as api } from './axios';

export const getEligibleSemesters = async () => {
  const response = await api.get('/api/v1/student/marksheets/eligible-semesters');
  return response.data;
};

export const getMyMarksheets = async () => {
  const response = await api.get('/api/v1/student/marksheets');
  return response.data;
};

export const uploadMarksheet = async (semesterId, file) => {
  const formData = new FormData();
  formData.append('semesterId', semesterId);
  formData.append('file', file);
  
  const response = await api.post('/api/v1/student/marksheets', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const replaceMarksheet = async (marksheetId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await api.put(`/api/v1/student/marksheets/${marksheetId}/file`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const downloadMarksheet = async (marksheetId) => {
  const response = await api.get(`/api/v1/student/marksheets/${marksheetId}/file`, {
    responseType: 'blob', // Important for downloading files
  });
  return response.data;
};

export const getProcessingStatus = async (marksheetId) => {
  const response = await api.get(`/api/v1/student/marksheets/${marksheetId}/processing-status`);
  return response.data;
};
