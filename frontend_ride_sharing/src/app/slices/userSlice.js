import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  profile: null,
  dashboard: null,
  preferences: {
    notifications: true,
    theme: "light",
    language: "en",
  },
  isLoading: false,
  error: null,
};

const userSlice = createSlice({
  name: "user",
  initialState,
  reducers: {
    setProfile: (state, action) => {
      state.profile = action.payload;
    },

    updateProfile: (state, action) => {
      if (state.profile) {
        state.profile = { ...state.profile, ...action.payload };
      }
    },

    setDashboard: (state, action) => {
      state.dashboard = action.payload;
    },

    updatePreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
    },

    setUserLoading: (state, action) => {
      state.isLoading = action.payload;
    },

    setUserError: (state, action) => {
      state.error = action.payload;
    },
  },
});

export const {
  setProfile,
  updateProfile,
  setDashboard,
  updatePreferences,
  setUserLoading,
  setUserError,
} = userSlice.actions;

export default userSlice.reducer;
