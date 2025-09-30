import React, { useState } from 'react';
import { useLazySearchRidesQuery } from '../app/api/rideApi';
import { useNavigate } from 'react-router-dom';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { formatDateTime } from '../utils/formatters';

const SearchRidesPage = () => {
  const navigate = useNavigate();
  const [params, setParams] = useState({ source: '', destination: '', date: '' });
  const [trigger, { data: results, isFetching }] = useLazySearchRidesQuery();
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [selectedRide, setSelectedRide] = useState(null);
  const [numberOfSeats, setNumberOfSeats] = useState(1);

  const onChange = (e) => setParams({ ...params, [e.target.name]: e.target.value });
  
  const onSearch = async (e) => {
    e.preventDefault();
    await trigger(params);
  };

  const handleRideClick = (rideId) => {
    navigate(`/rides/${rideId}`);
  };

  const handleBookNowClick = (e, ride) => {
    e.stopPropagation();
    setSelectedRide(ride);
    setNumberOfSeats(1);
    setShowBookingModal(true);
  };

  const handleConfirmBooking = () => {
    // Navigate to ride details with seat parameter
    navigate(`/rides/${selectedRide.rideId}`, { 
      state: { preSelectedSeats: numberOfSeats } 
    });
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <Header />
      <main className="max-w-6xl mx-auto p-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Search Rides</h1>
        
        <form onSubmit={onSearch} className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Source City
              </label>
              <input 
                name="source" 
                placeholder="e.g., Almora" 
                value={params.source} 
                onChange={onChange} 
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Destination City
              </label>
              <input 
                name="destination" 
                placeholder="e.g., Delhi" 
                value={params.destination} 
                onChange={onChange} 
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Travel Date
              </label>
              <input 
                name="date" 
                type="date" 
                value={params.date} 
                onChange={onChange} 
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>
          </div>
          <div className="mt-4">
            <button 
              type="submit"
              disabled={isFetching}
              className="w-full px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:outline-none transition disabled:opacity-50"
            >
              {isFetching ? 'Searching...' : 'Search Rides'}
            </button>
          </div>
        </form>

        <div className="space-y-4">
          {isFetching && (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          )}
          
          {!isFetching && results?.length === 0 && (
            <div className="bg-white rounded-lg shadow-md p-12 text-center">
              <p className="text-gray-600 text-lg mb-2">No rides found</p>
              <p className="text-gray-500 text-sm">Try adjusting your search criteria</p>
            </div>
          )}
          
          {!isFetching && results && results.length > 0 && (
            <>
              <div className="text-gray-700 font-medium mb-4">
                Found {results.length} ride{results.length > 1 ? 's' : ''}
              </div>
              {results.map((ride) => (
                <div 
                  key={ride.rideId} 
                  onClick={() => handleRideClick(ride.rideId)}
                  className="bg-white rounded-lg shadow-md hover:shadow-xl transition cursor-pointer overflow-hidden"
                >
                  <div className="flex flex-col md:flex-row">
                    {/* Vehicle Image */}
                    <div className="md:w-1/3 h-48 md:h-auto bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center relative overflow-hidden">
                      {ride.vehicle?.images && ride.vehicle.images.length > 0 ? (
                        <img 
                          src={ride.vehicle.images[0]} 
                          alt={`${ride.vehicle.make} ${ride.vehicle.model}`}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="text-center text-white p-6">
                          <svg className="w-24 h-24 mx-auto mb-2 opacity-80" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z"/>
                            <path d="M3 4a1 1 0 00-1 1v10a1 1 0 001 1h1.05a2.5 2.5 0 014.9 0H10a1 1 0 001-1V5a1 1 0 00-1-1H3zM14 7a1 1 0 00-1 1v6.05A2.5 2.5 0 0115.95 16H17a1 1 0 001-1v-5a1 1 0 00-.293-.707l-2-2A1 1 0 0015 7h-1z"/>
                          </svg>
                          <p className="text-lg font-semibold">
                            {ride.vehicle?.make} {ride.vehicle?.model}
                          </p>
                          <p className="text-sm opacity-90">{ride.vehicle?.licensePlate}</p>
                        </div>
                      )}
                    </div>

                    {/* Ride Details */}
                    <div className="flex-1 p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div>
                          <h3 className="text-2xl font-bold text-gray-900 mb-1">
                            {ride.sourceCity} → {ride.destinationCity}
                          </h3>
                          <p className="text-sm text-gray-500">{ride.routeInfo}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-3xl font-bold text-blue-600">₹{ride.baseFare}</p>
                          <p className="text-sm text-gray-500">per seat</p>
                        </div>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                        <div>
                          <p className="text-xs text-gray-500 mb-1">Departure</p>
                          <p className="font-medium text-gray-800">{ride.sourceAddress}</p>
                          <p className="text-sm text-gray-600">{formatDateTime(ride.departureDateTime)}</p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-500 mb-1">Arrival</p>
                          <p className="font-medium text-gray-800">{ride.destinationAddress}</p>
                        </div>
                      </div>

                      <div className="flex items-center justify-between border-t pt-4">
                        <div className="flex items-center gap-6">
                          <div className="flex items-center gap-2">
                            <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
                              <span className="text-purple-600 font-bold">
                                {ride.driverName?.charAt(0)?.toUpperCase()}
                              </span>
                            </div>
                            <div>
                              <p className="text-sm text-gray-500">Driver</p>
                              <p className="font-medium">{ride.driverName}</p>
                            </div>
                          </div>
                          
                          <div className="border-l pl-6">
                            <p className="text-sm text-gray-500">Available Seats</p>
                            <p className="text-2xl font-bold text-green-600">{ride.availableSeats}</p>
                          </div>

                          {ride.vehicle && (
                            <div className="border-l pl-6">
                              <p className="text-sm text-gray-500">Vehicle</p>
                              <p className="font-medium">{ride.vehicle.vehicleInfo}</p>
                            </div>
                          )}
                        </div>

                        <button 
                          onClick={(e) => handleBookNowClick(e, ride)}
                          className="px-6 py-2 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition"
                        >
                          Book Now
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}
        </div>
      </main>

      {/* Seat Selection Modal */}
      {showBookingModal && selectedRide && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">Select Seats</h2>
            
            <div className="mb-4 p-4 bg-gray-50 rounded-lg">
              <div className="flex justify-between mb-2">
                <span className="font-medium">{selectedRide.sourceCity} → {selectedRide.destinationCity}</span>
                <span className="text-sm text-gray-600">{formatDateTime(selectedRide.departureDateTime)}</span>
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Number of Seats (Max: {selectedRide.availableSeats})
              </label>
              <input
                type="number"
                min="1"
                max={selectedRide.availableSeats}
                value={numberOfSeats}
                onChange={(e) => {
                  const value = parseInt(e.target.value);
                  if (value > selectedRide.availableSeats) {
                    setNumberOfSeats(selectedRide.availableSeats);
                  } else if (value < 1) {
                    setNumberOfSeats(1);
                  } else {
                    setNumberOfSeats(value);
                  }
                }}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 text-center text-2xl font-bold"
              />
              <div className="flex justify-between mt-2">
                <button
                  onClick={() => setNumberOfSeats(Math.max(1, numberOfSeats - 1))}
                  className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 font-bold"
                >
                  -
                </button>
                <span className="text-sm text-gray-500 self-center">
                  Available: {selectedRide.availableSeats} of {selectedRide.totalSeats}
                </span>
                <button
                  onClick={() => setNumberOfSeats(Math.min(selectedRide.availableSeats, numberOfSeats + 1))}
                  className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 font-bold"
                >
                  +
                </button>
              </div>
            </div>

            <div className="mb-4 p-4 bg-blue-50 rounded-lg border-2 border-blue-200">
              <div className="flex justify-between mb-2">
                <span className="text-gray-600">Fare per seat:</span>
                <span className="font-medium">₹{selectedRide.baseFare}</span>
              </div>
              <div className="flex justify-between mb-2">
                <span className="text-gray-600">Number of seats:</span>
                <span className="font-medium">{numberOfSeats}</span>
              </div>
              <div className="flex justify-between font-bold text-xl border-t-2 border-blue-300 pt-2 mt-2">
                <span>Total Fare:</span>
                <span className="text-blue-600">₹{selectedRide.baseFare * numberOfSeats}</span>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowBookingModal(false);
                  setSelectedRide(null);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmBooking}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-semibold"
              >
                Continue to Book
              </button>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};

export default SearchRidesPage;
