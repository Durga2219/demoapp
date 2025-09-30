package car_polling_project;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
public class customerController {
    @Autowired
    private customerRepository customerRepository;
    @GetMapping("/customers")
    public List<customer> getAllCustomers(){
        System.out.println("Fetching");
        return customerRepository.findAll();
    }
    @GetMapping("/profile/{userName}")
    public Object getProfile(@PathVariable String userName) {
        Optional<customer> optionalUser = customerRepository.findByUserName(userName);

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        customer user = optionalUser.get();
        // Only return public info, not password
        Map<String, Object> profile = new HashMap<>();
        profile.put("userName", user.getUserName());
        profile.put("email", user.getEmail());
        profile.put("userType", user.getUserType());
        profile.put("age", user.getAge());
        profile.put("phoneNumber", user.getPhoneNumber());

        return profile;
    }
}
