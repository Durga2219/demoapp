import { baseApi } from "./baseApi";

export const bookingApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    bookRide: builder.mutation({
      query: (bookingData) => ({
        url: "/bookings",
        method: "POST",
        body: bookingData,
      }),
      invalidatesTags: ["Booking", "Ride"],
    }),

    getMyBookings: builder.query({
      query: () => "/bookings/my-bookings",
      providesTags: ["Booking"],
    }),

    getDriverBookings: builder.query({
      query: () => "/bookings/driver-bookings",
      providesTags: ["Booking"],
    }),

    getBookingById: builder.query({
      query: (bookingId) => `/bookings/${bookingId}`,
      providesTags: ["Booking"],
    }),

    confirmBooking: builder.mutation({
      query: (bookingId) => ({
        url: `/bookings/${bookingId}/confirm`,
        method: "PUT",
      }),
      invalidatesTags: ["Booking"],
    }),

    cancelBooking: builder.mutation({
      query: ({ bookingId, reason }) => ({
        url: `/bookings/${bookingId}/cancel`,
        method: "PUT",
        params: { reason },
      }),
      invalidatesTags: ["Booking"],
    }),
  }),
});

export const {
  useBookRideMutation,
  useGetMyBookingsQuery,
  useGetDriverBookingsQuery,
  useGetBookingByIdQuery,
  useConfirmBookingMutation,
  useCancelBookingMutation,
} = bookingApi;
