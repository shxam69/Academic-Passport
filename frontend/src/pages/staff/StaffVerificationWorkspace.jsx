import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getVerificationDetails, getMarksheetFileBlob, updateSubjectMarks, approveMarksheet, rejectMarksheet } from '../../api/staffVerification';
import { Check, X, AlertTriangle, ArrowLeft } from 'lucide-react';

export default function StaffVerificationWorkspace() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [details, setDetails] = useState(null);
  const [fileUrl, setFileUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [rejectReason, setRejectReason] = useState('');
  const [isRejecting, setIsRejecting] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchData();
    return () => {
      if (fileUrl) URL.revokeObjectURL(fileUrl);
    };
  }, [id]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const data = await getVerificationDetails(id);
      setDetails(data);
      
      const blob = await getMarksheetFileBlob(id);
      const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      setFileUrl(url);
      
      setError(null);
    } catch (err) {
      setError('Failed to load verification workspace.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkChange = async (subjectId, newMarks) => {
    try {
      await updateSubjectMarks(id, subjectId, newMarks === '' ? null : parseInt(newMarks, 10));
      // Update local state to avoid full reload
      setDetails(prev => ({
        ...prev,
        subjects: prev.subjects.map(s => 
          s.subjectId === subjectId ? { ...s, correctedMarks: newMarks === '' ? null : parseInt(newMarks, 10) } : s
        )
      }));
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update marks');
      // Revert will happen automatically on next refresh, or we can just fetch again
      fetchData();
    }
  };

  const handleApprove = async () => {
    if (!window.confirm('Are you sure you want to approve these marks? This will make them permanent.')) return;
    
    try {
      setSaving(true);
      await approveMarksheet(id);
      alert('Marksheet approved successfully!');
      navigate('/dashboard/verifications');
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to approve marksheet');
    } finally {
      setSaving(false);
    }
  };

  const handleReject = async () => {
    if (!rejectReason) {
      alert('Please enter a rejection reason.');
      return;
    }
    
    try {
      setSaving(true);
      await rejectMarksheet(id, rejectReason);
      alert('Marksheet rejected successfully!');
      navigate('/dashboard/verifications');
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to reject marksheet');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-8 text-center">Loading workspace...</div>;
  if (error) return <div className="p-8 text-red-600">{error}</div>;
  if (!details) return null;

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col">
      {/* Header */}
      <div className="bg-white border-b px-4 py-3 flex justify-between items-center shrink-0">
        <div className="flex items-center">
          <button onClick={() => navigate('/dashboard/verifications')} className="mr-4 text-gray-500 hover:text-gray-700">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-lg font-semibold text-gray-900">Verification Workspace</h1>
            <p className="text-xs text-gray-500">
              {details.studentName} ({details.registerNumber}) - Sem {details.semesterNumber} - {details.departmentName}
            </p>
          </div>
        </div>
        <div className="flex space-x-3">
          <button 
            onClick={() => setIsRejecting(!isRejecting)}
            className="px-4 py-2 border border-red-300 text-red-700 rounded-md hover:bg-red-50 text-sm font-medium"
            disabled={saving}
          >
            Reject
          </button>
          <button 
            onClick={handleApprove}
            className="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 text-sm font-medium flex items-center"
            disabled={saving}
          >
            <Check className="w-4 h-4 mr-1" />
            Approve
          </button>
        </div>
      </div>

      {isRejecting && (
        <div className="bg-red-50 border-b border-red-200 p-4 shrink-0 flex items-center space-x-4">
          <input 
            type="text" 
            placeholder="Reason for rejection (e.g. Blurry image, wrong semester)"
            value={rejectReason}
            onChange={e => setRejectReason(e.target.value)}
            className="flex-1 p-2 border border-red-300 rounded-md"
          />
          <button 
            onClick={handleReject}
            className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm font-medium"
            disabled={saving}
          >
            Confirm Reject
          </button>
          <button 
            onClick={() => setIsRejecting(false)}
            className="px-4 py-2 text-gray-600 hover:text-gray-900 text-sm font-medium"
            disabled={saving}
          >
            Cancel
          </button>
        </div>
      )}

      {/* Split Pane */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Pane - PDF Viewer */}
        <div className="w-1/2 border-r bg-gray-100 flex flex-col">
          {fileUrl ? (
            <iframe src={fileUrl} className="w-full h-full border-0" title="Marksheet Document" />
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-500">
              Document not available
            </div>
          )}
        </div>

        {/* Right Pane - Data Correction */}
        <div className="w-1/2 bg-white overflow-y-auto p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Extracted Subjects</h2>
          
          {details.findings && details.findings.length > 0 && (
            <div className="mb-6 bg-yellow-50 border-l-4 border-yellow-400 p-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertTriangle className="h-5 w-5 text-yellow-400" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-yellow-800">Review Required Findings:</h3>
                  <div className="mt-2 text-sm text-yellow-700">
                    <ul className="list-disc pl-5 space-y-1">
                      {details.findings.map((f, i) => <li key={i}>{f}</li>)}
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="bg-white shadow overflow-hidden sm:rounded-md border border-gray-200">
            <ul className="divide-y divide-gray-200">
              {details.subjects.map((subject) => {
                const isOverMax = subject.correctedMarks > subject.maxMarks;
                return (
                  <li key={subject.subjectId} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex-1 min-w-0 pr-4">
                        <p className="text-sm font-medium text-primary-600 truncate">{subject.subjectCode}</p>
                        <p className="text-sm text-gray-500 truncate">{subject.subjectName}</p>
                        <p className="text-xs text-gray-400 mt-1">
                          Max Marks: {subject.maxMarks} | AI: {subject.aiExtractedMarks ?? 'N/A'}
                        </p>
                      </div>
                      <div className="flex-shrink-0 flex items-center">
                        <label className="mr-2 text-sm font-medium text-gray-700">Marks:</label>
                        <input
                          type="number"
                          className={`w-24 p-2 border rounded-md text-right ${isOverMax ? 'border-red-500 text-red-600 bg-red-50' : 'border-gray-300 focus:ring-primary-500 focus:border-primary-500'}`}
                          value={subject.correctedMarks ?? ''}
                          onChange={(e) => handleMarkChange(subject.subjectId, e.target.value)}
                        />
                      </div>
                    </div>
                    {isOverMax && (
                      <p className="mt-2 text-sm text-red-600 text-right">Marks cannot exceed {subject.maxMarks}</p>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
