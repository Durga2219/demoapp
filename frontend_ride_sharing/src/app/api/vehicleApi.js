import { baseApi } from "./baseApi";

export const vehicleApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    addVehicle: builder.mutation({
      query: (vehicleData) => ({
        url: "/vehicles",
        method: "POST",
        body: vehicleData,
      }),
      invalidatesTags: ["Vehicle"],
    }),

    getMyVehicles: builder.query({
      query: () => "/vehicles/my-vehicles",
      providesTags: ["Vehicle"],
    }),

    updateVehicle: builder.mutation({
      query: ({ vehicleId, vehicleData }) => ({
        url: `/vehicles/${vehicleId}`,
        method: "PUT",
        body: vehicleData,
      }),
      invalidatesTags: ["Vehicle"],
    }),

    deleteVehicle: builder.mutation({
      query: (vehicleId) => ({
        url: `/vehicles/${vehicleId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Vehicle"],
    }),
  }),
});

export const {
  useAddVehicleMutation,
  useGetMyVehiclesQuery,
  useUpdateVehicleMutation,
  useDeleteVehicleMutation,
} = vehicleApi;
