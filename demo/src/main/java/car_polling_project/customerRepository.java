package car_polling_project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface customerRepository extends JpaRepository<customer,Integer> {
    Optional<customer>findByUserName(String userName);
    Optional<customer>findByEmail(String email);
}
