package car_polling_project;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
public class customer {
    @Id
    @GeneratedValue(
      strategy = GenerationType.AUTO
    )
    private int id;
    private String userName;
    private String password;
    private String email;
    private String userType;
    private int age;
    private long phoneNumber;
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime registerTime;
    public customer() {

    }

    public customer(String userName, String password, String email,String userType, int age,int phoneNumber) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.age = age;
        this.userType=userType;
        this.phoneNumber=phoneNumber;
    }

    public String getUserName() {
        return userName;
    }

    public long getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(long phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public int getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
