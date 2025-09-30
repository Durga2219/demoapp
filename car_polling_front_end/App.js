import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Welcome from "./Welcome";
import Login from "./Login";
import Register from "./Register";
import PassengerDashboard from "./PassengerDashboard"; // passenger view
import DriverDashboard from "./DriverDashboard"; // driver view

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Welcome />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<PassengerDashboard />} />
        <Route path="/driver-dashboard" element={<DriverDashboard />} />
      </Routes>
    </Router>
  );
}

export default App;
