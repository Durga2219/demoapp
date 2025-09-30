import React, { useMemo, useState } from 'react';
import { useVerifyDriverMutation, useLazyGetDriverStatusQuery } from '../app/api/driverApi';
import { useLazyGetCurrentUserQuery } from '../app/api/authApi';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

const DriverVerificationPage = () => {
  const [form, setForm] = useState({
    driverLicenseNumber: '',
    licenseExpiryDate: '',
    licenseIssuingState: '',
  });
  const [verifyDriver, { isLoading }] = useVerifyDriverMutation();
  const [triggerGetDriverStatus] = useLazyGetDriverStatusQuery();
  const [triggerGetCurrentUser] = useLazyGetCurrentUserQuery();
  const navigate = useNavigate();

  const todayIso = useMemo(() => {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }, []);

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      driverLicenseNumber: form.driverLicenseNumber.trim(),
      licenseExpiryDate: form.licenseExpiryDate || null,
      licenseIssuingState: form.licenseIssuingState.trim(),
    };

    try {
      const result = await verifyDriver(payload).unwrap();

      // After server accepts the verification request, poll the verification-status endpoint
      // to confirm server-side verification state. This uses your backend GET /driver/verification-status
      let statusResult;
      try {
        statusResult = await triggerGetDriverStatus().unwrap();
      } catch (statusErr) {
        // If status call fails, still attempt to refresh the user and show server message
        console.error('Failed to get driver verification status:', statusErr);
      }

      // Refresh current user so UI reflects any driverVerified change
      try {
        await triggerGetCurrentUser().unwrap();
      } catch (refreshErr) {
        console.error('Failed to refresh user after verification:', refreshErr);
      }

      // Examine statusResult (DriverStatusResponse). Many backends return { verified: boolean, message: string }
      const verified = statusResult?.verified ?? false;
      const statusMsg = statusResult?.message || result?.message || 'Driver verification completed.';

      if (verified) {
        toast.success(statusMsg);
        navigate('/vehicles');
      } else {
        // Not verified yet — show server message and stay on page so user sees next steps
        toast(statusMsg);
      }
    } catch (err) {
      console.error('Driver verification failed:', err);
      const message = err?.data?.message || err?.message || 'Verification failed';
      toast.error(message);
    }
  };

  return (
    <div>
      <Header />
      <main className="max-w-3xl h-[100vh] mx-auto p-6">
        <h1 className="text-xl font-semibold">Driver Verification</h1>
        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          <input name="driverLicenseNumber" placeholder="Driver license number" value={form.driverLicenseNumber} onChange={onChange} className="w-full px-3 py-2 border rounded" />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <input name="licenseIssuingState" placeholder="Issuing state" value={form.licenseIssuingState} onChange={onChange} className="px-3 py-2 border rounded" />
            <input name="licenseExpiryDate" type="date" placeholder="Expiry date" min={todayIso} value={form.licenseExpiryDate} onChange={onChange} className="px-3 py-2 border rounded" />
          </div>
          <p className="text-sm text-gray-600">After verification, you can add multiple vehicles on the Vehicles page.</p>
          <button className="px-4 py-2 bg-blue-600 text-white rounded" disabled={isLoading}>Submit</button>
        </form>
      </main>
      <Footer />
    </div>
  );
};

export default DriverVerificationPage;
