import React, { useState } from 'react';
import { useGetMyRidesQuery } from '../app/api/rideApi';
import { useGetDriverBookingsQuery } from '../app/api/bookingApi';
import { useNavigate } from 'react-router-dom';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { formatDateTime } from '../utils/formatters';

const MyRidesPage = () => {
  const { data: myRides, isLoading } = useGetMyRidesQuery();
  const { data: driverBookings } = useGetDriverBookingsQuery();
  const navigate = useNavigate();
  const [expandedRide, setExpandedRide] = useState(null);

  // Get bookings for a specific ride
  const getRideBookings = (rideId) => {
    if (!driverBookings) return [];
    return driverBookings.filter(booking => booking.ride?.rideId === rideId);
  };

  // Get booking counts for a ride
  const getBookingStats = (rideId) => {
    const bookings = getRideBookings(rideId);
    return {
      total: bookings.length,
      pending: bookings.filter(b => b.status === 'PENDING').length,
      confirmed: bookings.filter(b => b.status === 'CONFIRMED').length,
      cancelled: bookings.filter(b => b.status === 'CANCELLED').length,
    };
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'COMPLETED':
        return 'bg-blue-100 text-blue-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <Header />
      <main className="max-w-6xl mx-auto p-6">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900">My Rides</h1>
          <button
            onClick={() => navigate('/rides/create')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium"
          >
            + Create New Ride
          </button>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <LoadingSpinner />
          </div>
        ) : myRides && myRides.length > 0 ? (
          <div className="grid grid-cols-1 gap-4">
            {myRides.map((ride) => (
              <div
                key={ride.rideId}
                className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition"
              >
                <div className="flex justify-between items-start mb-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h2 className="text-2xl font-bold text-gray-900">
                        {ride.sourceCity} → {ride.destinationCity}
                      </h2>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(ride.status)}`}>
                        {ride.status}
                      </span>
                    </div>
                    <p className="text-sm text-gray-500">{ride.routeInfo}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-blue-600">₹{ride.baseFare}</p>
                    <p className="text-sm text-gray-500">per seat</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                  <div>
                    <p className="text-sm text-gray-600">Departure</p>
                    <p className="font-medium">{ride.sourceAddress}</p>
                    <p className="text-sm text-gray-500">{formatDateTime(ride.departureDateTime)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Destination</p>
                    <p className="font-medium">{ride.destinationAddress}</p>
                  </div>
                </div>

                {ride.vehicle && (
                  <div className="border-t pt-4 mb-4">
                    <p className="text-sm text-gray-600">Vehicle</p>
                    <p className="font-medium">{ride.vehicle.vehicleInfo} • {ride.vehicle.licensePlate}</p>
                  </div>
                )}

                <div className="border-t pt-4 mb-4">
                  <div className="grid grid-cols-4 gap-4">
                    <div>
                      <p className="text-sm text-gray-600">Available Seats</p>
                      <p className="text-lg font-bold text-green-600">{ride.availableSeats}/{ride.totalSeats}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Booked Seats</p>
                      <p className="text-lg font-bold text-blue-600">{ride.totalSeats - ride.availableSeats}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Total Bookings</p>
                      <p className="text-lg font-bold text-purple-600">{getBookingStats(ride.rideId).total}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Revenue</p>
                      <p className="text-lg font-bold text-green-600">
                        ₹{ride.baseFare * (ride.totalSeats - ride.availableSeats)}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Booking Statistics */}
                {getBookingStats(ride.rideId).total > 0 && (
                  <div className="border-t pt-4 mb-4">
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="text-sm font-semibold text-gray-700">Booking Status</h3>
                      <button
                        onClick={() => setExpandedRide(expandedRide === ride.rideId ? null : ride.rideId)}
                        className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                      >
                        {expandedRide === ride.rideId ? '▼ Hide Details' : '▶ Show Details'}
                      </button>
                    </div>
                    <div className="grid grid-cols-3 gap-2">
                      {getBookingStats(ride.rideId).pending > 0 && (
                        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-center">
                          <p className="text-2xl font-bold text-yellow-700">{getBookingStats(ride.rideId).pending}</p>
                          <p className="text-xs text-yellow-600">Pending</p>
                        </div>
                      )}
                      {getBookingStats(ride.rideId).confirmed > 0 && (
                        <div className="bg-green-50 border border-green-200 rounded-lg p-3 text-center">
                          <p className="text-2xl font-bold text-green-700">{getBookingStats(ride.rideId).confirmed}</p>
                          <p className="text-xs text-green-600">Confirmed</p>
                        </div>
                      )}
                      {getBookingStats(ride.rideId).cancelled > 0 && (
                        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-center">
                          <p className="text-2xl font-bold text-red-700">{getBookingStats(ride.rideId).cancelled}</p>
                          <p className="text-xs text-red-600">Cancelled</p>
                        </div>
                      )}
                    </div>
                    
                    {/* Expanded Bookings List */}
                    {expandedRide === ride.rideId && (
                      <div className="mt-4 space-y-2">
                        {getRideBookings(ride.rideId).map((booking) => (
                          <div key={booking.bookingId} className="bg-gray-50 rounded-lg p-3 flex justify-between items-center">
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 bg-purple-500 rounded-full flex items-center justify-center text-white font-bold">
                                {booking.passengerName?.charAt(0)?.toUpperCase()}
                              </div>
                              <div>
                                <p className="font-medium text-gray-900">{booking.passengerName}</p>
                                <p className="text-xs text-gray-500">{booking.seatsBooked} seat(s) • ₹{booking.totalFare}</p>
                              </div>
                            </div>
                            <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                              booking.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                              booking.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                              'bg-red-100 text-red-800'
                            }`}>
                              {booking.status}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                <div className="flex gap-3 border-t pt-4">
                  <button
                    onClick={() => navigate(`/rides/${ride.rideId}`)}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 font-medium"
                  >
                    View Details
                  </button>
                  {ride.status === 'ACTIVE' && (
                    <button
                      onClick={() => navigate(`/rides/edit/${ride.rideId}`)}
                      className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium"
                    >
                      Edit Ride
                    </button>
                  )}
                  <button
                    onClick={() => navigate('/bookings/driver-bookings')}
                    className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 font-medium"
                  >
                    View Bookings
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow-md p-12 text-center">
            <p className="text-gray-600 mb-4">You haven't created any rides yet.</p>
            <button
              onClick={() => navigate('/rides/create')}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              Create Your First Ride
            </button>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
};

export default MyRidesPage;
