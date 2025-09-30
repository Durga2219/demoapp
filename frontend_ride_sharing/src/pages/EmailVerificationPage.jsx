import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import axios from "axios";
import LoadingSpinner from "../components/common/LoadingSpinner";
import toast from "react-hot-toast";

const EmailVerificationPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const verifyEmail = async () => {
      const token = searchParams.get("token");

      if (!token) {
        toast.error("Verification token missing.");
        navigate("/verify-email?error=missing_token");
        return;
      }

      try {
        // Backend expects token as a request parameter (query string) on POST
        const url = `/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`;
        const response = await axios.post(url, null, {
          headers: { "Content-Type": "application/json" },
        });

        if (response.status === 200) {
          toast.success("Email verified — please log in.");
          navigate("/login");
        } else {
          toast.error("Email verification failed.");
          navigate("/verify-email?error=verification_failed");
        }
      } catch (err) {
        console.error(
          "Email verification error:",
          err?.response?.data || err.message || err
        );
        // If backend returns a helpful message, show it
        const message =
          err?.response?.data?.message || "Server error during verification.";
        toast.error(message);
        navigate("/verify-email?error=server_error");
      }
    };

    verifyEmail();
  }, [searchParams, navigate]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-green-50">
      <LoadingSpinner />
    </div>
  );
};

export default EmailVerificationPage;
