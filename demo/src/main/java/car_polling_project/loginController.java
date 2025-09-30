package car_polling_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
@RestController
@RequestMapping("/login")
public class loginController {
    @Autowired
    private customerRepository customerRepository;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @PostMapping
    public Object login(@RequestBody loginRequest request) {
        Optional<customer> optionalUser = customerRepository.findByUserName(request.getUserName());

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        customer user = optionalUser.get();


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return "Invalid password";
        }


        String token = JWTUtil.generateToken(user.getUserName(), user.getUserType());

        return  new LoginResponse(token);
    }
}
