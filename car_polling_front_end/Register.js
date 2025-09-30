import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

function Register() {
  const [userName, setUserName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [userType, setUserType] = useState("user"); // only user/passenger
  const [age, setAge] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);

  const navigate = useNavigate();

  // Step 1: Send OTP
  const handleSendOtp = async () => {
    try {
      const response = await fetch("http://localhost:8080/register/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userName, email }),
      });
      const data = await response.text();
      alert(data);
      if (data.includes("OTP sent")) {
        setOtpSent(true);
      }
    } catch (error) {
      console.error(error);
      alert("Failed to send OTP: " + error.message);
    }
  };

  // Step 2: Verify OTP and register
  const handleVerifyOtp = async () => {
    try {
      const response = await fetch("http://localhost:8080/register/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          otp,
          user: { userName, email, password, userType, age, phoneNumber },
        }),
      });
      const data = await response.text();
      alert(data);
      if (data === "Registration Successfully") {
        navigate("/login");
      }
    } catch (error) {
      console.error(error);
      alert("OTP Verification failed: " + error.message);
    }
  };

  return (
    <div className="fullPage">
      <h1 className="appName">Car Pooling App</h1>

      <div className="formContainerCenter registerCard">
        <h2>Register</h2>

        <input
          className="formInput"
          type="text"
          placeholder="Username"
          value={userName}
          onChange={(e) => setUserName(e.target.value)}
        />
        <input
          className="formInput"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <input
          className="formInput"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <select
          className="formInput"
          value={userType}
          onChange={(e) => setUserType(e.target.value)}
        >
          <option value="USER">USER</option>
          <option value="DRIVER">DRIVER</option>
        </select>
        <input
          className="formInput"
          type="number"
          placeholder="Age"
          value={age}
          onChange={(e) => setAge(e.target.value)}
        />
        <input
          className="formInput"
          type="text"
          placeholder="Phone Number"
          value={phoneNumber}
          onChange={(e) => setPhoneNumber(e.target.value)}
        />

        {!otpSent && (
          <button className="startButton" onClick={handleSendOtp}>
            Send OTP
          </button>
        )}

        {otpSent && (
          <>
            <input
              className="formInput"
              type="text"
              placeholder="Enter OTP"
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
            />
            <button className="startButton" onClick={handleVerifyOtp}>
              Verify & Register
            </button>
          </>
        )}
      </div>
    </div>
  );
}

export default Register;
