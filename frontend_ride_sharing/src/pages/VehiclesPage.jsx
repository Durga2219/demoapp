import React, { useState } from 'react';
import { useGetMyVehiclesQuery, useDeleteVehicleMutation, useAddVehicleMutation } from '../app/api/vehicleApi';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import toast from 'react-hot-toast';

const VehiclesPage = () => {
  const { data: vehicles, isLoading, isError, refetch } = useGetMyVehiclesQuery();
  const [deleteVehicle] = useDeleteVehicleMutation();
  const [addVehicle, { isLoading: isAdding }] = useAddVehicleMutation();
  const [form, setForm] = useState({
    make: '',
    model: '',
    color: '',
    licensePlate: '',
    year: '',
    capacity: '',
    type: 'SEDAN',
    images: null,
  });

  const onChange = (e) => {
    const { name, value, files } = e.target;
    if (name === "images") {
      setForm({ ...form, images: files });
    } else {
      setForm({ ...form, [name]: value });
    }
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const formData = new FormData();

    formData.append('make', form.make.trim());
    formData.append('model', form.model.trim());
    formData.append('licensePlate', form.licensePlate.trim());
    formData.append('year', form.year);
    formData.append('capacity', form.capacity);
    formData.append('type', form.type);
    if (form.color) {
      formData.append('color', form.color.trim());
    }

    if (form.images) {
      for (let i = 0; i < form.images.length; i++) {
        formData.append('images', form.images[i]);
      }
    }

    try {
      await addVehicle(formData).unwrap();
      toast.success('Vehicle added successfully!');
      setForm({ make: '', model: '', color: '', licensePlate: '', year: '', capacity: '', type: 'SEDAN', images: null });
      e.target.reset();
    } catch (err) {
      toast.error(err.data?.message || 'Failed to add vehicle.');
    }
  };

  return (
    <div className="bg-green-50 min-h-screen">
      <Header />
      <main className="max-w-6xl mx-auto p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">My Vehicles</h1>
        <div className="space-y-4">
          {isLoading && <LoadingSpinner />}
          {isError && <ErrorMessage message="Could not load your vehicles." onRetry={refetch} />}
          {vehicles?.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {vehicles.map((v) => (
                <div key={v.vehicleId || v.id} className="bg-white shadow rounded-lg p-4 flex justify-between items-center">
                  <div>
                    <h2 className="text-lg font-semibold text-gray-700">{v.make} {v.model}</h2>
                    <p className="text-sm text-gray-500">{v.licensePlate} • {v.year} • {v.capacity} seats</p>
                  </div>
                  <button
                    onClick={() => deleteVehicle(v.vehicleId || v.id)}
                    className="text-red-600 hover:text-red-800 text-sm font-medium"
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          ) : (
            !isLoading && <p className="text-gray-600">No vehicles added yet.</p>
          )}
        </div>

        <h2 className="text-xl font-bold text-gray-800 mt-10">Add a Vehicle</h2>
        <form onSubmit={onSubmit} className="mt-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <input
            name="make"
            placeholder="Make"
            value={form.make}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
            required
          />
          <input
            name="model"
            placeholder="Model"
            value={form.model}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
            required
          />
          <input
            name="color"
            placeholder="Color"
            value={form.color}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
          />
          <input
            name="licensePlate"
            placeholder="License Plate"
            value={form.licensePlate}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
            required
          />
          <input
            name="year"
            type="number"
            placeholder="Year"
            value={form.year}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
            required
          />
          <input
            name="capacity"
            type="number"
            placeholder="Capacity"
            value={form.capacity}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
            required
          />
          <select
            name="type"
            value={form.type}
            onChange={onChange}
            className="px-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:outline-none"
          >
            <option value="SEDAN">SEDAN</option>
            <option value="SUV">SUV</option>
            <option value="HATCHBACK">HATCHBACK</option>
            <option value="COUPE">COUPE</option>
            <option value="CONVERTIBLE">CONVERTIBLE</option>
            <option value="PICKUP_TRUCK">PICKUP_TRUCK</option>
            <option value="MINIVAN">MINIVAN</option>
            <option value="CROSSOVER">CROSSOVER</option>
          </select>
          <div className="lg:col-span-3">
            <label className="block text-sm font-medium text-gray-700 mb-2">Vehicle Images (optional)</label>
            <input
              name="images"
              type="file"
              multiple
              accept="image/*"
              onChange={onChange}
              className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100"
            />
          </div>
          <div className="lg:col-span-3">
            <button
              className="w-full px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 focus:ring-2 focus:ring-green-500 focus:outline-none"
              disabled={isAdding}
            >
              {isAdding && <LoadingSpinner size="sm" />} Add Vehicle
            </button>
          </div>
        </form>
      </main>
      <Footer />
    </div>
  );
};

export default VehiclesPage;
