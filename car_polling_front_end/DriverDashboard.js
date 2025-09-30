import React, { useState } from "react";
import "./App.css";

function DriverDashboard() {
  const [showProfile, setShowProfile] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [showPassengers, setShowPassengers] = useState(false);

  // Ride form state
  const [rideId, setRideId] = useState("");
  const [source, setSource] = useState("");
  const [destination, setDestination] = useState("");
  const [rideDateTime, setRideDateTime] = useState("");
  const [availableSeats, setAvailableSeats] = useState("");
  const [vehicleType, setVehicleType] = useState("");
  const [licensePlate, setLicensePlate] = useState("");

  const [passengers, setPassengers] = useState([]);

  // User profile from localStorage
  const userName = localStorage.getItem("userName");
  const email = localStorage.getItem("email");
  const userType = localStorage.getItem("userType");
  const age = localStorage.getItem("age");
  const phoneNumber = localStorage.getItem("phoneNumber");
  const token = localStorage.getItem("token");

  // Post new ride
  const handlePostRide = async () => {
    if (!rideId || !source || !destination || !rideDateTime || !availableSeats || !vehicleType || !licensePlate) {
      alert("Please fill all fields");
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/rides/post`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`,
        },
        body: JSON.stringify({
          rideId: parseInt(rideId),
          source,
          destination,
          rideDateTime,
          availableSeats: parseInt(availableSeats),
          vehicleType,
          driverName: userName,
          licensePlate,
        }),
      });

      if (!response.ok) throw new Error("Failed to post ride");

      const msg = await response.text();
      alert(msg);

      // Reset form and hide it
      setRideId(""); setSource(""); setDestination(""); setRideDateTime("");
      setAvailableSeats(""); setVehicleType(""); setLicensePlate("");
      setShowForm(false);

    } catch (error) {
      console.error(error);
      alert("Error posting ride: " + error.message);
    }
  };

  // Fetch passengers of driver’s rides
  const fetchMyPassengers = async () => {
    try {
      const response = await fetch("http://localhost:8080/bookings/driver/my", {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      setPassengers(data);
      setShowPassengers(true);
    } catch (error) {
      console.error(error);
      alert("Error fetching passengers");
    }
  };

  // Logout function
  const handleLogout = async () => {
    try {
      const response = await fetch("http://localhost:8080/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok) throw new Error("Logout failed");

      alert(await response.text());
      localStorage.clear();
      window.location.href = "/login";

    } catch (error) {
      console.error(error);
      alert("Error during logout: " + error.message);
    }
  };

  return (
    <div className="fullPage" style={{ background: "#f0f2f5", minHeight: "100vh", paddingTop: "20px" }}>
      
      {/* Profile Icon */}
      <div
        className="profileIcon"
        onClick={() => setShowProfile(!showProfile)}
        title="View Profile"
      >
        👤
      </div>

      {/* Profile Card */}
      {showProfile && (
        <div className="professionalCard">
          <h2>Profile</h2>
          <p><strong>Username:</strong> {userName}</p>
          <p><strong>Email:</strong> {email}</p>
          <p><strong>User Type:</strong> {userType}</p>
          <p><strong>Age:</strong> {age}</p>
          <p><strong>Phone Number:</strong> {phoneNumber}</p>
          <button
            className="driverButton"
            style={{ marginTop: "10px", backgroundColor: "#FF4D4D", color: "#fff" }}
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      )}

      {/* Main Buttons */}
      {!showForm && !showPassengers && (
        <div style={{ textAlign: "center", marginTop: "50px", display: "flex", justifyContent: "center", gap: "20px" }}>
          <button
            className="driverButton"
            style={{ padding: "10px 20px", fontSize: "18px" }}
            onClick={() => setShowForm(true)}
          >
            Post Ride
          </button>
          <button
            className="driverButton"
            style={{ padding: "10px 20px", fontSize: "18px", backgroundColor: "#FF4D4D", color: "#fff" }}
            onClick={fetchMyPassengers}
          >
            My Passengers
          </button>
        </div>
      )}

      {/* Ride Form */}
      {showForm && (
        <div className="driverFormContainer">
          <h2>Post New Ride</h2>
          <div className="formRow">
            <input className="driverFormInput" type="number" placeholder="Ride ID" value={rideId} onChange={e => setRideId(e.target.value)} />
            <input className="driverFormInput" type="text" placeholder="Source" value={source} onChange={e => setSource(e.target.value)} />
          </div>
          <div className="formRow">
            <input className="driverFormInput" type="text" placeholder="Destination" value={destination} onChange={e => setDestination(e.target.value)} />
            <input className="driverFormInput" type="datetime-local" value={rideDateTime} onChange={e => setRideDateTime(e.target.value)} />
          </div>
          <div className="formRow">
            <input className="driverFormInput" type="number" placeholder="Available Seats" value={availableSeats} onChange={e => setAvailableSeats(e.target.value)} />
            <input className="driverFormInput" type="text" placeholder="Vehicle Type" value={vehicleType} onChange={e => setVehicleType(e.target.value)} />
          </div>
          <div className="formRow">
            <input className="driverFormInput" type="text" placeholder="License Plate" value={licensePlate} onChange={e => setLicensePlate(e.target.value)} />
            <input className="driverFormInput" type="text" placeholder="Driver Name" value={userName} readOnly />
          </div>
          <div style={{ textAlign: "center", marginTop: "20px", display: "flex", justifyContent: "center", gap: "10px" }}>
            <button className="driverButton" onClick={handlePostRide}>Submit Ride</button>
            <button className="driverButton" onClick={() => setShowForm(false)} style={{ backgroundColor: "#ccc" }}>Back</button>
          </div>
        </div>
      )}

      {/* My Passengers */}
      {showPassengers && (
        <div style={{ marginTop: "30px" }}>
          <h2 style={{ textAlign: "center" }}>My Passengers</h2>
          <div className="ridesContainer" style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "20px" }}>
            {passengers.length === 0 ? (
              <p style={{ textAlign: "center" }}>No passengers yet</p>
            ) : (
              passengers.map(p => (
                <div key={p.passengerId} style={{
                  borderRadius: "15px",
                  padding: "15px 25px",
                  background: "rgba(72, 61, 139, 0.3)",
                  color: "#fff",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center"
                }}>
                  <div>
                    <p><strong>Passenger Name:</strong> {p.passengerName}</p>
                    <p><strong>Status:</strong> {p.status}</p>
                  </div>
                  <div>
                    <p><strong>Ride From:</strong> {p.ride.source}</p>
                    <p><strong>Ride To:</strong> {p.ride.destination}</p>
                    <p><strong>Date & Time:</strong> {new Date(p.ride.rideDateTime).toLocaleString()}</p>
                  </div>
                </div>
              ))
            )}
          </div>
          <div style={{ textAlign: "center", marginTop: "20px" }}>
            <button className="driverButton" onClick={() => setShowPassengers(false)} style={{ backgroundColor: "#ccc" }}>Back</button>
          </div>
        </div>
      )}
    </div>
  );
}

export default DriverDashboard;
