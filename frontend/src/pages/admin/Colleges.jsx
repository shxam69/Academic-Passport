import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getColleges } from '../../api/colleges';
import { getInvitations, generateInvitation, revokeInvitation, manualOnboard } from '../../api/onboarding';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../components/ui/Table';
import Badge from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import Modal from '../../components/ui/Modal';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Copy, CheckCircle, Search, Mail, Link as LinkIcon, Building2 } from 'lucide-react';

const Colleges = () => {
  const [activeTab, setActiveTab] = useState('directory'); // 'directory' | 'invitations'
  const navigate = useNavigate();

  // Directory State
  const [colleges, setColleges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Invitations State
  const [invitations, setInvitations] = useState([]);
  const [invLoading, setInvLoading] = useState(true);
  const [invPage, setInvPage] = useState(0);
  const [invTotalPages, setInvTotalPages] = useState(0);

  // Invite Modal State
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [inviteData, setInviteData] = useState({ institutionName: '', adminEmail: '' });
  const [inviteLink, setInviteLink] = useState('');
  const [inviteLoading, setInviteLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (activeTab === 'directory') fetchColleges();
    if (activeTab === 'invitations') fetchInvitations();
  }, [activeTab, page, search, invPage]);

  const fetchColleges = async () => {
    try {
      setLoading(true);
      const data = await getColleges({ page, size: 10, search: search || undefined });
      setColleges(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchInvitations = async () => {
    try {
      setInvLoading(true);
      const data = await getInvitations({ page: invPage, size: 10 });
      setInvitations(data.content);
      setInvTotalPages(data.totalPages);
    } catch (err) {
      console.error(err);
    } finally {
      setInvLoading(false);
    }
  };

  const handleGenerateInvite = async (e) => {
    e.preventDefault();
    setInviteLoading(true);
    try {
      const res = await generateInvitation(inviteData);
      const link = `${window.location.origin}/onboard/${res.token}`;
      setInviteLink(link);
      fetchInvitations();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to generate invitation');
    } finally {
      setInviteLoading(false);
    }
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(inviteLink);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRevoke = async (id) => {
    if (!window.confirm('Are you sure you want to revoke this invitation?')) return;
    try {
      await revokeInvitation(id);
      fetchInvitations();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to revoke');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Institutions</h1>
          <p className="mt-1 text-sm text-gray-500">Manage directory and onboarding invitations.</p>
        </div>
        <div className="flex gap-2">
          {/* We keep manual onboard available but deemphasize it by styling if preferred, or just rely on Invite */}
          <Button onClick={() => setIsInviteModalOpen(true)} className="flex items-center gap-2">
            <Mail className="w-4 h-4" /> Invite Institution
          </Button>
        </div>
      </div>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('directory')}
            className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === 'directory' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}
          >
            Directory
          </button>
          <button
            onClick={() => setActiveTab('invitations')}
            className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === 'invitations' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}`}
          >
            Invitations
          </button>
        </nav>
      </div>

      {activeTab === 'directory' && (
        <Card>
          <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <CardTitle>Active Institutions</CardTitle>
            <div className="relative max-w-md w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
              <input
                type="text"
                placeholder="Search colleges..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-md focus:ring-primary-500 focus:border-primary-500 text-sm"
              />
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow><TableCell colSpan={4} className="text-center py-8">Loading...</TableCell></TableRow>
                ) : colleges.length === 0 ? (
                  <TableRow><TableCell colSpan={4} className="text-center py-8">No colleges found.</TableCell></TableRow>
                ) : (
                  colleges.map((college) => (
                    <TableRow key={college.id} onClick={() => navigate(`/admin/colleges/${college.id}`)} className="cursor-pointer">
                      <TableCell className="font-medium text-gray-900">{college.collegeCode}</TableCell>
                      <TableCell>{college.name}</TableCell>
                      <TableCell>
                        <Badge variant={college.isActive ? 'success' : 'danger'}>
                          {college.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-gray-500">
                        {new Date(college.createdAt).toLocaleDateString()}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {activeTab === 'invitations' && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Institution Name</TableHead>
                  <TableHead>Admin Email</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Expires At</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {invLoading ? (
                  <TableRow><TableCell colSpan={5} className="text-center py-8">Loading...</TableCell></TableRow>
                ) : invitations.length === 0 ? (
                  <TableRow><TableCell colSpan={5} className="text-center py-8">No invitations found.</TableCell></TableRow>
                ) : (
                  invitations.map((inv) => (
                    <TableRow key={inv.id}>
                      <TableCell className="font-medium text-gray-900">{inv.institutionName}</TableCell>
                      <TableCell>{inv.adminEmail}</TableCell>
                      <TableCell>
                        <Badge variant={inv.status === 'PENDING' ? 'primary' : inv.status === 'USED' ? 'success' : 'danger'}>
                          {inv.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-gray-500">
                        {new Date(inv.expiresAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell className="text-right">
                        {inv.status === 'PENDING' && (
                          <button onClick={() => handleRevoke(inv.id)} className="text-red-600 hover:text-red-800 text-sm font-medium">
                            Revoke
                          </button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Modal
        isOpen={isInviteModalOpen}
        onClose={() => {
          setIsInviteModalOpen(false);
          setInviteLink('');
          setInviteData({ institutionName: '', adminEmail: '' });
        }}
        title="Invite Institution"
        footer={null}
      >
        {!inviteLink ? (
          <form onSubmit={handleGenerateInvite} className="space-y-4">
            <p className="text-sm text-gray-500 mb-4">
              Generate a secure, single-use onboarding link for a new institution.
            </p>
            <Input
              label="Institution Name"
              value={inviteData.institutionName}
              onChange={(e) => setInviteData({ ...inviteData, institutionName: e.target.value })}
              required
            />
            <Input
              label="Primary Admin Email"
              type="email"
              value={inviteData.adminEmail}
              onChange={(e) => setInviteData({ ...inviteData, adminEmail: e.target.value })}
              required
            />
            <div className="flex justify-end gap-3 mt-6">
              <Button type="button" variant="secondary" onClick={() => setIsInviteModalOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={inviteLoading}>
                {inviteLoading ? 'Generating...' : 'Generate Link'}
              </Button>
            </div>
          </form>
        ) : (
          <div className="space-y-4">
            <div className="bg-green-50 text-green-800 p-4 rounded-lg flex items-start gap-3">
              <CheckCircle className="w-5 h-5 mt-0.5" />
              <div>
                <h4 className="font-medium">Invitation Generated</h4>
                <p className="text-sm mt-1 opacity-90">
                  Send this secure link to the institution's administrator. The link will expire in 7 days.
                </p>
              </div>
            </div>
            
            <div className="flex gap-2">
              <input
                type="text"
                readOnly
                value={inviteLink}
                className="flex-1 border border-gray-300 rounded-lg p-2.5 bg-gray-50 text-gray-600 focus:outline-none"
              />
              <Button onClick={copyToClipboard} variant="secondary" className="flex items-center gap-2">
                {copied ? <CheckCircle className="w-4 h-4 text-green-600" /> : <Copy className="w-4 h-4" />}
                {copied ? 'Copied' : 'Copy'}
              </Button>
            </div>
            <div className="flex justify-end mt-4">
              <Button onClick={() => {
                setIsInviteModalOpen(false);
                setInviteLink('');
                setInviteData({ institutionName: '', adminEmail: '' });
              }}>Done</Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Colleges;
