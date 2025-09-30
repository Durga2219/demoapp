package com.ashu.ride_sharing.dto.response;

import java.util.List;
import java.util.UUID;

import com.ashu.ride_sharing.models.enums.VehicleStatus;
import com.ashu.ride_sharing.models.enums.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleResponse {
    private UUID vehicleId;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private String licensePlate;
    private Integer capacity;
    private VehicleType type;
    private VehicleStatus status;
    private String vehicleInfo;
    private List<String> images;
}
