import { baseApi } from "./baseApi";

export const driverApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    verifyDriver: builder.mutation({
      query: (verificationData) => ({
        url: "/driver/verify",
        method: "POST",
        body: verificationData,
        // backend returns plain text; ask fetchBaseQuery to return raw text
        responseHandler: 'text',
      }),
      // Backend returns plain text; coerce to JSON-like result to avoid PARSING_ERROR
      transformResponse: (response) => {
        // fetchBaseQuery will return a string when responseHandler is 'text'
        if (typeof response === "string") {
          return { message: response };
        }
        return response;
      },
      invalidatesTags: ["Driver", "User"],
    }),

    getDriverStatus: builder.query({
      query: () => "/driver/verification-status",
      providesTags: ["Driver"],
    }),
  }),
});

export const { useVerifyDriverMutation, useGetDriverStatusQuery, useLazyGetDriverStatusQuery } = driverApi;
