import { configureStore } from "@reduxjs/toolkit";
import { setupListeners } from "@reduxjs/toolkit/query";
import { persistStore, persistReducer } from "redux-persist";
import storage from "redux-persist/lib/storage";
import { combineReducers } from "@reduxjs/toolkit";

// API Slices
// Import API modules for side-effects (they inject endpoints into baseApi)
import "./api/authApi";
import "./api/rideApi";
import "./api/bookingApi";
import "./api/userApi";
import "./api/vehicleApi";
import "./api/driverApi";
import { baseApi } from "./api/baseApi";

// State Slices
import authSlice from "./slices/authSlice";
import uiSlice from "./slices/uiSlice";
import userSlice from "./slices/userSlice";
import rideSlice from "./slices/rideSlice";

const persistConfig = {
  key: "root",
  storage,
  whitelist: ["auth", "user"],
  blacklist: ["ui"],
};

const rootReducer = combineReducers({
  // API Reducer (shared baseApi used for all injected endpoints)
  [baseApi.reducerPath]: baseApi.reducer,

  // State Reducers
  auth: authSlice,
  ui: uiSlice,
  user: userSlice,
  ride: rideSlice,
});

const persistedReducer = persistReducer(persistConfig, rootReducer);

export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ["persist/PERSIST", "persist/REHYDRATE"],
      },
    }).concat(baseApi.middleware),
  devTools: Boolean(import.meta?.env?.DEV),
});

export const persistor = persistStore(store);
setupListeners(store.dispatch);
