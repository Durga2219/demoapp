import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/hooks/useAuth';
import { useGetDriverStatusQuery } from '../../app/api/driverApi';
import { useGetUnreadCountQuery, useGetUnreadNotificationsQuery, useMarkAsReadMutation } from '../../app/api/notificationApi';

const Header = () => {
  const { user, handleLogout, isDriver } = useAuth();
  const navigate = useNavigate();
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const dropdownRef = useRef(null);
  const notificationRef = useRef(null);

  // Fetch live driver verification status when user is a driver
  const { data: driverStatus } = useGetDriverStatusQuery(undefined, { skip: !isDriver });
  const driverVerified = driverStatus?.driverVerified === true || user?.driverVerified === true;
  
  // Fetch unread notification count
  const { data: unreadCount = 0 } = useGetUnreadCountQuery(undefined, {
    skip: !user,
    pollingInterval: 30000, // Poll every 30 seconds
  });

  // Fetch unread notifications
  const { data: unreadNotifications = [] } = useGetUnreadNotificationsQuery(undefined, {
    skip: !user,
    pollingInterval: 30000, // Poll every 30 seconds
  });

  // Mutation for marking notifications as read
  const [markAsRead] = useMarkAsReadMutation();

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setIsNotificationOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const toggleDropdown = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-14 items-center">
          {/* Logo */}
          <div className="flex items-center">
            <Link to="/" className="flex items-center gap-2 hover:opacity-90 transition-opacity">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-xl">🚗</span>
              </div>
              <span className="text-xl font-bold text-gray-900">
                RideShare
              </span>
            </Link>
          </div>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-1">
            <Link
              to="/rides/search"
              className="px-3 py-1.5 text-sm text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all font-medium"
            >
              Search Rides
            </Link>
            {isDriver && !driverVerified && (
              <Link
                to="/driver/verify"
                className="px-3 py-1.5 text-sm text-amber-700 hover:text-amber-800 hover:bg-amber-50 rounded-md transition-all font-medium"
              >
                Verify Driver
              </Link>
            )}
            {isDriver && driverVerified && (
              <>
                <Link
                  to="/vehicles"
                  className="px-3 py-1.5 text-sm text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all font-medium"
                >
                  Vehicles
                </Link>
                <Link
                  to="/rides/create"
                  className="px-4 py-1.5 text-sm bg-blue-600 text-white hover:bg-blue-700 rounded-md transition-all font-semibold"
                >
                  Create Ride
                </Link>
                <Link
                  to="/rides/my-rides"
                  className="px-3 py-1.5 text-sm text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all font-medium"
                >
                  My Rides
                </Link>
                <Link
                  to="/bookings/driver-bookings"
                  className="px-3 py-1.5 text-sm text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all font-medium"
                >
                  Ride Requests
                </Link>
              </>
            )}
            <Link
              to="/bookings/my-bookings"
              className="px-3 py-1.5 text-sm text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all font-medium"
            >
              Booked Rides
            </Link>
          </nav>

          {/* User Menu */}
          <div className="flex items-center gap-2">
            {user && (
              <div className="relative" ref={notificationRef}>
                <button
                  onClick={() => setIsNotificationOpen(!isNotificationOpen)}
                  className="relative p-2 text-gray-600 hover:bg-gray-100 rounded-md transition-all"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                  </svg>
                  {unreadCount > 0 && (
                    <span className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-600 rounded-full">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </button>
                
                {isNotificationOpen && (
                  <div className="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                    <div className="px-4 py-2 border-b border-gray-200 flex justify-between items-center">
                      <h3 className="font-semibold text-gray-900">Notifications</h3>
                      <Link
                        to="/notifications"
                        className="text-sm text-blue-600 hover:text-blue-700"
                        onClick={() => setIsNotificationOpen(false)}
                      >
                        View All
                      </Link>
                    </div>
                    <div className="max-h-96 overflow-y-auto">
                      {unreadNotifications.length === 0 ? (
                        <div className="px-4 py-8 text-center text-gray-500">
                          No new notifications
                        </div>
                      ) : (
                        <div>
                          {unreadNotifications.slice(0, 5).map((notification) => (
                            <div
                              key={notification.id}
                              onClick={async () => {
                                await markAsRead(notification.id);
                                setIsNotificationOpen(false);
                                if (notification.type === 'RIDE_REQUEST') {
                                  navigate('/bookings/driver-bookings');
                                }
                              }}
                              className="px-4 py-3 hover:bg-gray-50 cursor-pointer border-b border-gray-100 transition-colors"
                            >
                              <div className="flex items-start gap-3">
                                <div className="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center text-xl">
                                  {notification.type === 'RIDE_REQUEST' ? '🚗' : '🔔'}
                                </div>
                                <div className="flex-1 min-w-0">
                                  <p className="text-sm font-medium text-gray-900 truncate">
                                    {notification.title || 'New Notification'}
                                  </p>
                                  <p className="text-sm text-gray-600 line-clamp-2">
                                    {notification.message}
                                  </p>
                                  <p className="text-xs text-gray-400 mt-1">
                                    {new Date(notification.createdAt).toLocaleString()}
                                  </p>
                                </div>
                              </div>
                            </div>
                          ))}
                          {unreadNotifications.length > 5 && (
                            <div className="px-4 py-2 text-center text-sm text-gray-500">
                              + {unreadNotifications.length - 5} more notifications
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
            {user ? (
              <div className="relative" ref={dropdownRef}>
                <button
                  onClick={toggleDropdown}
                  className="flex items-center gap-2 px-2 py-1.5 text-gray-700 hover:bg-gray-50 rounded-md transition-all"
                >
                  <div className="w-7 h-7 bg-gradient-to-r from-blue-500 to-blue-600 rounded-full flex items-center justify-center text-white text-sm font-semibold">
                    {(user.firstName || user.username || 'U').charAt(0).toUpperCase()}
                  </div>
                  <span className="text-sm font-medium text-gray-900 hidden sm:inline">
                    {user.firstName || user.username}
                  </span>
                  <svg
                    className={`w-4 h-4 text-gray-500 transition-transform ${isDropdownOpen ? 'rotate-180' : ''}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {/* Dropdown Menu */}
                {isDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50 animate-fade-in">
                    {/* Profile Header */}
                    <div className="px-4 py-3 border-b border-gray-100">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full flex items-center justify-center text-white font-bold">
                          {(user.firstName || user.username || 'U').charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">
                            {user.firstName || user.username}
                          </p>
                          <p className="text-sm text-gray-500">{user.email}</p>
                        </div>
                      </div>
                    </div>

                    {/* Menu Items */}
                    <div className="py-2">
                      <Link
                        to="/profile"
                        className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-blue-600 transition-colors"
                        onClick={() => setIsDropdownOpen(false)}
                      >
                        <span className="text-lg">👤</span>
                        Profile Settings
                      </Link>

                      <Link
                        to="/dashboard"
                        className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-blue-600 transition-colors"
                        onClick={() => setIsDropdownOpen(false)}
                      >
                        <span className="text-lg">📊</span>
                        Dashboard
                      </Link>

                      {isDriver && (
                        <>
                          <Link
                            to="/rides/my-rides"
                            className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-blue-600 transition-colors"
                            onClick={() => setIsDropdownOpen(false)}
                          >
                            {/* <span className="text-lg">🚗</span> */}
                            My Rides
                          </Link>
                          <Link
                            to="/bookings/driver-bookings"
                            className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-blue-600 transition-colors"
                            onClick={() => setIsDropdownOpen(false)}
                          >
                            {/* <span className="text-lg">📨</span> */}
                            Ride Requests
                          </Link>
                        </>
                      )}

                      <Link
                        to="/bookings/my-bookings"
                        className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-blue-600 transition-colors"
                        onClick={() => setIsDropdownOpen(false)}
                      >
                        {/* <span className="text-lg">📋</span> */}
                        Booked rides
                      </Link>
                    </div>

                    {/* Logout */}
                    <div className="border-t border-gray-100 pt-2">
                      <button
                        onClick={() => {
                          handleLogout();
                          setIsDropdownOpen(false);
                        }}
                        className="flex items-center gap-3 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <span className="text-lg">🚪</span>
                        Sign Out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="px-4 py-1.5 text-sm text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all font-medium"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-1.5 text-sm bg-blue-600 text-white font-semibold rounded-md hover:bg-blue-700 transition-all"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
