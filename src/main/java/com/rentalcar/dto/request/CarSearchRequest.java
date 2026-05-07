package com.rentalcar.dto.request;

import com.rentalcar.enums.CarCategory;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CarSearchRequest {

    private String city;

    private CarCategory category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private BigDecimal minRate;
    private BigDecimal maxRate;
}
