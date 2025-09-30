package car_polling_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/rides")
public class RideController {
    @Autowired
    private customerRepository customerRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private DriverRepository driverRepository;
    @PostMapping("/post")
    public String postRide(@RequestBody Ride ride) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

    // Fetch user from customer table
    customer user = customerRepository.findByUserName(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (!"DRIVER".equalsIgnoreCase(user.getUserType())) {
        return "Access denied: only drivers can add rides";
    }

        rideRepository.save(ride);
        return "Ride posted successfully!";
    }
   @GetMapping("/all")
    public List<Ride> getAllRides() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        customer user = customerRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"USER".equalsIgnoreCase(user.getUserType())) {
            throw new AccessDeniedException("Access denied: only passengers can search rides");
        }
        return rideRepository.findAll();
    }

    @GetMapping("/search")
    public List<Ride> searchRides(@RequestParam String source,
                                  @RequestParam String destination,
                                  @RequestParam String date) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        customer user = customerRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"USER".equalsIgnoreCase(user.getUserType())) {
            throw new AccessDeniedException("Access denied: only passengers can search rides");
        }

        date = date.trim();
        LocalDateTime start = LocalDateTime.parse(date + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(date + "T23:59:59");
        return rideRepository.findBySourceAndDestinationAndRideDateTimeBetween(source, destination, start, end);
    }
}

