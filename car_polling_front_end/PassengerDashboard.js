import React, { useState, useEffect } from "react";
import "./App.css";

function PassengerDashboard() {
  const [source, setSource] = useState("");
  const [destination, setDestination] = useState("");
  const [date, setDate] = useState("");
  const [rides, setRides] = useState([]);
  const [myBookings, setMyBookings] = useState([]);
  const [showProfile, setShowProfile] = useState(false);
  const [showBookings, setShowBookings] = useState(false);

  const userName = localStorage.getItem("userName");
  const email = localStorage.getItem("email");
  const userType = localStorage.getItem("userType");
  const age = localStorage.getItem("age");
  const phoneNumber = localStorage.getItem("phoneNumber");
  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchAllRides();
    fetchMyBookings();
  }, []);

  const fetchAllRides = async () => {
    try {
      const response = await fetch("http://localhost:8080/rides/all", {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      setRides(data);
    } catch (error) {
      console.error(error);
    }
  };

  const fetchMyBookings = async () => {
    try {
      const response = await fetch("http://localhost:8080/bookings/my", {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      setMyBookings(data);
    } catch (error) {
      console.error(error);
    }
  };

  const handleSearch = async () => {
    if (!source && !destination && !date) {
      fetchAllRides();
      return;
    }

    if (!source || !destination || !date) {
      alert("Please fill all fields to search rides");
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/rides/search?source=${source}&destination=${destination}&date=${date}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const data = await response.json();
      setRides(data);
    } catch (error) {
      console.error(error);
    }
  };

  const handleBooking = async (rideId) => {
    try {
      const response = await fetch(`http://localhost:8080/bookings/book/${rideId}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      const msg = await response.text();
      alert(msg);
      fetchAllRides();
      fetchMyBookings();
    } catch (error) {
      console.error(error);
    }
  };

  const handleCancel = async (passengerId) => {
    try {
      const response = await fetch(`http://localhost:8080/bookings/cancel/${passengerId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      const msg = await response.text();
      alert(msg);
      fetchAllRides();
      fetchMyBookings();
    } catch (error) {
      console.error(error);
    }
  };

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

  const rideCardStyle = {
    borderRadius: "15px",
    padding: "15px 25px",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
    transition: "transform 0.2s",
    marginTop: "15px", // Ensures rides always below search bar
  };

  const rideInfoStyle = { flex: 1 };

  const buttonStyle = {
    padding: "8px 16px",
    borderRadius: "8px",
    border: "none",
    cursor: "pointer",
    fontWeight: "bold",
    transition: "all 0.2s",
  };

  return (
    <div className="fullPage" style={{ background: "#f0f2f5", minHeight: "100vh", padding: "20px" }}>
      
      {/* Profile Icon */}
      <div className="profileIcon" onClick={() => setShowProfile(!showProfile)} title="View Profile">
        👤
      </div>

      {/* Profile Card */}
      {showProfile && (
        <div
          className="professionalCard"
          style={{
            background: "rgba(255, 255, 255, 0.95)",
            padding: "20px",
            borderRadius: "15px",
            boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
            maxWidth: "300px",
            position: "absolute",
            top: "70px",
            right: "20px",
            color: "#333",
            zIndex: 10,
          }}
        >
          <h2>Profile</h2>
          <p><strong>Username:</strong> {userName}</p>
          <p><strong>Email:</strong> {email}</p>
          <p><strong>User Type:</strong> {userType}</p>
          <p><strong>Age:</strong> {age}</p>
          <p><strong>Phone:</strong> {phoneNumber}</p>
          <button
            style={{ marginTop: "10px", backgroundColor: "#FF4D4D", color: "#fff" }}
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      )}

      {/* Toggle Buttons */}
      <div style={{ margin: "20px 0" }}>
        <button
          onClick={() => setShowBookings(false)}
          style={{
            ...buttonStyle,
            marginRight: "10px",
            backgroundColor: !showBookings ? "#4CAF50" : "#ccc",
            color: !showBookings ? "#fff" : "#333",
          }}
        >
          All Rides
        </button>
        <button
          onClick={() => setShowBookings(true)}
          style={{
            ...buttonStyle,
            backgroundColor: showBookings ? "#FF4D4D" : "#ccc",
            color: showBookings ? "#fff" : "#333",
          }}
        >
          My Bookings
        </button>
      </div>

      {!showBookings && (
        <>
          {/* Search Bar */}
          <div className="searchBarTop" style={{ display: "flex", gap: "10px", marginBottom: "30px" }}>
            <input
              className="searchInput"
              type="text"
              placeholder="Source"
              value={source}
              onChange={e => setSource(e.target.value)}
            />
            <input
              className="searchInput"
              type="text"
              placeholder="Destination"
              value={destination}
              onChange={e => setDestination(e.target.value)}
            />
            <input
              className="searchInput"
              type="date"
              value={date}
              onChange={e => setDate(e.target.value)}
            />
            <button className="startButton" onClick={handleSearch}>
              Search
            </button>
          </div>

          {/* Ride Cards */}
          <div className="ridesContainer">
            {rides.map((ride) => (
              <div
                key={ride.id}
                style={{ ...rideCardStyle, background: "rgba(72, 61, 139, 0.3)", color: "#fff" }}
                onMouseEnter={e => e.currentTarget.style.transform = "scale(1.02)"}
                onMouseLeave={e => e.currentTarget.style.transform = "scale(1)"}
              >
                <div style={rideInfoStyle}>
                  <p><strong>Driver:</strong> {ride.driverName}</p>
                  <p><strong>Vehicle:</strong> {ride.vehicleType}</p>
                  <p><strong>License Plate:</strong> {ride.licensePlate}</p>
                </div>
                <div style={rideInfoStyle}>
                  <p><strong>From:</strong> {ride.source}</p>
                  <p><strong>To:</strong> {ride.destination}</p>
                  <p><strong>Date & Time:</strong> {new Date(ride.rideDateTime).toLocaleString()}</p>
                </div>
                <div style={{ textAlign: "center" }}>
                  <p><strong>Seats:</strong> {ride.availableSeats}</p>
                  <button
                    style={{ ...buttonStyle, backgroundColor: "#4CAF50", color: "#fff" }}
                    onClick={() => handleBooking(ride.id)}
                    disabled={ride.availableSeats <= 0}
                  >
                    {ride.availableSeats > 0 ? "Book Ride" : "Full"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {showBookings && (
        <>
          <h2>My Bookings</h2>
          <div className="ridesContainer">
            {myBookings.length === 0 ? (
              <p>No bookings yet</p>
            ) : (
              myBookings.map(b => (
                <div
                  key={b.passengerId}
                  style={{ ...rideCardStyle, background: "rgba(139, 61, 72, 0.3)", color: "#fff" }}
                  onMouseEnter={e => e.currentTarget.style.transform = "scale(1.02)"}
                  onMouseLeave={e => e.currentTarget.style.transform = "scale(1)"}
                >
                  <div style={rideInfoStyle}>
                    <p><strong>Ride by:</strong> {b.ride.driverName}</p>
                    <p><strong>Vehicle:</strong> {b.ride.vehicleType}</p>
                  </div>
                  <div style={rideInfoStyle}>
                    <p><strong>From:</strong> {b.ride.source}</p>
                    <p><strong>To:</strong> {b.ride.destination}</p>
                    <p><strong>Date & Time:</strong> {new Date(b.ride.rideDateTime).toLocaleString()}</p>
                  </div>
                  <div style={{ textAlign: "center" }}>
                    <button
                      style={{ ...buttonStyle, backgroundColor: "#FF4D4D", color: "#fff" }}
                      onClick={() => handleCancel(b.passengerId)}
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}

export default PassengerDashboard;
