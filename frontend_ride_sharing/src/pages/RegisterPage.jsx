import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../app/hooks/useAuth';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { FaUserPlus, FaUser, FaCar } from 'react-icons/fa';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';

const RegisterPage = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    role: 'PASSENGER',
    phoneNumber: '',
    profilePicture: null,
  });
  const [errors, setErrors] = useState({});
  const { handleRegister, isRegistering } = useAuth();

  const handleChange = (e) => {
    const { name, value, files } = e.target;
    setFormData({
      ...formData,
      [name]: files ? files[0] : value,
    });
    if (errors[name]) {
      setErrors({ ...errors, [name]: '' });
    }
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.username) newErrors.username = 'Username is required';
    if (!formData.email) newErrors.email = 'Email is required';
    if (!formData.password || formData.password.length < 8)
      newErrors.password = 'Password must be at least 8 characters';
    if (!formData.firstName) newErrors.firstName = 'First name is required';
    if (!formData.lastName) newErrors.lastName = 'Last name is required';
    if (!formData.profilePicture) newErrors.profilePicture = 'Profile picture is required';
    // Basic phone number validation (optional but good practice)
    if (formData.phoneNumber && !/^\d{10,}$/.test(formData.phoneNumber))
      newErrors.phoneNumber = 'Enter a valid phone number (min 10 digits)';
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = validateForm();

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    await handleRegister(formData);
  };

  // --- UI Improvements ---
  // 1. Adjusted overall padding and shadow for a cleaner look.
  // 2. Used a header icon for better visual appeal.
  // 3. Enhanced input field focus states and added a subtle transition.
  // 4. Improved role selection (radio buttons) with icons and better selected state visual.
  // 5. Added a phone number field for a more complete profile.
  // 6. Refined file input styling.

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      <main className="flex-grow flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-xl w-full bg-white p-10 rounded-2xl shadow-xl border border-gray-100">
        <div className="flex justify-center items-center mb-6">
          <FaUserPlus className="h-8 w-8 text-green-600 mr-3" />
          <h2 className="text-center text-3xl font-extrabold text-gray-900">
            Create Your Account
          </h2>
        </div>
        <p className="text-center text-sm text-gray-500 mb-8">
          Join our community! Fill out the form below to get started.
        </p>

        <form className="space-y-6" onSubmit={handleSubmit}>
          {/* Personal Information (2-Column Grid) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <InputField
              id="firstName"
              name="firstName"
              type="text"
              label="First Name"
              placeholder="Your first name"
              value={formData.firstName}
              onChange={handleChange}
              error={errors.firstName}
            />
            <InputField
              id="lastName"
              name="lastName"
              type="text"
              label="Last Name"
              placeholder="Your last name"
              value={formData.lastName}
              onChange={handleChange}
              error={errors.lastName}
            />
          </div>

          {/* Account Type Selection */}
          <RoleSelection
            formData={formData}
            handleChange={handleChange}
          />

          <div className="space-y-6 pt-2">
            <InputField
              id="username"
              name="username"
              type="text"
              label="Username"
              placeholder="Choose a unique username"
              value={formData.username}
              onChange={handleChange}
              error={errors.username}
            />
            <InputField
              id="email"
              name="email"
              type="email"
              label="Email Address"
              placeholder="you@example.com"
              value={formData.email}
              onChange={handleChange}
              error={errors.email}
            />
            <InputField
              id="phoneNumber"
              name="phoneNumber"
              type="tel"
              label="Phone Number (Optional)"
              placeholder="e.g., 555-123-4567"
              value={formData.phoneNumber}
              onChange={handleChange}
              error={errors.phoneNumber}
            />
            <InputField
              id="password"
              name="password"
              type="password"
              label="Password"
              placeholder="Must be at least 8 characters"
              value={formData.password}
              onChange={handleChange}
              error={errors.password}
            />

            {/* Profile Picture Upload */}
            <div>
              <label htmlFor="profilePicture" className="block text-sm font-medium text-gray-700 mb-1">
                Profile Picture (Required)
              </label>
              <input
                id="profilePicture"
                name="profilePicture"
                type="file"
                accept="image/*"
                // Enhanced file input styling for better cross-browser consistency
                className="mt-1 block w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100 transition duration-150 ease-in-out"
                onChange={handleChange}
              />
              {errors.profilePicture && <p className="mt-2 text-sm text-red-600 font-medium">{errors.profilePicture}</p>}
            </div>
          </div>


          {/* Submit Button */}
          <div>
            <button
              type="submit"
              disabled={isRegistering}
              // Kept button color but made it slightly more impactful with hover shadow and focus ring
              className="w-full flex justify-center py-3 px-4 border border-transparent text-lg font-semibold rounded-lg text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-4 focus:ring-offset-2 focus:ring-green-500/50 transition duration-150 ease-in-out disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-lg"
            >
              {isRegistering ? <LoadingSpinner size="sm" /> : 'Register Now'}
            </button>
          </div>
        </form>

        {/* Footer Link */}
        <div className="mt-8 text-center border-t pt-6">
          <p className="text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="font-semibold text-green-600 hover:text-green-700 transition duration-150 ease-in-out">
              Sign in here
            </Link>
          </p>
        </div>
      </div>
      </main>
      <div className="mt-auto">
        <Footer />
      </div>
    </div>
  );
};

export default RegisterPage;

// --- Helper Components for Cleaner JSX ---

// Reusable Input Field Component
const InputField = ({ id, name, type, label, placeholder, value, onChange, error }) => (
  <div>
    <label htmlFor={id} className="block text-sm font-medium text-gray-700 mb-1">
      {label}
    </label>
    <input
      id={id}
      name={name}
      type={type}
      className={`mt-1 block w-full px-4 py-2 border ${error ? 'border-red-500' : 'border-gray-300'} rounded-lg shadow-sm focus:ring-green-500 focus:border-green-500 sm:text-sm transition duration-150 ease-in-out`}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      required={!['tel', 'file'].includes(type)} // Assuming optional for phone and required for others unless overwritten
    />
    {error && <p className="mt-2 text-xs text-red-600 font-medium">{error}</p>}
  </div>
);

// Role Selection Component
const RoleSelection = ({ formData, handleChange }) => (
  <div>
    <label className="block text-sm font-semibold text-gray-800 mb-2">Account Type</label>
    <div className="grid grid-cols-2 gap-4">
      {/* Passenger Card */}
      <label className={`flex flex-col items-center justify-center p-4 border-2 rounded-xl cursor-pointer transition duration-200 ease-in-out ${formData.role === 'PASSENGER' ? 'border-green-600 bg-green-50 ring-2 ring-green-500 shadow-md' : 'border-gray-200 bg-white hover:border-green-300'}`}>
        <FaUser className={`h-6 w-6 mb-2 ${formData.role === 'PASSENGER' ? 'text-green-700' : 'text-gray-400'}`} />
        <span className={`font-medium ${formData.role === 'PASSENGER' ? 'text-green-800' : 'text-gray-700'}`}>Passenger</span>
        <input
          type="radio"
          name="role"
          value="PASSENGER"
          checked={formData.role === 'PASSENGER'}
          onChange={handleChange}
          className="sr-only"
        />
      </label>

      {/* Driver Card */}
      <label className={`flex flex-col items-center justify-center p-4 border-2 rounded-xl cursor-pointer transition duration-200 ease-in-out ${formData.role === 'DRIVER' ? 'border-green-600 bg-green-50 ring-2 ring-green-500 shadow-md' : 'border-gray-200 bg-white hover:border-green-300'}`}>
        <FaCar className={`h-6 w-6 mb-2 ${formData.role === 'DRIVER' ? 'text-green-700' : 'text-gray-400'}`} />
        <span className={`font-medium ${formData.role === 'DRIVER' ? 'text-green-800' : 'text-gray-700'}`}>Driver</span>
        <input
          type="radio"
          name="role"
          value="DRIVER"
          checked={formData.role === 'DRIVER'}
          onChange={handleChange}
          className="sr-only"
        />
      </label>
    </div>
    <p className="mt-3 text-xs text-gray-500">Choosing **Driver** will require subsequent vehicle and license verification.</p>
  </div>
);