package car_polling_project;

import jakarta.persistence.*;

@Entity
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private int passengerId;
    private String passengerName;
    private String status;
    // e.g. CONFIRMED, CANCELLED

//    private Passenger passenger;
    @ManyToOne
    @JoinColumn(name = "ride_id")
    private Ride ride;

    public Booking() {}

    // Getters & setters
    public int getId() { return id; }

    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }


}
