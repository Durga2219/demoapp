// src/utils/helpers.js
export const debounce = (func, wait) => {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

export const throttle = (func, limit) => {
  let inThrottle;
  return function () {
    const args = arguments;
    const context = this;
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
};

export const generateBookingReference = () => {
  return Math.random().toString(36).substr(2, 8).toUpperCase();
};

export const calculateFare = (baseFare, seats, distance = 1) => {
  return baseFare * seats * distance;
};

export const getTimeUntilDeparture = (departureDateTime) => {
  const now = new Date();
  const departure = new Date(departureDateTime);
  const diffMs = departure - now;

  if (diffMs <= 0) return "Departed";

  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

  if (diffHours > 24) {
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} day${diffDays > 1 ? "s" : ""}`;
  } else if (diffHours > 0) {
    return `${diffHours}h ${diffMinutes}m`;
  } else {
    return `${diffMinutes}m`;
  }
};

export const sortRides = (rides, sortBy, sortOrder) => {
  return [...rides].sort((a, b) => {
    let aVal, bVal;

    switch (sortBy) {
      case "departureTime":
        aVal = new Date(a.departureDateTime);
        bVal = new Date(b.departureDateTime);
        break;
      case "price":
        aVal = parseFloat(a.baseFare);
        bVal = parseFloat(b.baseFare);
        break;
      case "availableSeats":
        aVal = a.availableSeats;
        bVal = b.availableSeats;
        break;
      default:
        return 0;
    }

    if (sortOrder === "desc") {
      return bVal > aVal ? 1 : -1;
    }
    return aVal > bVal ? 1 : -1;
  });
};

export const filterRides = (rides, filters) => {
  return rides.filter((ride) => {
    if (filters.minPrice && parseFloat(ride.baseFare) < filters.minPrice)
      return false;
    if (filters.maxPrice && parseFloat(ride.baseFare) > filters.maxPrice)
      return false;
    if (filters.seatsRequired && ride.availableSeats < filters.seatsRequired)
      return false;
    if (filters.departureTime) {
      const rideTime = new Date(ride.departureDateTime).getTime();
      const filterTime = new Date(filters.departureTime).getTime();
      const timeDiff = Math.abs(rideTime - filterTime) / (1000 * 60 * 60); // hours
      if (timeDiff > 2) return false; // Within 2 hours
    }
    return true;
  });
};
