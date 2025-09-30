package car_polling_project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<DriverDetails,Integer> {
    Optional<DriverDetails> findByName(String name);
}
