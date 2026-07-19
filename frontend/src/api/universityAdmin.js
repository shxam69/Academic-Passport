import { apiClient } from './axios';

const BASE_URL = '/api/v1/university-admin';

// Dashboard
export const getDashboardStats = () => apiClient.get(`${BASE_URL}/dashboard/stats`);

// Departments
export const getDepartments = (params) => apiClient.get(`${BASE_URL}/departments`, { params });
export const createDepartment = (data) => apiClient.post(`${BASE_URL}/departments`, data);
export const updateDepartment = (id, data) => apiClient.put(`${BASE_URL}/departments/${id}`, data);
export const deleteDepartment = (id) => apiClient.delete(`${BASE_URL}/departments/${id}`);

// Academic Structure
export const getSemesters = (departmentId) => apiClient.get(`${BASE_URL}/departments/${departmentId}/semesters`);
export const createSemester = (departmentId, data) => apiClient.post(`${BASE_URL}/departments/${departmentId}/semesters`, data);

export const getSubjects = (semesterId) => apiClient.get(`${BASE_URL}/semesters/${semesterId}/subjects`);
export const createSubject = (semesterId, data) => apiClient.post(`${BASE_URL}/semesters/${semesterId}/subjects`, data);
export const updateSubject = (subjectId, data) => apiClient.put(`${BASE_URL}/subjects/${subjectId}`, data);

// Staff
export const getStaff = (params) => apiClient.get(`${BASE_URL}/staff`, { params });
export const createStaff = (data) => apiClient.post(`${BASE_URL}/staff`, data);
export const updateStaffStatus = (id, isActive) => apiClient.patch(`${BASE_URL}/staff/${id}/status`, { isActive });

// Students
export const getStudents = (params) => apiClient.get(`${BASE_URL}/students`, { params });
export const createStudent = (data) => apiClient.post(`${BASE_URL}/students`, data);
export const updateStudentStatus = (id, isActive) => apiClient.patch(`${BASE_URL}/students/${id}/status`, { isActive });
