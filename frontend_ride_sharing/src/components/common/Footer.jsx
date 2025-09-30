import React from 'react';

const Footer = () => {
	return (
		<footer className="bg-white border-t mt-8">
			<div className="max-w-7xl mx-auto px-4 py-6 text-center text-sm text-gray-500">
				© {new Date().getFullYear()} RideShare — Built with ❤
			</div>
		</footer>
	);
};

export default Footer;
