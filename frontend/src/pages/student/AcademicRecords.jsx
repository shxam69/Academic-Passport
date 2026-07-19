import React, { useState, useEffect } from 'react';
import { getMyMarksheets, getEligibleSemesters, uploadMarksheet, replaceMarksheet, downloadMarksheet, getProcessingStatus } from '../../api/student';
import { 
  FileText, 
  Upload, 
  RefreshCw,
  CheckCircle,
  Clock,
  AlertCircle,
  Download
} from 'lucide-react';

export default function AcademicRecords() {
  const [marksheets, setMarksheets] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isReplaceMode, setIsReplaceMode] = useState(false);
  const [selectedMarksheetId, setSelectedMarksheetId] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [marksheetData, semesterData] = await Promise.all([
        getMyMarksheets(),
        getEligibleSemesters()
      ]);
      setMarksheets(marksheetData);
      setSemesters(semesterData);
      setError(null);
    } catch (err) {
      setError('Failed to load academic records.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Poll for processing status
    const pollInterval = setInterval(async () => {
      const needsPolling = marksheets.some(m => m.status.includes('awaiting') || m.status.includes('Processing...'));
      if (needsPolling) {
        // Fetch new statuses
        const updatedMarksheets = await Promise.all(marksheets.map(async (m) => {
          if (m.status.includes('awaiting') || m.status.includes('Processing...')) {
            try {
              const statusData = await getProcessingStatus(m.id);
              return { ...m, status: statusData.displayMessage };
            } catch (e) {
              return m;
            }
          }
          return m;
        }));
        
        // Only update if changed
        const hasChanges = updatedMarksheets.some((m, i) => m.status !== marksheets[i].status);
        if (hasChanges) {
          setMarksheets(updatedMarksheets);
        }
      }
    }, 5000); // every 5 seconds

    return () => clearInterval(pollInterval);
  }, [marksheets]);

  const handleDownload = async (marksheetId, semesterName) => {
    try {
      const blob = await downloadMarksheet(marksheetId);
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `marksheet_${semesterName}.pdf`); // We don't have the exact original extension here in MVP, but pdf is a good fallback, or let the browser infer.
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (err) {
      alert('Failed to download marksheet.');
    }
  };

  const getStatusIcon = (status) => {
    if (status.includes('awaiting')) return <Clock className="w-5 h-5 text-yellow-500" />;
    if (status.includes('completed')) return <CheckCircle className="w-5 h-5 text-green-500" />;
    if (status.includes('failed')) return <AlertCircle className="w-5 h-5 text-red-500" />;
    if (status.includes('Review required')) return <AlertCircle className="w-5 h-5 text-yellow-600" />;
    if (status.includes('Verified')) return <CheckCircle className="w-5 h-5 text-blue-500" />;
    return <FileText className="w-5 h-5 text-gray-500" />;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="sm:flex sm:items-center">
        <div className="sm:flex-auto">
          <h1 className="text-2xl font-semibold text-gray-900">My Academic Records</h1>
          <p className="mt-2 text-sm text-gray-700">
            Upload and manage your semester marksheets for verification and processing.
          </p>
        </div>
        <div className="mt-4 sm:mt-0 sm:ml-16 sm:flex-none">
          <button
            onClick={() => {
              setIsReplaceMode(false);
              setSelectedMarksheetId(null);
              setIsUploadModalOpen(true);
            }}
            className="inline-flex items-center justify-center rounded-md border border-transparent bg-primary-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 sm:w-auto"
          >
            <Upload className="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            Upload Marksheet
          </button>
        </div>
      </div>

      {error && (
        <div className="mt-4 bg-red-50 p-4 rounded-md">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      <div className="mt-8 flex flex-col">
        <div className="-my-2 -mx-4 overflow-x-auto sm:-mx-6 lg:-mx-8">
          <div className="inline-block min-w-full py-2 align-middle md:px-6 lg:px-8">
            <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg bg-white">
              <table className="min-w-full divide-y divide-gray-300">
                <thead className="bg-gray-50">
                  <tr>
                    <th scope="col" className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">Semester</th>
                    <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Upload Date</th>
                    <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Processing Status</th>
                    <th scope="col" className="relative py-3.5 pl-3 pr-4 sm:pr-6">
                      <span className="sr-only">Actions</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 bg-white">
                  {loading ? (
                    <tr>
                      <td colSpan="4" className="text-center py-10 text-gray-500">Loading records...</td>
                    </tr>
                  ) : marksheets.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="text-center py-10 text-gray-500">No marksheets uploaded yet.</td>
                    </tr>
                  ) : (
                    marksheets.map((marksheet) => (
                      <tr key={marksheet.id}>
                        <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">
                          {marksheet.semesterName}
                        </td>
                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                          {new Date(marksheet.uploadedAt).toLocaleDateString()}
                        </td>
                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                          <div className="flex items-center space-x-2">
                            {getStatusIcon(marksheet.status)}
                            <span>{marksheet.status}</span>
                          </div>
                        </td>
                        <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6 space-x-4">
                          <button
                            onClick={() => handleDownload(marksheet.id, marksheet.semesterName)}
                            className="text-primary-600 hover:text-primary-900 inline-flex items-center"
                          >
                            <Download className="w-4 h-4 mr-1" />
                            Download
                          </button>
                          <button
                            onClick={() => {
                              setIsReplaceMode(true);
                              setSelectedMarksheetId(marksheet.id);
                              setIsUploadModalOpen(true);
                            }}
                            className="text-gray-600 hover:text-gray-900 inline-flex items-center"
                          >
                            <RefreshCw className="w-4 h-4 mr-1" />
                            Replace
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <UploadModal 
        isOpen={isUploadModalOpen} 
        onClose={() => setIsUploadModalOpen(false)}
        onSuccess={() => {
          setIsUploadModalOpen(false);
          fetchData();
        }}
        semesters={semesters}
        isReplaceMode={isReplaceMode}
        marksheetId={selectedMarksheetId}
      />
    </div>
  );
}

function UploadModal({ isOpen, onClose, onSuccess, semesters, isReplaceMode, marksheetId }) {
  const [selectedSemester, setSelectedSemester] = useState('');
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  // Reset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setSelectedSemester('');
      setFile(null);
      setError(null);
    }
  }, [isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isReplaceMode && !selectedSemester) {
      setError('Please select a semester.');
      return;
    }
    if (!file) {
      setError('Please select a file to upload.');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      if (isReplaceMode) {
        await replaceMarksheet(marksheetId, file);
      } else {
        await uploadMarksheet(selectedSemester, file);
      }
      onSuccess();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to upload file. Please ensure it is a valid PDF, JPG, or PNG under 5MB.';
      setError(msg);
    } finally {
      setUploading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
      <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" onClick={onClose}></div>

        <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>

        <div className="inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
          <div>
            <div className="mt-3 text-center sm:mt-5">
              <h3 className="text-lg leading-6 font-medium text-gray-900" id="modal-title">
                {isReplaceMode ? 'Replace Marksheet' : 'Upload Marksheet'}
              </h3>
              <div className="mt-2">
                <p className="text-sm text-gray-500">
                  Supported formats: PDF, JPEG, PNG. Maximum size: 5MB.
                </p>
              </div>
            </div>
          </div>
          
          <form onSubmit={handleSubmit} className="mt-5 space-y-4">
            {error && (
              <div className="bg-red-50 p-3 rounded-md text-sm text-red-600 mb-4">
                {error}
              </div>
            )}

            {!isReplaceMode && (
              <div>
                <label htmlFor="semester" className="block text-sm font-medium text-gray-700">Semester</label>
                <select
                  id="semester"
                  name="semester"
                  className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm rounded-md"
                  value={selectedSemester}
                  onChange={(e) => setSelectedSemester(e.target.value)}
                  disabled={uploading}
                >
                  <option value="">Select a semester</option>
                  {semesters.map(s => (
                    <option key={s.id} value={s.id}>Semester {s.semesterNumber}</option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700">File</label>
              <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md">
                <div className="space-y-1 text-center">
                  <FileText className="mx-auto h-12 w-12 text-gray-400" />
                  <div className="flex text-sm text-gray-600 justify-center">
                    <label
                      htmlFor="file-upload"
                      className="relative cursor-pointer bg-white rounded-md font-medium text-primary-600 hover:text-primary-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-primary-500"
                    >
                      <span>{file ? file.name : 'Upload a file'}</span>
                      <input 
                        id="file-upload" 
                        name="file-upload" 
                        type="file" 
                        className="sr-only" 
                        accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                        onChange={(e) => setFile(e.target.files[0])}
                        disabled={uploading}
                      />
                    </label>
                  </div>
                  {!file && <p className="text-xs text-gray-500">PDF, PNG, JPG up to 5MB</p>}
                </div>
              </div>
            </div>

            <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
              <button
                type="submit"
                disabled={uploading}
                className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary-600 text-base font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:col-start-2 sm:text-sm disabled:opacity-50"
              >
                {uploading ? 'Processing...' : (isReplaceMode ? 'Replace' : 'Upload')}
              </button>
              <button
                type="button"
                className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:mt-0 sm:col-start-1 sm:text-sm"
                onClick={onClose}
                disabled={uploading}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
