import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./App.css";

function Login() {
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const handleLogin = async () => {
    try {
      // Step 1: login and get token
      const response = await fetch("http://localhost:8080/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userName, password }),
      });

      if (!response.ok) throw new Error("Invalid credentials");

      const data = await response.json();
      if (!data.token) {
        alert(data);
        return;
      }

      localStorage.setItem("token", data.token);

      // Step 2: fetch profile
      const profileResponse = await fetch(`http://localhost:8080/profile/${userName}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${data.token}`,
        },
      });

      if (!profileResponse.ok) throw new Error("Failed to fetch profile");

      const profileData = await profileResponse.json();

      // Save profile info in localStorage
      localStorage.setItem("userName", profileData.userName);
      localStorage.setItem("email", profileData.email);
      localStorage.setItem("userType", profileData.userType);
      localStorage.setItem("age", profileData.age || ""); // optional for driver
      localStorage.setItem("phoneNumber", profileData.phoneNumber || "");

      alert("Login successful!");

      // Step 3: Redirect based on user type
      if (profileData.userType.toLowerCase() === "user") {
        navigate("/dashboard"); // Passenger dashboard
      } else if (profileData.userType.toLowerCase() === "driver") {
        navigate("/driver-dashboard"); // Driver dashboard
      } else {
        alert("Unknown user type");
      }
    } catch (error) {
      console.error(error);
      alert("Login failed: " + error.message);
    }
  };

  return (
    <div className="fullPage">
      <h1 className="appName">Car Pooling App</h1>

      <div className="formContainerCenter loginCard">
        <h2>Login</h2>
        <input
          className="formInput"
          type="text"
          placeholder="Username"
          value={userName}
          onChange={(e) => setUserName(e.target.value)}
        />
        <input
          className="formInput"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button className="startButton" onClick={handleLogin}>
          Login
        </button>
        <div className="linkContainer">
          <Link to="/register" className="linkButton">
            Register
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Login;
