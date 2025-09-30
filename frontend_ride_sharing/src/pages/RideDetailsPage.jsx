import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useGetRideByIdQuery } from '../app/api/rideApi';
import { useBookRideMutation } from '../app/api/bookingApi';
import { useAuth } from '../app/hooks/useAuth';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { formatDateTime } from '../utils/formatters';
import toast from 'react-hot-toast';

const RideDetailsPage = () => {
  const { rideId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: ride, isLoading, isError } = useGetRideByIdQuery(rideId);
  const [bookRide, { isLoading: isBooking }] = useBookRideMutation();
  const [numberOfSeats, setNumberOfSeats] = useState(1);
  const [showBookingModal, setShowBookingModal] = useState(false);

  const handleBookRide = async () => {
    // Validate seats
    if (numberOfSeats > ride.availableSeats) {
      toast.error(`Only ${ride.availableSeats} seat${ride.availableSeats > 1 ? 's' : ''} available. Cannot book ${numberOfSeats} seats.`);
      return;
    }

    if (numberOfSeats <= 0) {
      toast.error('Please select at least 1 seat');
      return;
    }

    try {
      await bookRide({
        rideId,
        seatsRequested: numberOfSeats,
      }).unwrap();
      toast.success(`🎉 Booking confirmed! ${numberOfSeats} seat${numberOfSeats > 1 ? 's' : ''} booked successfully.`);
      setShowBookingModal(false);
      navigate('/bookings/my-bookings');
    } catch (err) {
      const errorMessage = err.data?.message || err.error || 'Failed to book ride';
      toast.error(errorMessage);
    }
  };

  if (isLoading) return (
    <div>
      <Header />
      <div className="max-w-3xl mx-auto p-6 flex justify-center">
        <LoadingSpinner />
      </div>
      <Footer />
    </div>
  );

  if (isError || !ride) return (
    <div>
      <Header />
      <main className="max-w-3xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
          Ride not found or error loading ride details.
        </div>
      </main>
      <Footer />
    </div>
  );

  const isDriver = user?.role === 'DRIVER';
  const isOwnRide = ride.driver?.email === user?.email || ride.driverName === user?.fullName;
  const canBook = !isDriver && !isOwnRide && ride.availableSeats > 0 && ride.status === 'ACTIVE';

  return (
    <div className="bg-gray-50 min-h-screen">
      <Header />
      <main className="max-w-4xl mx-auto p-6">
        <button
          onClick={() => navigate(-1)}
          className="mb-4 text-blue-600 hover:text-blue-800 flex items-center gap-2"
        >
          ← Back
        </button>

        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                {ride.sourceCity} → {ride.destinationCity}
              </h1>
              <p className="text-sm text-gray-500 mt-1">{ride.routeInfo}</p>
            </div>
            <span className={`px-3 py-1 rounded-full text-sm font-medium ${
              ride.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
              ride.status === 'COMPLETED' ? 'bg-gray-100 text-gray-800' :
              'bg-red-100 text-red-800'
            }`}>
              {ride.status}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <h3 className="text-sm font-semibold text-gray-600 mb-2">Departure</h3>
              <p className="text-lg">{ride.sourceAddress}</p>
              <p className="text-sm text-gray-600">{formatDateTime(ride.departureDateTime)}</p>
            </div>
            <div>
              <h3 className="text-sm font-semibold text-gray-600 mb-2">Arrival</h3>
              <p className="text-lg">{ride.destinationAddress}</p>
            </div>
          </div>

          <div className="border-t pt-6 mb-6">
            <h3 className="text-lg font-semibold mb-4">Driver Information</h3>
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center text-white font-bold">
                {ride.driverName?.charAt(0)?.toUpperCase()}
              </div>
              <div>
                <p className="font-medium">{ride.driverName}</p>
                <p className="text-sm text-gray-600">Driver</p>
              </div>
            </div>
          </div>

          {ride.vehicle && (
            <div className="border-t pt-6 mb-6">
              <h3 className="text-lg font-semibold mb-4">Vehicle Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-600">Vehicle</p>
                  <p className="font-medium">{ride.vehicle.vehicleInfo}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">License Plate</p>
                  <p className="font-medium">{ride.vehicle.licensePlate}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Color</p>
                  <p className="font-medium">{ride.vehicle.color}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Type</p>
                  <p className="font-medium">{ride.vehicle.type}</p>
                </div>
              </div>
            </div>
          )}

          <div className="border-t pt-6 grid grid-cols-3 gap-4">
            <div>
              <p className="text-sm text-gray-600">Available Seats</p>
              <p className="text-2xl font-bold text-blue-600">{ride.availableSeats}/{ride.totalSeats}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Base Fare</p>
              <p className="text-2xl font-bold text-green-600">₹{ride.baseFare}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Per Seat</p>
              <p className="text-2xl font-bold text-gray-700">₹{ride.baseFare}</p>
            </div>
          </div>

          {ride.description && (
            <div className="border-t pt-6 mt-6">
              <h3 className="text-lg font-semibold mb-2">Description</h3>
              <p className="text-gray-700">{ride.description}</p>
            </div>
          )}

          {canBook && (
            <div className="border-t pt-6 mt-6">
              <button
                onClick={() => setShowBookingModal(true)}
                className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition"
              >
                Book This Ride
              </button>
            </div>
          )}

          {isOwnRide && (
            <div className="border-t pt-6 mt-6 bg-yellow-50 rounded-lg p-4">
              <p className="text-yellow-800 text-center font-medium">
                🚗 This is your ride - You cannot book your own vehicle
              </p>
            </div>
          )}

          {!canBook && !isOwnRide && ride.availableSeats === 0 && (
            <div className="border-t pt-6 mt-6 bg-red-50 rounded-lg p-4">
              <p className="text-red-800 text-center font-medium">
                ❌ This ride is fully booked - No seats available
              </p>
            </div>
          )}

          {!canBook && !isOwnRide && ride.status !== 'ACTIVE' && (
            <div className="border-t pt-6 mt-6 bg-gray-50 rounded-lg p-4">
              <p className="text-gray-600 text-center font-medium">
                This ride is no longer active
              </p>
            </div>
          )}
        </div>
      </main>

      {/* Booking Modal */}
      {showBookingModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">Confirm Booking</h2>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Number of Seats (Max: {ride.availableSeats})
              </label>
              <input
                type="number"
                min="1"
                max={ride.availableSeats}
                value={numberOfSeats}
                onChange={(e) => {
                  const value = parseInt(e.target.value);
                  if (value > ride.availableSeats) {
                    setNumberOfSeats(ride.availableSeats);
                    toast.error(`Only ${ride.availableSeats} seats available!`);
                  } else if (value < 1) {
                    setNumberOfSeats(1);
                  } else {
                    setNumberOfSeats(value);
                  }
                }}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 mt-1">
                Available seats: {ride.availableSeats} of {ride.totalSeats}
              </p>
            </div>
            <div className="mb-4 p-4 bg-gray-50 rounded-lg">
              <div className="flex justify-between mb-2">
                <span className="text-gray-600">Fare per seat:</span>
                <span className="font-medium">₹{ride.baseFare}</span>
              </div>
              <div className="flex justify-between mb-2">
                <span className="text-gray-600">Number of seats:</span>
                <span className="font-medium">{numberOfSeats}</span>
              </div>
              <div className="flex justify-between font-bold text-lg border-t pt-2">
                <span>Total:</span>
                <span>₹{ride.baseFare * numberOfSeats}</span>
              </div>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => setShowBookingModal(false)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                disabled={isBooking}
              >
                Cancel
              </button>
              <button
                onClick={handleBookRide}
                disabled={isBooking}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {isBooking ? 'Booking...' : 'Confirm Booking'}
              </button>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};

export default RideDetailsPage;
