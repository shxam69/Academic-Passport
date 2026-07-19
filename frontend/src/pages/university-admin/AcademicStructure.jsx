import React, { useState, useEffect } from 'react';
import { getDepartments, getSemesters, createSemester, getSubjects, createSubject, updateSubject } from '../../api/universityAdmin';
import { Plus, ChevronRight, Edit2 } from 'lucide-react';

const AcademicStructure = () => {
  const [departments, setDepartments] = useState([]);
  const [selectedDept, setSelectedDept] = useState(null);
  const [semesters, setSemesters] = useState([]);
  const [selectedSemester, setSelectedSemester] = useState(null);
  const [subjects, setSubjects] = useState([]);

  // Modals
  const [isSemesterModalOpen, setIsSemesterModalOpen] = useState(false);
  const [newSemesterNum, setNewSemesterNum] = useState('');
  
  const [isSubjectModalOpen, setIsSubjectModalOpen] = useState(false);
  const [currentSubject, setCurrentSubject] = useState(null);
  const [subjectForm, setSubjectForm] = useState({ subjectCode: '', subjectName: '', maxMarks: 100 });
  const [error, setError] = useState('');

  useEffect(() => {
    // Load departments initially (unpaginated for dropdown-like usage, we fetch first page of 100 for MVP)
    getDepartments({ page: 0, size: 100 }).then(res => setDepartments(res.data.content));
  }, []);

  const handleDeptSelect = async (dept) => {
    setSelectedDept(dept);
    setSelectedSemester(null);
    setSubjects([]);
    try {
      const res = await getSemesters(dept.id);
      setSemesters(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSemesterSelect = async (sem) => {
    setSelectedSemester(sem);
    try {
      const res = await getSubjects(sem.id);
      setSubjects(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreateSemester = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await createSemester(selectedDept.id, { semesterNumber: parseInt(newSemesterNum) });
      setIsSemesterModalOpen(false);
      setNewSemesterNum('');
      // Refresh semesters
      const res = await getSemesters(selectedDept.id);
      setSemesters(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create semester');
    }
  };

  const openSubjectModal = (sub = null) => {
    setCurrentSubject(sub);
    setSubjectForm(sub ? { subjectCode: sub.subjectCode, subjectName: sub.subjectName, maxMarks: sub.maxMarks } : { subjectCode: '', subjectName: '', maxMarks: 100 });
    setError('');
    setIsSubjectModalOpen(true);
  };

  const handleSaveSubject = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (currentSubject) {
        await updateSubject(currentSubject.id, subjectForm);
      } else {
        await createSubject(selectedSemester.id, subjectForm);
      }
      setIsSubjectModalOpen(false);
      // Refresh subjects
      const res = await getSubjects(selectedSemester.id);
      setSubjects(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save subject');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Academic Structure</h1>
        <p className="text-gray-500">Configure semesters and subjects for your departments</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Departments Column */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="p-4 border-b border-gray-100 bg-gray-50">
            <h3 className="font-semibold text-gray-900">1. Select Department</h3>
          </div>
          <div className="divide-y divide-gray-100 max-h-[600px] overflow-y-auto">
            {departments.length === 0 ? (
              <p className="p-4 text-gray-500 text-sm">No departments available.</p>
            ) : (
              departments.map(dept => (
                <button
                  key={dept.id}
                  onClick={() => handleDeptSelect(dept)}
                  className={`w-full text-left p-4 flex items-center justify-between transition-colors ${
                    selectedDept?.id === dept.id ? 'bg-indigo-50 border-l-4 border-indigo-600' : 'hover:bg-gray-50'
                  }`}
                >
                  <div>
                    <p className="font-medium text-gray-900">{dept.name}</p>
                    <p className="text-xs text-gray-500">{dept.code}</p>
                  </div>
                  <ChevronRight className={`text-gray-400 ${selectedDept?.id === dept.id ? 'text-indigo-600' : ''}`} />
                </button>
              ))
            )}
          </div>
        </div>

        {/* Semesters Column */}
        <div className={`bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden ${!selectedDept ? 'opacity-50 pointer-events-none' : ''}`}>
          <div className="p-4 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
            <h3 className="font-semibold text-gray-900">2. Select Semester</h3>
            {selectedDept && (
              <button
                onClick={() => { setError(''); setIsSemesterModalOpen(true); }}
                className="text-indigo-600 hover:text-indigo-700 text-sm font-medium flex items-center gap-1"
              >
                <Plus /> Add
              </button>
            )}
          </div>
          <div className="divide-y divide-gray-100 max-h-[600px] overflow-y-auto">
            {!selectedDept ? (
              <p className="p-4 text-gray-400 text-sm">Select a department first.</p>
            ) : semesters.length === 0 ? (
              <p className="p-4 text-gray-500 text-sm">No semesters configured.</p>
            ) : (
              semesters.sort((a,b) => a.semesterNumber - b.semesterNumber).map(sem => (
                <button
                  key={sem.id}
                  onClick={() => handleSemesterSelect(sem)}
                  className={`w-full text-left p-4 flex items-center justify-between transition-colors ${
                    selectedSemester?.id === sem.id ? 'bg-indigo-50 border-l-4 border-indigo-600' : 'hover:bg-gray-50'
                  }`}
                >
                  <p className="font-medium text-gray-900">Semester {sem.semesterNumber}</p>
                  <ChevronRight className={`text-gray-400 ${selectedSemester?.id === sem.id ? 'text-indigo-600' : ''}`} />
                </button>
              ))
            )}
          </div>
        </div>

        {/* Subjects Column */}
        <div className={`bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden ${!selectedSemester ? 'opacity-50 pointer-events-none' : ''}`}>
          <div className="p-4 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
            <h3 className="font-semibold text-gray-900">3. Subjects</h3>
            {selectedSemester && (
              <button
                onClick={() => openSubjectModal()}
                className="text-indigo-600 hover:text-indigo-700 text-sm font-medium flex items-center gap-1"
              >
                <Plus /> Add
              </button>
            )}
          </div>
          <div className="divide-y divide-gray-100 max-h-[600px] overflow-y-auto">
            {!selectedSemester ? (
              <p className="p-4 text-gray-400 text-sm">Select a semester first.</p>
            ) : subjects.length === 0 ? (
              <p className="p-4 text-gray-500 text-sm">No subjects configured.</p>
            ) : (
              subjects.map(sub => (
                <div key={sub.id} className="p-4 hover:bg-gray-50 group transition-colors">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium text-gray-900">{sub.subjectName}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-xs font-mono bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{sub.subjectCode}</span>
                        <span className="text-xs text-gray-500">Max: {sub.maxMarks}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => openSubjectModal(sub)}
                      className="text-gray-400 hover:text-indigo-600 opacity-0 group-hover:opacity-100 transition-all"
                    >
                      <Edit2 />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

      </div>

      {/* Semester Modal */}
      {isSemesterModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <h2 className="text-xl font-bold mb-4">Add Semester</h2>
            {error && <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">{error}</div>}
            <form onSubmit={handleCreateSemester} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Semester Number</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={newSemesterNum}
                  onChange={(e) => setNewSemesterNum(e.target.value)}
                  className="w-full px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. 1"
                />
              </div>
              <div className="flex justify-end gap-3 mt-6">
                <button type="button" onClick={() => setIsSemesterModalOpen(false)} className="px-4 py-2 text-gray-600">Cancel</button>
                <button type="submit" className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Subject Modal */}
      {isSubjectModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
            <h2 className="text-xl font-bold mb-4">{currentSubject ? 'Edit Subject' : 'Add Subject'}</h2>
            {error && <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">{error}</div>}
            <form onSubmit={handleSaveSubject} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Subject Code</label>
                <input
                  type="text"
                  required
                  value={subjectForm.subjectCode}
                  onChange={(e) => setSubjectForm({ ...subjectForm, subjectCode: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. CS101"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Subject Name</label>
                <input
                  type="text"
                  required
                  value={subjectForm.subjectName}
                  onChange={(e) => setSubjectForm({ ...subjectForm, subjectName: e.target.value })}
                  className="w-full px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. Introduction to Programming"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Max Marks</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={subjectForm.maxMarks}
                  onChange={(e) => setSubjectForm({ ...subjectForm, maxMarks: parseInt(e.target.value) })}
                  className="w-full px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="flex justify-end gap-3 mt-6">
                <button type="button" onClick={() => setIsSubjectModalOpen(false)} className="px-4 py-2 text-gray-600">Cancel</button>
                <button type="submit" className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AcademicStructure;
