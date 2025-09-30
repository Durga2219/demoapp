import { useDispatch } from "react-redux";
import {
  useBookRideMutation,
  useGetMyBookingsQuery,
  useGetDriverBookingsQuery,
  useCancelBookingMutation,
  useConfirmBookingMutation,
} from "../api/bookingApi";
import { addNotification } from "../slices/uiSlice";

export const useBookings = () => {
  const dispatch = useDispatch();

  const [bookRide, { isLoading: isBookingRide }] = useBookRideMutation();
  const [cancelBooking, { isLoading: isCancellingBooking }] =
    useCancelBookingMutation();
  const [confirmBooking, { isLoading: isConfirmingBooking }] =
    useConfirmBookingMutation();

  const { data: myBookings, isLoading: isLoadingMyBookings } =
    useGetMyBookingsQuery();
  const { data: driverBookings, isLoading: isLoadingDriverBookings } =
    useGetDriverBookingsQuery();

  const handleBookRide = async (bookingData) => {
    try {
      const result = await bookRide(bookingData).unwrap();
      dispatch(
        addNotification({
          type: "success",
          message: "Ride booked successfully!",
        })
      );
      return { success: true, data: result };
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: error.data?.message || "Failed to book ride",
        })
      );
      return { success: false, error };
    }
  };

  const handleCancelBooking = async (bookingId, reason) => {
    try {
      const result = await cancelBooking({ bookingId, reason }).unwrap();
      dispatch(
        addNotification({
          type: "success",
          message: "Booking cancelled successfully",
        })
      );
      return { success: true, data: result };
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: error.data?.message || "Failed to cancel booking",
        })
      );
      return { success: false, error };
    }
  };

  const handleConfirmBooking = async (bookingId) => {
    try {
      const result = await confirmBooking(bookingId).unwrap();
      dispatch(
        addNotification({
          type: "success",
          message: "Booking confirmed successfully",
        })
      );
      return { success: true, data: result };
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: error.data?.message || "Failed to confirm booking",
        })
      );
      return { success: false, error };
    }
  };

  return {
    myBookings,
    driverBookings,
    isBookingRide,
    isCancellingBooking,
    isConfirmingBooking,
    isLoadingMyBookings,
    isLoadingDriverBookings,
    handleBookRide,
    handleCancelBooking,
    handleConfirmBooking,
  };
};
