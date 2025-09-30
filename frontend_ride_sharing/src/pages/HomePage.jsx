import React from 'react';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';

const HomePage = () => {
	return (
		<div className="bg-gradient-to-r from-green-50 to-green-100 min-h-screen">
			<Header />
			<main className="max-w-6xl mx-auto p-6">
				<div className="bg-white p-8 rounded-lg shadow-md text-center">
					<h1 className="text-3xl font-bold text-gray-800">Welcome to RideShare</h1>
					<p className="mt-4 text-gray-600 text-lg">Find or offer rides in your city with ease and convenience.</p>
				</div>
			</main>
			<Footer />
		</div>
	);
};

export default HomePage;
