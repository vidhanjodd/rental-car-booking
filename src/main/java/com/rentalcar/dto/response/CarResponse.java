package com.rentalcar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rentalcar.enums.CarCategory;
import com.rentalcar.enums.CarStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarResponse {
    private UUID        id;
    private String      brand;
    private String      model;
    private Integer     year;
    private String      licensePlate;
    private CarCategory category;
    private CarStatus   status;
    private BigDecimal  dailyRate;
    private String      city;
    private Integer     seats;
    private String      transmission;
    private String      fuelType;
    private String      description;
    private String      imageUrl;
    private Instant     createdAt;
}
