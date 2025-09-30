import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useRides } from '../app/hooks/useRides';
import { useGetMyVehiclesQuery } from '../app/api/vehicleApi';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import toast from 'react-hot-toast';

const CreateRidePage = () => {
  const navigate = useNavigate();
  const { handleCreateRide, isCreatingRide } = useRides();
  const { data: vehicles, isLoading, isError } = useGetMyVehiclesQuery();
  const [showSuccess, setShowSuccess] = useState(false);
  const [form, setForm] = useState({ 
    vehicleId: '', 
    sourceCity: '', 
    sourceAddress: '',
    destinationCity: '', 
    destinationAddress: '',
    departureDateTime: '', 
    availableSeats: 1, 
    baseFare: '',
    description: ''
  });

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    const result = await handleCreateRide(form);
    
    if (result.success) {
      // Show success state
      setShowSuccess(true);
      
      // Show detailed success toast
      toast.success(
        `🎉 Ride created successfully from ${form.sourceCity} to ${form.destinationCity}!`,
        { duration: 4000 }
      );
      
      // Redirect to My Rides page after 2 seconds
      setTimeout(() => {
        navigate('/rides/my-rides');
      }, 2000);
    }
  };

  const renderVehicleOptions = () => {
    if (isLoading) {
      return <option>Loading vehicles...</option>;
    }
    if (isError) {
      return <option>Error loading vehicles</option>;
    }
    if (vehicles && vehicles.length > 0) {
      return vehicles.map(v => (
        <option key={v.vehicleId || v.id} value={v.vehicleId || v.id}>
          {v.make} {v.model} • {v.licensePlate}
        </option>
      ));
    }
    return null; // No vehicles, the parent component will handle this
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <Header />
      <main className="max-w-4xl mx-auto p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Create Ride</h1>

        {showSuccess && (
          <div className="mb-6 p-6 bg-green-50 border-2 border-green-500 rounded-lg animate-pulse">
            <div className="flex items-center gap-3 mb-3">
              <span className="text-3xl">✅</span>
              <h2 className="text-xl font-bold text-green-800">
                Ride Created Successfully!
              </h2>
            </div>
            <div className="text-green-700 space-y-2">
              <p className="text-lg font-semibold">
                {form.sourceCity} → {form.destinationCity}
              </p>
              <div className="grid grid-cols-2 gap-2 text-sm">
                <p>📍 Pickup: {form.sourceAddress}</p>
                <p>📍 Drop: {form.destinationAddress}</p>
                <p>🪑 Available Seats: {form.availableSeats}</p>
                <p>💰 Fare: ₹{form.baseFare} per seat</p>
              </div>
              <p className="text-sm mt-3 font-medium">
                Redirecting to your rides...
              </p>
            </div>
          </div>
        )}

        {isError && <ErrorMessage message="Could not load vehicle data. Please try again later." />}

        {!showSuccess && isLoading ? (
          <LoadingSpinner />
        ) : !showSuccess && vehicles && vehicles.length === 0 ? (
          <div className="mt-4 p-6 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p className="text-yellow-800 font-medium">
              You need to add a vehicle before you can create a ride.
            </p>
            <Link to="/vehicles" className="mt-2 inline-block text-sm font-medium text-yellow-900 underline">
              + Add a Vehicle Now
            </Link>
          </div>
        ) : !showSuccess ? (
          <form onSubmit={onSubmit} className="mt-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Vehicle *</label>
              <select
                name="vehicleId"
                value={form.vehicleId}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              >
                <option value="">Select a vehicle</option>
                {renderVehicleOptions()}
              </select>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Source City *</label>
                <input
                  name="sourceCity"
                  placeholder="e.g., Nainital"
                  value={form.sourceCity}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Destination City *</label>
                <input
                  name="destinationCity"
                  placeholder="e.g., Delhi"
                  value={form.destinationCity}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Source Address *</label>
              <input
                name="sourceAddress"
                placeholder="Pickup location (e.g., Bus Stand, Nainital)"
                value={form.sourceAddress}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Destination Address *</label>
              <input
                name="destinationAddress"
                placeholder="Drop-off location (e.g., ISBT, Delhi)"
                value={form.destinationAddress}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Departure Date & Time *</label>
              <input
                name="departureDateTime"
                type="datetime-local"
                value={form.departureDateTime}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Available Seats *</label>
                <input
                  name="availableSeats"
                  type="number"
                  min="1"
                  max="8"
                  placeholder="Number of seats"
                  value={form.availableSeats}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Base Fare (₹) *</label>
                <input
                  name="baseFare"
                  type="number"
                  min="0.01"
                  step="0.01"
                  placeholder="Fare per seat"
                  value={form.baseFare}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Description (optional)</label>
              <textarea
                name="description"
                placeholder="Any additional details about the ride..."
                value={form.description}
                onChange={onChange}
                rows="3"
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <button
              className="w-full px-4 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:outline-none transition disabled:opacity-50"
              disabled={isCreatingRide}
            >
              {isCreatingRide ? 'Creating...' : 'Create Ride'}
            </button>
          </form>
        ) : null}
      </main>
      <Footer />
    </div>
  );
};

export default CreateRidePage;
