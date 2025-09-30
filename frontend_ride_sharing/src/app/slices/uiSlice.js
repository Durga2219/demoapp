import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  isGlobalLoading: false,
  notifications: [],
  modals: {
    rideDetails: { isOpen: false, data: null },
    bookingConfirmation: { isOpen: false, data: null },
  },
  theme: "light",
};

const uiSlice = createSlice({
  name: "ui",
  initialState,
  reducers: {
    setGlobalLoading: (state, action) => {
      state.isGlobalLoading = action.payload;
    },

    addNotification: (state, action) => {
      state.notifications.push({
        id: Date.now(),
        ...action.payload,
      });
    },

    removeNotification: (state, action) => {
      state.notifications = state.notifications.filter(
        (n) => n.id !== action.payload
      );
    },

    openModal: (state, action) => {
      const { modalName, data } = action.payload;
      state.modals[modalName] = { isOpen: true, data };
    },

    closeModal: (state, action) => {
      const modalName = action.payload;
      state.modals[modalName] = { isOpen: false, data: null };
    },

    setTheme: (state, action) => {
      state.theme = action.payload;
    },
  },
});

export const {
  setGlobalLoading,
  addNotification,
  removeNotification,
  openModal,
  closeModal,
  setTheme,
} = uiSlice.actions;

export default uiSlice.reducer;
