import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { validatePublicInvitation, submitPublicOnboarding } from '../../api/onboarding';
import { Building2, CheckCircle2, AlertCircle, ChevronRight, Loader2 } from 'lucide-react';

const PublicOnboarding = () => {
  const { token } = useParams();
  const navigate = useNavigate();
  
  const [step, setStep] = useState(0); // 0: loading, 1: details, 2: contact, 3: password, 4: success, -1: error
  const [errorMsg, setErrorMsg] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [invitationData, setInvitationData] = useState(null);

  const [formData, setFormData] = useState({
    institutionName: '',
    addressLine: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
    contactName: '',
    contactEmail: '',
    contactPhone: '',
    institutionType: 'University',
    website: '',
    password: '',
    confirmPassword: ''
  });

  useEffect(() => {
    const checkToken = async () => {
      try {
        const data = await validatePublicInvitation(token);
        setInvitationData(data);
        setFormData(prev => ({
          ...prev,
          institutionName: data.institutionName,
          contactEmail: data.adminEmail
        }));
        setStep(1);
      } catch (err) {
        setErrorMsg(err.response?.data?.message || 'Invalid or expired invitation link.');
        setStep(-1);
      }
    };
    checkToken();
  }, [token]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const nextStep = (e) => {
    e.preventDefault();
    if (step === 3) {
      if (formData.password !== formData.confirmPassword) {
        alert("Passwords do not match!");
        return;
      }
      submitForm();
    } else {
      setStep(prev => prev + 1);
    }
  };

  const prevStep = () => {
    setStep(prev => prev - 1);
  };

  const submitForm = async () => {
    setIsSubmitting(true);
    try {
      await submitPublicOnboarding(token, formData);
      setStep(4);
    } catch (err) {
      alert(err.response?.data?.message || 'Submission failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (step === 0) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-900">Validating Invitation...</h2>
        </div>
      </div>
    );
  }

  if (step === -1) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white p-8 rounded-xl shadow-sm border border-red-100 max-w-md w-full text-center">
          <AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Invalid Link</h2>
          <p className="text-gray-600 mb-6">{errorMsg}</p>
        </div>
      </div>
    );
  }

  if (step === 4) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-200 max-w-md w-full text-center">
          <CheckCircle2 className="h-16 w-16 text-green-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Onboarding Complete!</h2>
          <p className="text-gray-600 mb-6">
            Your institution has been successfully verified. You can now login using your email and the password you just set.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="w-full bg-primary-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-primary-700 transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4 py-12">
      <div className="mb-8 flex items-center gap-3">
        <Building2 className="h-8 w-8 text-primary-600" />
        <h1 className="text-2xl font-bold text-gray-900">Academic Passport Onboarding</h1>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 max-w-2xl w-full overflow-hidden">
        {/* Progress Bar */}
        <div className="flex border-b border-gray-100">
          {['Institution Details', 'Contact Info', 'Admin Setup'].map((label, idx) => (
            <div key={label} className={`flex-1 text-center py-4 text-sm font-medium border-b-2 ${step === idx + 1 ? 'border-primary-600 text-primary-600' : 'border-transparent text-gray-400'}`}>
              {idx + 1}. {label}
            </div>
          ))}
        </div>

        <div className="p-8">
          <form onSubmit={nextStep}>
            {step === 1 && (
              <div className="space-y-4 animate-in fade-in slide-in-from-right-4">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Institution Details</h3>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Institution Name</label>
                  <input type="text" name="institutionName" value={formData.institutionName} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Institution Type</label>
                    <select name="institutionType" value={formData.institutionType} onChange={handleChange} className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500">
                      <option value="University">University</option>
                      <option value="Autonomous College">Autonomous College</option>
                      <option value="Affiliated College">Affiliated College</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Website (Optional)</label>
                    <input type="url" name="website" value={formData.website} onChange={handleChange} className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Address Line</label>
                  <input type="text" name="addressLine" value={formData.addressLine} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                    <input type="text" name="city" value={formData.city} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
                    <input type="text" name="state" value={formData.state} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Postal Code</label>
                    <input type="text" name="postalCode" value={formData.postalCode} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                    <input type="text" name="country" value={formData.country} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                  </div>
                </div>
              </div>
            )}

            {step === 2 && (
              <div className="space-y-4 animate-in fade-in slide-in-from-right-4">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Primary Contact Information</h3>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Contact Name</label>
                  <input type="text" name="contactName" value={formData.contactName} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Contact Email</label>
                  <input type="email" name="contactEmail" value={formData.contactEmail} disabled className="w-full border border-gray-200 bg-gray-50 text-gray-500 rounded-lg p-2.5" />
                  <p className="text-xs text-gray-500 mt-1">This email is locked to the invitation.</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Contact Phone</label>
                  <input type="tel" name="contactPhone" value={formData.contactPhone} onChange={handleChange} required className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                </div>
              </div>
            )}

            {step === 3 && (
              <div className="space-y-4 animate-in fade-in slide-in-from-right-4">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">University Administrator Setup</h3>
                <p className="text-sm text-gray-600 mb-6">
                  You are setting up the primary <b>University Admin</b> account for {formData.institutionName}. 
                  Please set a secure password for <b>{formData.contactEmail}</b>.
                </p>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                  <input type="password" name="password" value={formData.password} onChange={handleChange} required minLength={8} className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
                  <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} required minLength={8} className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary-500" />
                </div>
              </div>
            )}

            <div className="mt-8 flex items-center justify-between pt-6 border-t border-gray-100">
              <button
                type="button"
                onClick={prevStep}
                disabled={step === 1 || isSubmitting}
                className="px-4 py-2 text-gray-600 font-medium hover:text-gray-900 disabled:opacity-50"
              >
                Back
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="bg-primary-600 text-white px-6 py-2 rounded-lg font-medium hover:bg-primary-700 transition-colors flex items-center gap-2 disabled:opacity-75"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                {step === 3 ? 'Complete Onboarding' : 'Continue'} 
                {step !== 3 && <ChevronRight className="w-4 h-4" />}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default PublicOnboarding;
