package com.ashu.ride_sharing.event;

import com.ashu.ride_sharing.models.Ride;
import com.ashu.ride_sharing.models.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RideBookedEvent {
    private final Ride ride;
    private final User passenger;
}
