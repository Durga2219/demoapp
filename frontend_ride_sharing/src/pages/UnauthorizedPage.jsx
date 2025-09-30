import React from 'react';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';

const UnauthorizedPage = () => {
  return (
    <div>
      <Header />
      <main className="max-w-3xl mx-auto p-6 text-center">
        <h1 className="text-2xl font-semibold">Unauthorized</h1>
        <p className="mt-2 text-gray-600">You do not have permission to view this page.</p>
      </main>
      <Footer />
    </div>
  );
};

export default UnauthorizedPage;
