import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  searchResults: [],
  searchFilters: {
    source: "",
    destination: "",
    date: "",
    seatsRequired: 1,
  },
  currentRide: null,
  myRides: [],
  recentSearches: [],
  isLoading: false,
  error: null,
};

const rideSlice = createSlice({
  name: "ride",
  initialState,
  reducers: {
    setSearchResults: (state, action) => {
      state.searchResults = action.payload;
    },

    updateSearchFilters: (state, action) => {
      state.searchFilters = { ...state.searchFilters, ...action.payload };
    },

    setCurrentRide: (state, action) => {
      state.currentRide = action.payload;
    },

    addRecentSearch: (state, action) => {
      const search = { ...action.payload, timestamp: Date.now() };
      state.recentSearches = [search, ...state.recentSearches.slice(0, 9)];
    },

    clearSearchResults: (state) => {
      state.searchResults = [];
    },

    setRideLoading: (state, action) => {
      state.isLoading = action.payload;
    },

    setRideError: (state, action) => {
      state.error = action.payload;
    },
  },
});

export const {
  setSearchResults,
  updateSearchFilters,
  setCurrentRide,
  addRecentSearch,
  clearSearchResults,
  setRideLoading,
  setRideError,
} = rideSlice.actions;

// Selectors
export const selectSearchResults = (state) => state.ride.searchResults;
export const selectSearchFilters = (state) => state.ride.searchFilters;
export const selectCurrentRide = (state) => state.ride.currentRide;
export const selectRecentSearches = (state) => state.ride.recentSearches;

export default rideSlice.reducer;
