import React, { useState } from 'react';
import LoadingSpinner from '../common/LoadingSpinner';
import { useAuth } from '../../app/hooks/useAuth';

const RegisterForm = () => {
	const { handleRegister, isRegistering } = useAuth();
	const [formData, setFormData] = useState({
		username: '',
		email: '',
		password: '',
		firstName: '',
		lastName: '',
		role: 'PASSENGER',
		phoneNumber: '',
		profilePicture: null,
	});

	const [errors, setErrors] = useState({});

	const handleChange = (e) => {
		const { name, value, files } = e.target;
		setFormData({ ...formData, [name]: files ? files[0] : value });
		if (errors[name]) setErrors({ ...errors, [name]: '' });
	};

	const validateForm = () => {
		const newErrors = {};
		if (!formData.username) newErrors.username = 'Username is required';
		if (!formData.email) newErrors.email = 'Email is required';
		if (!formData.password || formData.password.length < 8)
			newErrors.password = 'Password must be at least 8 characters';
		if (!formData.firstName) newErrors.firstName = 'First name is required';
		if (!formData.lastName) newErrors.lastName = 'Last name is required';
		return newErrors;
	};

	const onSubmit = async (e) => {
		e.preventDefault();
		const newErrors = validateForm();
		if (Object.keys(newErrors).length > 0) return setErrors(newErrors);
		await handleRegister(formData);
	};

	return (
		<form onSubmit={onSubmit} className="space-y-4">
			<input name="username" placeholder="Username" value={formData.username} onChange={handleChange} className="w-full px-3 py-2 border rounded" />
			{errors.username && <p className="text-sm text-red-600">{errors.username}</p>}

			<input name="email" type="email" placeholder="Email" value={formData.email} onChange={handleChange} className="w-full px-3 py-2 border rounded" />
			{errors.email && <p className="text-sm text-red-600">{errors.email}</p>}

			<input name="password" type="password" placeholder="Password" value={formData.password} onChange={handleChange} className="w-full px-3 py-2 border rounded" />
			{errors.password && <p className="text-sm text-red-600">{errors.password}</p>}

			<div className="grid grid-cols-2 gap-2">
				<input name="firstName" placeholder="First name" value={formData.firstName} onChange={handleChange} className="w-full px-3 py-2 border rounded" />
				<input name="lastName" placeholder="Last name" value={formData.lastName} onChange={handleChange} className="w-full px-3 py-2 border rounded" />
			</div>

			<input name="phoneNumber" placeholder="Phone number" value={formData.phoneNumber} onChange={handleChange} className="w-full px-3 py-2 border rounded" />

			<select name="role" value={formData.role} onChange={handleChange} className="w-full px-3 py-2 border rounded">
				<option value="PASSENGER">Passenger</option>
				<option value="DRIVER">Driver</option>
			</select>

			<div>
				<label className="block text-sm mb-1">Profile picture (optional)</label>
				<input name="profilePicture" type="file" accept="image/*" onChange={handleChange} />
			</div>

			<button type="submit" disabled={isRegistering} className="w-full py-2 bg-blue-600 text-white rounded">
				{isRegistering ? <LoadingSpinner size="sm" /> : 'Register'}
			</button>
		</form>
	);
};

export default RegisterForm;
