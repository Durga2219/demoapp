import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import {
  useLoginMutation,
  useRegisterMutation,
  useLogoutMutation,
  useVerifyEmailMutation,
} from "../api/authApi";
import {
  selectAuth,
  selectIsAuthenticated,
  selectUser,
  selectUserRole,
  logout,
} from "../slices/authSlice";
import { addNotification } from "../slices/uiSlice";

export const useAuth = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const auth = useSelector(selectAuth);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const user = useSelector(selectUser);
  const userRole = useSelector(selectUserRole);

  const [login, { isLoading: isLoggingIn }] = useLoginMutation();
  const [register, { isLoading: isRegistering }] = useRegisterMutation();
  const [logoutMutation] = useLogoutMutation();
  const [verifyEmail] = useVerifyEmailMutation();

  const handleLogin = async (credentials) => {
    try {
      const result = await login(credentials).unwrap();
      dispatch(
        addNotification({
          type: "success",
          message: "Login successful!",
        })
      );
      navigate("/dashboard");
      return { success: true, data: result };
    } catch (error) {
      const backendMessage = error?.data?.message || "";
      const disabled =
        backendMessage.toLowerCase().includes("disabled") ||
        backendMessage.toLowerCase().includes("verify") ||
        error?.status === 401;

      if (disabled) {
        dispatch(
          addNotification({
            type: "error",
            message: "Please verify your email before logging in.",
          })
        );
        navigate("/verify-email");
      } else {
        dispatch(
          addNotification({
            type: "error",
            message: backendMessage || "Login failed",
          })
        );
      }

      return { success: false, error };
    }
  };

  const handleRegister = async (userData) => {
    try {
      const result = await register(userData).unwrap();
      // Redirect to login and show a clear message on the login page instead of firing a toast here
      navigate("/login?registered=true");
      return { success: true, data: result };
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: error.data?.message || "Registration failed",
        })
      );
      return { success: false, error };
    }
  };

  const handleLogout = async () => {
    try {
      await logoutMutation().unwrap();
      dispatch(logout());
      navigate("/login");
    } catch (e) {
      // Even if logout fails on server, clear local state
      console.error('Logout error:', e);
      dispatch(logout());
      navigate("/login");
    }
  };

  const handleVerifyEmail = async (token) => {
    try {
      const result = await verifyEmail(token).unwrap();
      // Let the page control user-facing messaging to avoid duplicate toasts
      navigate("/login?verified=true");
      return { success: true, data: result };
    } catch (err) {
      console.error('Verify email error:', err);
      dispatch(
        addNotification({
          type: "error",
          message: err.data?.message || "Email verification failed",
        })
      );
      return { success: false, error: err };
    }
  };

  return {
    auth,
    isAuthenticated,
    user,
    userRole,
    isLoggingIn,
    isRegistering,
    handleLogin,
    handleRegister,
    handleLogout,
    handleVerifyEmail,
    isDriver: userRole === "DRIVER",
    isPassenger: userRole === "PASSENGER",
  };
};
