import React from 'react';
import { useAuth } from '../app/hooks/useAuth';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';

const Dashboard = () => {
  const { user, isDriver, isPassenger } = useAuth();

  return (
    <div className="bg-green-50 min-h-screen flex flex-col">
      <Header />
      <main className="flex-grow max-w-6xl mx-auto p-6 w-full">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h1 className="text-2xl font-bold text-gray-800">Welcome, {user?.firstName || user?.username || 'User'}</h1>
          <p className="mt-2 text-gray-600">Role: {user?.role}</p>
        </div>

        {isDriver && (
          <section className="mt-8 bg-green-100 p-6 rounded-lg shadow-md">
            <h2 className="text-xl font-semibold text-green-800">Driver Actions</h2>
            <p className="mt-2 text-gray-600">Create rides, manage vehicles, and bookings.</p>
          </section>
        )}

        {isPassenger && (
          <section className="mt-8 bg-green-100 p-6 rounded-lg shadow-md">
            <h2 className="text-xl font-semibold text-green-800">Passenger Actions</h2>
            <p className="mt-2 text-gray-600">Search rides and manage bookings.</p>
          </section>
        )}
      </main>
      <div className="mt-auto">
        <Footer />
      </div>
    </div>
  );
};

export default Dashboard;
