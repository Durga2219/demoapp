package car_polling_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DriverController {
    @Autowired
    private DriverRepository driverRepository;

    // Endpoint to get all drivers
    @GetMapping("/drivers")
    public List<DriverDetails> getAllDrivers() {
        return driverRepository.findAll();
    }
}
