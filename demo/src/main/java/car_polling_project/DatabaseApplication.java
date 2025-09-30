package car_polling_project;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication

public class DatabaseApplication implements CommandLineRunner {
  @Autowired
  private customerRepository customerRepository;
  @Autowired
  private DriverRepository   DriverRepository;
  @Autowired
  private RegistrationController registrationController;
	public static void main(String[] args) {
		SpringApplication.run(DatabaseApplication.class, args);
	}
   @Override
    public void run(String... args) throws Exception{
//       BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        customer c1=new customer(
                "sai4455",
                "Darshan",
                "darshanbommineni@gmail.com",
                "driver",
                25,
                459038303
        );
//        customerRepository.save(c1);
//       registrationController.sendOtp(c1);
//      RegistrationController.registerUser(c1);

   }


}
