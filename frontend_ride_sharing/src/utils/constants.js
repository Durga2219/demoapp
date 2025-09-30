// src/utils/constants.js
export const API_ENDPOINTS = {
  AUTH: {
    REGISTER: "/auth/register",
    LOGIN: "/auth/login",
    REFRESH: "/auth/refresh-token",
    VERIFY_EMAIL: "/auth/verify-email",
  },
  USER: {
    PROFILE: "/user/profile",
    DASHBOARD: "/user/dashboard",
  },
  RIDES: {
    CREATE: "/rides",
    SEARCH: "/rides/search",
    MY_RIDES: "/rides/my-rides",
    BY_ID: (id) => `/rides/${id}`,
  },
  BOOKINGS: {
    CREATE: "/bookings",
    MY_BOOKINGS: "/bookings/my-bookings",
    DRIVER_BOOKINGS: "/bookings/driver-bookings",
    BY_ID: (id) => `/bookings/${id}`,
    CONFIRM: (id) => `/bookings/${id}/confirm`,
    CANCEL: (id) => `/bookings/${id}/cancel`,
  },
  VEHICLES: {
    CREATE: "/vehicles",
    MY_VEHICLES: "/vehicles/my-vehicles",
  },
  DRIVER: {
    VERIFY: "/driver/verify",
    STATUS: "/driver/verification-status",
  },
};

export const USER_ROLES = {
  DRIVER: "DRIVER",
  PASSENGER: "PASSENGER",
  ADMIN: "ADMIN",
};

export const BOOKING_STATUS = {
  PENDING: "PENDING",
  CONFIRMED: "CONFIRMED",
  CANCELLED: "CANCELLED",
  COMPLETED: "COMPLETED",
  NO_SHOW: "NO_SHOW",
  REFUNDED: "REFUNDED",
};

export const RIDE_STATUS = {
  ACTIVE: "ACTIVE",
  FULL: "FULL",
  CANCELLED: "CANCELLED",
  COMPLETED: "COMPLETED",
  IN_PROGRESS: "IN_PROGRESS",
  EXPIRED: "EXPIRED",
};

export const VEHICLE_TYPES = {
  SEDAN: "SEDAN",
  SUV: "SUV",
  HATCHBACK: "HATCHBACK",
  COUPE: "COUPE",
  CONVERTIBLE: "CONVERTIBLE",
  PICKUP_TRUCK: "PICKUP_TRUCK",
  MINIVAN: "MINIVAN",
  CROSSOVER: "CROSSOVER",
};
