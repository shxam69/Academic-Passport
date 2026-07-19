import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCollegeDetails, updateCollegeStatus, createUniversityAdmin } from '../../api/colleges';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '../../components/ui/Card';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../components/ui/Table';
import Badge from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import Modal from '../../components/ui/Modal';

const CollegeDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [college, setCollege] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [statusLoading, setStatusLoading] = useState(false);

  const [isAdminModalOpen, setIsAdminModalOpen] = useState(false);
  const [newAdmin, setNewAdmin] = useState({ email: '', password: '', mobile: '' });
  const [adminLoading, setAdminLoading] = useState(false);
  const [adminError, setAdminError] = useState(null);

  const fetchDetails = async () => {
    try {
      setLoading(true);
      const data = await getCollegeDetails(id);
      setCollege(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch college details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [id]);

  const handleToggleStatus = async () => {
    try {
      setStatusLoading(true);
      await updateCollegeStatus(id, !college.isActive);
      setIsStatusModalOpen(false);
      fetchDetails();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update status');
    } finally {
      setStatusLoading(false);
    }
  };

  const handleCreateAdmin = async (e) => {
    e.preventDefault();
    try {
      setAdminLoading(true);
      setAdminError(null);
      await createUniversityAdmin(id, newAdmin);
      setIsAdminModalOpen(false);
      setNewAdmin({ email: '', password: '', mobile: '' });
      fetchDetails();
    } catch (err) {
      setAdminError(err.response?.data?.message || 'Failed to create admin');
    } finally {
      setAdminLoading(false);
    }
  };

  if (loading) {
    return <div className="text-center py-12">Loading college details...</div>;
  }

  if (error || !college) {
    return (
      <div className="text-center py-12 text-red-600">
        <p className="text-xl font-semibold mb-4">{error || 'College not found'}</p>
        <Button onClick={() => navigate('/admin/colleges')}>Back to Directory</Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => navigate('/admin/colleges')}
            className="text-gray-500 hover:text-gray-700"
          >
            &larr; Back
          </button>
          <h1 className="text-2xl font-bold text-gray-900">{college.name}</h1>
          <Badge variant={college.isActive ? 'success' : 'danger'}>
            {college.isActive ? 'Active' : 'Inactive'}
          </Badge>
        </div>
        <div className="flex gap-3">
          <Button 
            variant={college.isActive ? 'danger' : 'primary'}
            onClick={() => setIsStatusModalOpen(true)}
          >
            {college.isActive ? 'Deactivate' : 'Activate'}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left Column: Info */}
        <Card className="md:col-span-1">
          <CardHeader>
            <CardTitle>Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="text-sm font-medium text-gray-500">College Code</p>
              <p className="mt-1 text-sm text-gray-900 font-mono bg-gray-100 px-2 py-1 rounded inline-block">
                {college.collegeCode}
              </p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Address</p>
              <p className="mt-1 text-sm text-gray-900 whitespace-pre-wrap">
                {college.address || 'No address provided'}
              </p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Created At</p>
              <p className="mt-1 text-sm text-gray-900">
                {new Date(college.createdAt).toLocaleString()}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Right Column: Admins */}
        <Card className="md:col-span-2">
          <CardHeader className="flex flex-row justify-between items-center">
            <div>
              <CardTitle>University Admins</CardTitle>
              <CardDescription>Users who can manage this college.</CardDescription>
            </div>
            <Button onClick={() => setIsAdminModalOpen(true)} size="sm">
              Provision Admin
            </Button>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Email</TableHead>
                  <TableHead>Mobile</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {!college.admins || college.admins.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={3} className="text-center py-8 text-gray-500">
                      No University Admins provisioned yet.
                    </TableCell>
                  </TableRow>
                ) : (
                  college.admins.map((admin) => (
                    <TableRow key={admin.id}>
                      <TableCell className="font-medium text-gray-900">{admin.email}</TableCell>
                      <TableCell>{admin.mobile || 'N/A'}</TableCell>
                      <TableCell>
                        <Badge variant={admin.isActive ? 'success' : 'danger'}>
                          {admin.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      {/* Status Modal */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title={college.isActive ? 'Deactivate College' : 'Activate College'}
        footer={
          <>
            <Button 
              variant={college.isActive ? 'danger' : 'primary'}
              onClick={handleToggleStatus} 
              disabled={statusLoading}
            >
              {statusLoading ? 'Updating...' : `Yes, ${college.isActive ? 'Deactivate' : 'Activate'}`}
            </Button>
            <Button 
              variant="secondary" 
              onClick={() => setIsStatusModalOpen(false)}
              className="mr-3"
            >
              Cancel
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          Are you sure you want to {college.isActive ? 'deactivate' : 'activate'} <strong>{college.name}</strong>?
          {college.isActive && (
            <span className="block mt-2 text-red-600 font-medium">
              Warning: Deactivating a college will immediately revoke login access for all its students, staff, and admins.
            </span>
          )}
        </p>
      </Modal>

      {/* Provision Admin Modal */}
      <Modal
        isOpen={isAdminModalOpen}
        onClose={() => setIsAdminModalOpen(false)}
        title="Provision University Admin"
        footer={
          <>
            <Button 
              onClick={handleCreateAdmin} 
              disabled={adminLoading}
            >
              {adminLoading ? 'Provisioning...' : 'Provision Admin'}
            </Button>
            <Button 
              variant="secondary" 
              onClick={() => setIsAdminModalOpen(false)}
              className="mr-3 mt-3 sm:mt-0"
            >
              Cancel
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreateAdmin} className="space-y-4">
          <p className="text-sm text-gray-600 mb-4">
            Provisioning an admin automatically verifies their email address.
          </p>
          
          {adminError && (
            <div className="bg-red-50 text-red-700 p-3 rounded text-sm">
              {adminError}
            </div>
          )}
          
          <Input
            label="Email Address"
            type="email"
            value={newAdmin.email}
            onChange={(e) => setNewAdmin({ ...newAdmin, email: e.target.value })}
            placeholder="admin@college.edu"
            required
          />
          
          <Input
            label="Initial Password"
            type="password"
            value={newAdmin.password}
            onChange={(e) => setNewAdmin({ ...newAdmin, password: e.target.value })}
            placeholder="Min 8 chars, 1 letter, 1 number"
            required
          />
          
          <Input
            label="Mobile Number (Optional)"
            type="tel"
            value={newAdmin.mobile}
            onChange={(e) => setNewAdmin({ ...newAdmin, mobile: e.target.value })}
            placeholder="+1 234 567 8900"
          />
        </form>
      </Modal>
    </div>
  );
};

export default CollegeDetails;
