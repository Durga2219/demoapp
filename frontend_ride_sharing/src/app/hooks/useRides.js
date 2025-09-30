import { useDispatch } from "react-redux";
import {
  useSearchRidesQuery,
  useCreateRideMutation,
  useGetMyRidesQuery,
  useCancelRideMutation,
} from "../api/rideApi";
import { addNotification } from "../slices/uiSlice";

export const useRides = () => {
  const dispatch = useDispatch();

  const [createRide, { isLoading: isCreatingRide }] = useCreateRideMutation();
  const [cancelRide, { isLoading: isCancellingRide }] = useCancelRideMutation();

  const { data: myRides, isLoading: isLoadingMyRides } = useGetMyRidesQuery();

  const handleCreateRide = async (rideData) => {
    try {
      const result = await createRide(rideData).unwrap();
      dispatch(
        addNotification({
          type: "success",
          message: "Ride created successfully!",
        })
      );
      return { success: true, data: result };
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: error.data?.message || "Failed to create ride",
        })
      );
      return { success: false, error };
    }
  };

  const handleCancelRide = async (rideId, reason) => {
    try {
      await cancelRide({ rideId, reason }).unwrap();
      dispatch(
        addNotification({
          type: "success",
          message: "Ride cancelled successfully",
        })
      );
      return { success: true };
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: error.data?.message || "Failed to cancel ride",
        })
      );
      return { success: false, error };
    }
  };

  return {
    myRides,
    isCreatingRide,
    isCancellingRide,
    isLoadingMyRides,
    handleCreateRide,
    handleCancelRide,
  };
};
