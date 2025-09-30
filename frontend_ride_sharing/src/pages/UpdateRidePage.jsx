import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useGetRideByIdQuery, useUpdateRideMutation, useCancelRideMutation } from '../app/api/rideApi';
import { useGetMyVehiclesQuery } from '../app/api/vehicleApi';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import LoadingSpinner from '../components/common/LoadingSpinner';
import toast from 'react-hot-toast';

const UpdateRidePage = () => {
  const { rideId } = useParams();
  const navigate = useNavigate();
  const { data: ride, isLoading: isLoadingRide } = useGetRideByIdQuery(rideId);
  const { data: vehicles } = useGetMyVehiclesQuery();
  const [updateRide, { isLoading: isUpdating }] = useUpdateRideMutation();
  const [cancelRide, { isLoading: isCancelling }] = useCancelRideMutation();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  
  const [form, setForm] = useState({
    vehicleId: '',
    sourceCity: '',
    sourceAddress: '',
    destinationCity: '',
    destinationAddress: '',
    departureDateTime: '',
    availableSeats: 1,
    baseFare: '',
    description: '',
  });

  useEffect(() => {
    if (ride) {
      // Convert datetime for input
      const dateTime = ride.departureDateTime ? new Date(ride.departureDateTime).toISOString().slice(0, 16) : '';
      
      setForm({
        vehicleId: ride.vehicle?.vehicleId || '',
        sourceCity: ride.sourceCity || '',
        sourceAddress: ride.sourceAddress || '',
        destinationCity: ride.destinationCity || '',
        destinationAddress: ride.destinationAddress || '',
        departureDateTime: dateTime,
        availableSeats: ride.availableSeats || 1,
        baseFare: ride.baseFare || '',
        description: ride.description || '',
      });
    }
  }, [ride]);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await updateRide({
        rideId,
        rideData: form,
      }).unwrap();
      toast.success('Ride updated successfully!');
      navigate('/rides/my-rides');
    } catch (err) {
      toast.error(err.data?.message || 'Failed to update ride');
    }
  };

  const handleCancelRide = async () => {
    try {
      await cancelRide({
        rideId,
        reason: cancelReason || 'Cancelled by driver',
      }).unwrap();
      toast.success('Ride cancelled successfully');
      setShowCancelModal(false);
      navigate('/rides/my-rides');
    } catch (err) {
      toast.error(err.data?.message || 'Failed to cancel ride');
    }
  };

  if (isLoadingRide) {
    return (
      <div className="bg-gray-50 min-h-screen">
        <Header />
        <div className="flex justify-center items-center py-20">
          <LoadingSpinner />
        </div>
        <Footer />
      </div>
    );
  }

  if (!ride || ride.status !== 'ACTIVE') {
    return (
      <div className="bg-gray-50 min-h-screen">
        <Header />
        <main className="max-w-4xl mx-auto p-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
            {!ride ? 'Ride not found' : 'Only active rides can be edited'}
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen">
      <Header />
      <main className="max-w-4xl mx-auto p-6">
        <button
          onClick={() => navigate('/rides/my-rides')}
          className="mb-4 text-blue-600 hover:text-blue-800 flex items-center gap-2"
        >
          ← Back to My Rides
        </button>

        <div className="bg-white rounded-lg shadow-md p-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-6">Update Ride</h1>

          <form onSubmit={onSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Vehicle *
              </label>
              <select
                name="vehicleId"
                value={form.vehicleId}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
                disabled
              >
                {vehicles && vehicles.length > 0 ? (
                  vehicles.map(v => (
                    <option key={v.vehicleId} value={v.vehicleId}>
                      {v.make} {v.model} • {v.licensePlate}
                    </option>
                  ))
                ) : (
                  <option value="">No vehicles available</option>
                )}
              </select>
              <p className="text-xs text-gray-500 mt-1">Vehicle cannot be changed after ride creation</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Source City *
                </label>
                <input
                  name="sourceCity"
                  placeholder="Source City"
                  value={form.sourceCity}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Destination City *
                </label>
                <input
                  name="destinationCity"
                  placeholder="Destination City"
                  value={form.destinationCity}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Source Address *
              </label>
              <input
                name="sourceAddress"
                placeholder="Pickup address"
                value={form.sourceAddress}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Destination Address *
              </label>
              <input
                name="destinationAddress"
                placeholder="Drop-off address"
                value={form.destinationAddress}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Departure Date & Time *
              </label>
              <input
                name="departureDateTime"
                type="datetime-local"
                value={form.departureDateTime}
                onChange={onChange}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Available Seats *
                </label>
                <input
                  name="availableSeats"
                  type="number"
                  min="1"
                  placeholder="Available Seats"
                  value={form.availableSeats}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
                <p className="text-xs text-gray-500 mt-1">
                  Current bookings: {(ride.totalSeats || 0) - (ride.availableSeats || 0)} seat(s)
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Base Fare (₹) *
                </label>
                <input
                  name="baseFare"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="Base Fare"
                  value={form.baseFare}
                  onChange={onChange}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                name="description"
                placeholder="Any additional details about the ride..."
                value={form.description}
                onChange={onChange}
                rows="4"
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div className="flex gap-3 pt-6 border-t">
              <button
                type="submit"
                disabled={isUpdating}
                className="flex-1 px-4 py-3 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:opacity-50"
              >
                {isUpdating ? 'Updating...' : 'Update Ride'}
              </button>
              <button
                type="button"
                onClick={() => setShowCancelModal(true)}
                disabled={isCancelling}
                className="flex-1 px-4 py-3 bg-red-600 text-white rounded-lg font-semibold hover:bg-red-700 focus:ring-2 focus:ring-red-500 focus:outline-none disabled:opacity-50"
              >
                Cancel Ride
              </button>
            </div>
          </form>
        </div>
      </main>

      {/* Cancel Ride Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">Cancel Ride</h2>
            <p className="text-gray-600 mb-4">
              Are you sure you want to cancel this ride? This action cannot be undone.
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Reason (optional)
              </label>
              <textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-red-500"
                rows="3"
                placeholder="Why are you cancelling this ride?"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowCancelModal(false);
                  setCancelReason('');
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                disabled={isCancelling}
              >
                Keep Ride
              </button>
              <button
                onClick={handleCancelRide}
                disabled={isCancelling}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {isCancelling ? 'Cancelling...' : 'Yes, Cancel Ride'}
              </button>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};

export default UpdateRidePage;
