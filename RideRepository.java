package car_polling_project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Integer> {
    List<Ride> findBySourceAndDestinationAndRideDateTimeBetween(
            String source, String destination, LocalDateTime start, LocalDateTime end
    );
    List<Ride>findByDriverName(String name);
}
