package car_polling_project;

public class OtpRequest {
    private String email;
    private String otp;
    private customer user;

    public String getEmail() {
        return email;
    }

    public OtpRequest() {
    }

    public String getOtp() {
        return otp;
    }

    public customer getUser() {
        return user;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public void setUser(customer user) {
        this.user = user;
    }
}
