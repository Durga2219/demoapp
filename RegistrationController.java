package car_polling_project;

//import org.apache.commons.collections.map.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.*;
import java.util.Random;

@RestController
@RequestMapping("/register")
public class RegistrationController {
    @Autowired
    private customerRepository customerRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private EmailService emailService;
    private Map<String,String> otpStorage=new HashMap<>();
    private BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
    public static void registerUser(customer c) {
        // Your static logic for OTP, email, age, etc.
        System.out.println("Registering user: " + c.getUserName());

        // For static testing, you can skip actual repository save
    }
    @PostMapping("/send-otp")
    public String sendOtp(@RequestBody customer user){
        if(customerRepository.findByUserName(user.getUserName()).isPresent()){
            return "User name already exists";
        }
        if(customerRepository.findByEmail(user.getEmail()).isPresent()){
            return "Email already Exists";
        }
        String otp=String.format("%06d",new Random().nextInt(99999));
        otpStorage.put(user.getEmail(),otp);
        emailService.sendEmail(user.getEmail(),"Your OTP for Registration ","OTP:"+otp);
        return "OTP sent to email";
    }
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody OtpRequest otpRequest){
        String storedOtp=otpStorage.get(otpRequest.getEmail());
        if(storedOtp==null)
            return "No otp sent to this email";
        if(!storedOtp.equals(otpRequest.getOtp()))
              return "Invalid OTP";

        customer user=otpRequest.getUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        customerRepository.save(user);
        return "Registration Successfully";
    }
}
