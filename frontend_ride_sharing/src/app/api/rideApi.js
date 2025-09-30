import { baseApi } from "./baseApi";

export const rideApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    searchRides: builder.query({
      query: ({ source, destination, date }) => ({
        url: "/rides/search",
        params: { source, destination, date },
      }),
      providesTags: ["Ride"],
    }),

    createRide: builder.mutation({
      query: (rideData) => ({
        url: "/rides",
        method: "POST",
        body: rideData,
      }),
      invalidatesTags: ["Ride"],
    }),

    getMyRides: builder.query({
      query: () => "/rides/my-rides",
      providesTags: ["Ride"],
    }),

    getRideById: builder.query({
      query: (rideId) => `/rides/${rideId}`,
      providesTags: ["Ride"],
    }),

    updateRide: builder.mutation({
      query: ({ rideId, rideData }) => ({
        url: `/rides/${rideId}`,
        method: "PUT",
        body: rideData,
      }),
      invalidatesTags: ["Ride"],
    }),

    cancelRide: builder.mutation({
      query: ({ rideId, reason }) => ({
        url: `/rides/${rideId}`,
        method: "DELETE",
        params: { reason },
      }),
      invalidatesTags: ["Ride"],
    }),
  }),
});

export const {
  useSearchRidesQuery,
  useLazySearchRidesQuery,
  useCreateRideMutation,
  useGetMyRidesQuery,
  useGetRideByIdQuery,
  useUpdateRideMutation,
  useCancelRideMutation,
} = rideApi;
