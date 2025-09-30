package car_polling_project;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String name;
    private String email;
    private Long phoneNumber;
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "ride_id")
    @JsonBackReference
    private Ride ride;  // Links passenger to a specific ride

    public Passenger() {}

    public Passenger(String name, String email, long phoneNumber, Ride ride, boolean isActive) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.ride = ride;
        this.isActive = isActive;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(long phoneNumber) { this.phoneNumber = phoneNumber; }

    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
