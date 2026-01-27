import { useState } from 'react';
import { FileText, Upload, CheckCircle, XCircle, AlertCircle, Mail, Phone, MapPin } from 'lucide-react';

interface FormData {
  fullName: string;
  policyNumber: string;
  causeOfDeath: string;
  email: string;
  mobileNumber: string;
  address: string;
  nomineeFullName: string;
  nomineeRelationship: string;
  nomineeMobileNumber: string;
  claimForm: File | null;
  deathCertificate: File | null;
  doctorReport: File | null;
  policeReport: File | null;
}

interface SubmissionResult {
  status: 'Approved' | 'Rejected' | 'Manual Review';
  reason?: string;
  claimReference?: string;
}

function App() {
  const [formData, setFormData] = useState<FormData>({
    fullName: '',
    policyNumber: '',
    causeOfDeath: '',
    email: '',
    mobileNumber: '',
    address: '',
    nomineeFullName: '',
    nomineeRelationship: '',
    nomineeMobileNumber: '',
    claimForm: null,
    deathCertificate: null,
    doctorReport: null,
    policeReport: null,
  });

  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>, fieldName: keyof FormData) => {
    const file = e.target.files?.[0] || null;
    setFormData(prev => ({ ...prev, [fieldName]: file }));
    if (errors[fieldName]) {
      setErrors(prev => ({ ...prev, [fieldName]: '' }));
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Full name is required';
    }
    if (!formData.policyNumber.trim()) {
      newErrors.policyNumber = 'Policy number is required';
    }
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }
    if (!formData.mobileNumber.trim()) {
      newErrors.mobileNumber = 'Mobile number is required';
    } else if (!/^\d{10}$/.test(formData.mobileNumber.replace(/\D/g, ''))) {
      newErrors.mobileNumber = 'Please enter a valid 10-digit mobile number';
    }
    if (!formData.address.trim()) {
      newErrors.address = 'Address is required';
    }
    if (!formData.causeOfDeath) {
      newErrors.causeOfDeath = 'Cause of death is required';
    }
    if (!formData.nomineeFullName.trim()) {
      newErrors.nomineeFullName = 'Nominee name is required';
    }
    if (!formData.nomineeRelationship.trim()) {
      newErrors.nomineeRelationship = 'Nominee relationship is required';
    }
    if (!formData.nomineeMobileNumber.trim()) {
      newErrors.nomineeMobileNumber = 'Nominee mobile number is required';
    } else if (!/^\d{10}$/.test(formData.nomineeMobileNumber.replace(/\D/g, ''))) {
      newErrors.nomineeMobileNumber = 'Please enter a valid 10-digit mobile number';
    }
    if (!formData.claimForm) {
      newErrors.claimForm = 'Claim form is required';
    }
    if (!formData.deathCertificate) {
      newErrors.deathCertificate = 'Death certificate is required';
    }
    if (!formData.doctorReport) {
      newErrors.doctorReport = 'Doctor report is required';
    }
    if ((formData.causeOfDeath === 'Accident' || formData.causeOfDeath === 'Suicide') && !formData.policeReport) {
      newErrors.policeReport = 'Police report is required for this case';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    setResult(null);

    try {
      const formDataToSend = new FormData();
      formDataToSend.append('policyNumber', formData.policyNumber);
      formDataToSend.append('causeOfDeath', formData.causeOfDeath);
      formDataToSend.append('deceasedFullName', formData.fullName);
      formDataToSend.append('deceasedEmail', formData.email);
      formDataToSend.append('deceasedMobile', formData.mobileNumber);
      formDataToSend.append('deceasedAddress', formData.address);
      formDataToSend.append('nomineeFullName', formData.nomineeFullName);
      formDataToSend.append('nomineeRelationship', formData.nomineeRelationship);
      formDataToSend.append('nomineeMobile', formData.nomineeMobileNumber);

      // Note: policyDocument is not provided in the form, assuming it's optional
      if (formData.claimForm && formData.claimForm.size > 0) {
        formDataToSend.append('claimForm', formData.claimForm);
      }
      if (formData.deathCertificate && formData.deathCertificate.size > 0) {
        formDataToSend.append('deathCertificate', formData.deathCertificate);
      }
      if (formData.doctorReport && formData.doctorReport.size > 0) {
        formDataToSend.append('doctorReport', formData.doctorReport);
      }
      if (formData.policeReport && formData.policeReport.size > 0) {
        formDataToSend.append('policeReport', formData.policeReport);
      }

      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
      const response = await fetch(`${API_URL}/api/claim/submit`, {
        method: 'POST',
        body: formDataToSend,
        // Don't set Content-Type header - let browser set it for FormData
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      const statusMap: Record<string, 'Approved' | 'Rejected' | 'Manual Review'> = {
        'APPROVED': 'Approved',
        'REJECTED': 'Rejected',
        'MANUAL_REVIEW': 'Manual Review'
      };
      setResult({
        status: statusMap[data.status] || 'Manual Review',
        reason: data.message,
        claimReference: data.claimReference,
      });
    } catch (error) {
      console.error('Submission error:', error);
      setResult({
        status: 'Manual Review',
        reason: 'Error submitting claim. Please try again or contact support.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormData({
      fullName: '',
      policyNumber: '',
      causeOfDeath: '',
      email: '',
      mobileNumber: '',
      address: '',
      nomineeFullName: '',
      nomineeRelationship: '',
      nomineeMobileNumber: '',
      claimForm: null,
      deathCertificate: null,
      doctorReport: null,
      policeReport: null,
    });
    setResult(null);
    setErrors({});
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white shadow-md border-b-4 border-blue-600">
        <div className="max-w-6xl mx-auto px-6 py-6">
          <div className="flex items-center mb-4">
            <div className="w-10 h-10 bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg flex items-center justify-center mr-3">
              <FileText className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900">Trinetra</h1>
          </div>
          <h2 className="text-xl font-semibold text-gray-700">Life Insurance Claim Submission Portal</h2>
          <p className="text-gray-600 text-sm mt-2">Submit your life insurance claim securely and get quick processing</p>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 py-8">
        {result ? (
          <div className="bg-white rounded-lg shadow-md p-8">
            <div className="text-center">
              {result.status === 'Approved' && (
                <>
                  <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
                  <h2 className="text-3xl font-bold text-green-600 mb-2">Claim Approved</h2>
                  <p className="text-gray-600 mb-6">
                    Your claim has been approved and will be processed within 5-7 business days.
                  </p>
                </>
              )}
              {result.status === 'Rejected' && (
                <>
                  <XCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
                  <h2 className="text-3xl font-bold text-red-600 mb-2">Claim Rejected</h2>
                  <p className="text-gray-600 mb-6">
                    {result.reason || 'Your claim does not meet the policy requirements.'}
                  </p>
                </>
              )}
              {result.status === 'Manual Review' && (
                <>
                  <AlertCircle className="w-16 h-16 text-amber-500 mx-auto mb-4" />
                  <h2 className="text-3xl font-bold text-amber-600 mb-2">Manual Review Required</h2>
                  <p className="text-gray-600 mb-6">
                    {result.reason || 'Your claim requires additional review. Our team will contact you within 2-3 business days.'}
                  </p>
                </>
              )}

              <div className="bg-gray-50 rounded-lg p-4 mb-6">
                <p className="text-sm text-gray-700">
                  <strong>Claim Reference:</strong> {result.claimReference || `${formData.policyNumber}-${Date.now().toString().slice(-6)}`}
                </p>
              </div>

              <button
                onClick={resetForm}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
              >
                Submit Another Claim
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-md p-8">
            <div className="mb-8 pb-6 border-b border-gray-200">
              <h2 className="text-lg font-bold text-gray-900 mb-2 text-blue-700">Policy Information</h2>
              <p className="text-gray-600 text-sm">Enter your policy details</p>
            </div>

            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label htmlFor="policyNumber" className="block text-sm font-medium text-gray-700 mb-2">
                    Policy Number <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    id="policyNumber"
                    name="policyNumber"
                    value={formData.policyNumber}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.policyNumber ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="e.g., PNB1001"
                  />
                  {errors.policyNumber && <p className="text-red-500 text-sm mt-1">{errors.policyNumber}</p>}
                </div>

                <div>
                  <label htmlFor="causeOfDeath" className="block text-sm font-medium text-gray-700 mb-2">
                    Cause of Death <span className="text-red-500">*</span>
                  </label>
                  <select
                    id="causeOfDeath"
                    name="causeOfDeath"
                    value={formData.causeOfDeath}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.causeOfDeath ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all bg-white`}
                  >
                    <option value="">Select cause of death</option>
                    <option value="Natural">Natural Causes</option>
                    <option value="Disease">Any Disease</option>
                    <option value="Accident">Accident</option>
                    <option value="Suicide">Suicide</option>
                  </select>
                  {errors.causeOfDeath && <p className="text-red-500 text-sm mt-1">{errors.causeOfDeath}</p>}
                </div>
              </div>

              <div className="mb-8 pb-6 border-t border-gray-200 pt-6">
                <h2 className="text-lg font-bold text-gray-900 mb-2 text-blue-700">Deceased Information</h2>
                <p className="text-gray-600 text-sm">Provide information about the deceased</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label htmlFor="fullName" className="block text-sm font-medium text-gray-700 mb-2">
                    Full Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    id="fullName"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.fullName ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="Enter full name as per policy"
                  />
                  {errors.fullName && <p className="text-red-500 text-sm mt-1">{errors.fullName}</p>}
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2 flex items-center">
                    <Mail className="w-4 h-4 mr-2 text-gray-500" />
                    Email Address <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.email ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="your.email@example.com"
                  />
                  {errors.email && <p className="text-red-500 text-sm mt-1">{errors.email}</p>}
                </div>

                <div>
                  <label htmlFor="mobileNumber" className="block text-sm font-medium text-gray-700 mb-2 flex items-center">
                    <Phone className="w-4 h-4 mr-2 text-gray-500" />
                    Mobile Number <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    id="mobileNumber"
                    name="mobileNumber"
                    value={formData.mobileNumber}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.mobileNumber ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="10-digit mobile number"
                  />
                  {errors.mobileNumber && <p className="text-red-500 text-sm mt-1">{errors.mobileNumber}</p>}
                </div>

                <div>
                  <label htmlFor="address" className="block text-sm font-medium text-gray-700 mb-2 flex items-center">
                    <MapPin className="w-4 h-4 mr-2 text-gray-500" />
                    Address <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    id="address"
                    name="address"
                    value={formData.address}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.address ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="Full address"
                  />
                  {errors.address && <p className="text-red-500 text-sm mt-1">{errors.address}</p>}
                </div>
              </div>

              <div className="mb-8 pb-6 border-t border-gray-200 pt-6">
                <h2 className="text-lg font-bold text-gray-900 mb-2 text-blue-700">Nominee Information</h2>
                <p className="text-gray-600 text-sm">Details of the claim beneficiary</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label htmlFor="nomineeFullName" className="block text-sm font-medium text-gray-700 mb-2">
                    Nominee Full Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    id="nomineeFullName"
                    name="nomineeFullName"
                    value={formData.nomineeFullName}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.nomineeFullName ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="Nominee's full name"
                  />
                  {errors.nomineeFullName && <p className="text-red-500 text-sm mt-1">{errors.nomineeFullName}</p>}
                </div>

                <div>
                  <label htmlFor="nomineeRelationship" className="block text-sm font-medium text-gray-700 mb-2">
                    Relationship <span className="text-red-500">*</span>
                  </label>
                  <select
                    id="nomineeRelationship"
                    name="nomineeRelationship"
                    value={formData.nomineeRelationship}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.nomineeRelationship ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all bg-white`}
                  >
                    <option value="">Select relationship</option>
                    <option value="Spouse">Spouse</option>
                    <option value="Child">Child</option>
                    <option value="Parent">Parent</option>
                    <option value="Sibling">Sibling</option>
                    <option value="Other">Other</option>
                  </select>
                  {errors.nomineeRelationship && <p className="text-red-500 text-sm mt-1">{errors.nomineeRelationship}</p>}
                </div>

                <div>
                  <label htmlFor="nomineeMobileNumber" className="block text-sm font-medium text-gray-700 mb-2 flex items-center">
                    <Phone className="w-4 h-4 mr-2 text-gray-500" />
                    Nominee Mobile Number <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    id="nomineeMobileNumber"
                    name="nomineeMobileNumber"
                    value={formData.nomineeMobileNumber}
                    onChange={handleInputChange}
                    className={`w-full px-4 py-3 border ${errors.nomineeMobileNumber ? 'border-red-500' : 'border-gray-300'} rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all`}
                    placeholder="10-digit mobile number"
                  />
                  {errors.nomineeMobileNumber && <p className="text-red-500 text-sm mt-1">{errors.nomineeMobileNumber}</p>}
                </div>
              </div>

              <div className="mb-8 pb-6 border-t border-gray-200 pt-6">
                <h2 className="text-lg font-bold text-gray-900 mb-2 text-blue-700">Supporting Documents</h2>
                <p className="text-gray-600 text-sm">Upload required documents (PDF, JPG, PNG - Max 10MB each)</p>
              </div>

              <div className="space-y-4">
                <div>
                  <label htmlFor="claimForm" className="block text-sm font-medium text-gray-700 mb-2">
                    Claim Form <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="file"
                      id="claimForm"
                      onChange={(e) => handleFileChange(e, 'claimForm')}
                      accept=".pdf,.jpg,.jpeg,.png"
                      className="hidden"
                    />
                    <label
                      htmlFor="claimForm"
                      className={`flex items-center justify-center w-full px-6 py-4 border-2 ${errors.claimForm ? 'border-red-500 bg-red-50' : 'border-gray-300 hover:border-blue-500'} border-dashed rounded-lg cursor-pointer transition-colors`}
                    >
                      <Upload className="w-5 h-5 text-gray-400 mr-2" />
                      <span className="text-gray-600">
                        {formData.claimForm ? formData.claimForm.name : 'Click to upload claim form'}
                      </span>
                    </label>
                  </div>
                  {errors.claimForm && <p className="text-red-500 text-sm mt-1">{errors.claimForm}</p>}
                </div>

                <div>
                  <label htmlFor="deathCertificate" className="block text-sm font-medium text-gray-700 mb-2">
                    Death Certificate <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="file"
                      id="deathCertificate"
                      onChange={(e) => handleFileChange(e, 'deathCertificate')}
                      accept=".pdf,.jpg,.jpeg,.png"
                      className="hidden"
                    />
                    <label
                      htmlFor="deathCertificate"
                      className={`flex items-center justify-center w-full px-6 py-4 border-2 ${errors.deathCertificate ? 'border-red-500 bg-red-50' : 'border-gray-300 hover:border-blue-500'} border-dashed rounded-lg cursor-pointer transition-colors`}
                    >
                      <Upload className="w-5 h-5 text-gray-400 mr-2" />
                      <span className="text-gray-600">
                        {formData.deathCertificate ? formData.deathCertificate.name : 'Click to upload death certificate'}
                      </span>
                    </label>
                  </div>
                  {errors.deathCertificate && <p className="text-red-500 text-sm mt-1">{errors.deathCertificate}</p>}
                </div>

                <div>
                  <label htmlFor="doctorReport" className="block text-sm font-medium text-gray-700 mb-2">
                    Doctor Report <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="file"
                      id="doctorReport"
                      onChange={(e) => handleFileChange(e, 'doctorReport')}
                      accept=".pdf,.jpg,.jpeg,.png"
                      className="hidden"
                    />
                    <label
                      htmlFor="doctorReport"
                      className={`flex items-center justify-center w-full px-6 py-4 border-2 ${errors.doctorReport ? 'border-red-500 bg-red-50' : 'border-gray-300 hover:border-blue-500'} border-dashed rounded-lg cursor-pointer transition-colors`}
                    >
                      <Upload className="w-5 h-5 text-gray-400 mr-2" />
                      <span className="text-gray-600">
                        {formData.doctorReport ? formData.doctorReport.name : 'Click to upload doctor report'}
                      </span>
                    </label>
                  </div>
                  {errors.doctorReport && <p className="text-red-500 text-sm mt-1">{errors.doctorReport}</p>}
                </div>

                {(formData.causeOfDeath === 'Accident' || formData.causeOfDeath === 'Suicide') && (
                  <div className="animate-fadeIn">
                    <label htmlFor="policeReport" className="block text-sm font-medium text-gray-700 mb-2">
                      Police Report <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <input
                        type="file"
                        id="policeReport"
                        onChange={(e) => handleFileChange(e, 'policeReport')}
                        accept=".pdf,.jpg,.jpeg,.png"
                        className="hidden"
                      />
                      <label
                        htmlFor="policeReport"
                        className={`flex items-center justify-center w-full px-6 py-4 border-2 ${errors.policeReport ? 'border-red-500 bg-red-50' : 'border-gray-300 hover:border-blue-500'} border-dashed rounded-lg cursor-pointer transition-colors`}
                      >
                        <Upload className="w-5 h-5 text-gray-400 mr-2" />
                        <span className="text-gray-600">
                          {formData.policeReport ? formData.policeReport.name : 'Click to upload police report'}
                        </span>
                      </label>
                    </div>
                    {errors.policeReport && <p className="text-red-500 text-sm mt-1">{errors.policeReport}</p>}
                  </div>
                )}
              </div>

              <div className="bg-gradient-to-r from-blue-50 to-blue-100 border border-blue-300 rounded-lg p-5 mt-8">
                <div className="flex items-start">
                  <AlertCircle className="w-5 h-5 text-blue-600 mr-3 mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="text-sm font-semibold text-blue-900 mb-1">Important Information</p>
                    <p className="text-sm text-blue-800">
                      All documents should be clear, legible, and issued by authorized authorities. Accepted formats: PDF, JPG, JPEG, PNG. Maximum file size: 10MB per document. Processing typically takes 5-7 business days.
                    </p>
                  </div>
                </div>
              </div>

              <div className="pt-8">
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full py-4 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-lg hover:from-blue-700 hover:to-blue-800 disabled:from-gray-400 disabled:to-gray-500 disabled:cursor-not-allowed transition-all font-semibold text-lg shadow-md hover:shadow-lg"
                >
                  {submitting ? (
                    <span className="flex items-center justify-center">
                      <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Processing Your Claim...
                    </span>
                  ) : (
                    'Submit Claim'
                  )}
                </button>
                <p className="text-center text-gray-600 text-xs mt-3">By submitting, you declare that all information is true and accurate</p>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default App;
