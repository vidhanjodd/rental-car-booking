package com.rentalcar.dto.request;

import com.rentalcar.enums.CarCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CarRequest {

    @NotBlank(message = "Brand is required")
    @Size(max = 50)
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50)
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    private Integer year;

    @NotBlank(message = "License plate is required")
    @Size(max = 20)
    private String licensePlate;

    @NotNull(message = "Category is required")
    private CarCategory category;

    @NotNull(message = "Daily rate is required")
    @DecimalMin(value = "0.01", message = "Daily rate must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal dailyRate;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotNull(message = "Seats count is required")
    @Min(2) @Max(15)
    private Integer seats;

    @Pattern(regexp = "^(AUTOMATIC|MANUAL)$", message = "Transmission must be AUTOMATIC or MANUAL")
    private String transmission;

    @Pattern(regexp = "^(PETROL|DIESEL|ELECTRIC|HYBRID)$", message = "Invalid fuel type")
    private String fuelType;

    private String description;
    private String imageUrl;
}
