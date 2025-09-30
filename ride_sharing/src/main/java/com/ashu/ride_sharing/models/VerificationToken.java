package com.ashu.ride_sharing.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;


@Data
@NoArgsConstructor
@Entity
public class VerificationToken {

    private static final int EXPIRATION_MINUTES = 60*24; //24 HOURS

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public VerificationToken(String token, User user){
        this.token=token;
        this.user=user;
        this.expiryDate=calculateExpiryDate();
    }

    private LocalDateTime calculateExpiryDate(){
        return LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
    }

    public boolean isExpired(){
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
}
