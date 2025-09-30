import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

function Welcome() {
  const [started, setStarted] = useState(false);
  const navigate = useNavigate();

  return (
    <div
      className="fullPage"
      style={{
        backgroundImage: `url('https://tse3.mm.bing.net/th/id/OIP.URsoANCpEekGqGnRq1W2ggHaE8?pid=Api')`,
        backgroundSize: "cover",
        backgroundPosition: "center",
        color: "white",
        textAlign: "center",
      }}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          alignItems: "center",
          height: "100vh",
          gap: "20px",
        }}
      >
        {!started && (
          <>
            <h1 className="greeting">Car Pooling Website</h1>
            <p>Connect with drivers and passengers easily.</p>
            <button className="startButton" onClick={() => setStarted(true)}>
              Get Started
            </button>
          </>
        )}

        {started && (
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <button
              className="startButton"
              onClick={() => navigate("/login")}
            >
              Login
            </button>
            <button
              className="startButton"
              onClick={() => navigate("/register")}
            >
              Register
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default Welcome;
