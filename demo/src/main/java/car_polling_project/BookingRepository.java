package car_polling_project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByPassengerId(int passengerId);
    List<Booking> findByRideId(int rideId);
    List<Booking> findByPassengerName(String passengerName);
}
