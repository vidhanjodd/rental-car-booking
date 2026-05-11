package com.rentalcar.service;

import com.rentalcar.config.RedisConfig;
import com.rentalcar.dto.request.CarRequest;
import com.rentalcar.dto.request.CarSearchRequest;
import com.rentalcar.dto.response.CarResponse;
import com.rentalcar.dto.response.PageResponse;
import com.rentalcar.entity.Car;
import com.rentalcar.enums.CarStatus;
import com.rentalcar.exception.ResourceAlreadyExistsException;
import com.rentalcar.exception.ResourceNotFoundException;
import com.rentalcar.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final CarRepository carRepository;


    @Cacheable(
        value  = RedisConfig.CACHE_CAR_SEARCH,
        key    = "T(String).join('-', "
               + "  #req.city    ?: 'any',"
               + "  #req.category?.name() ?: 'any',"
               + "  #req.startDate?.toString() ?: 'any',"
               + "  #req.endDate?.toString()   ?: 'any',"
               + "  #req.minRate?.toString()   ?: '0',"
               + "  #req.maxRate?.toString()   ?: 'max',"
               + "  #page.toString(), #size.toString())",
        unless = "#result.content.isEmpty()"
    )
    @Transactional(readOnly = true)
    public PageResponse<CarResponse> searchCars(CarSearchRequest req, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dailyRate").ascending());

        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now().plusDays(1);
        LocalDate end   = req.getEndDate()   != null ? req.getEndDate()   : start.plusDays(1);

        var results = carRepository.findAvailableCars(
            req.getCity(), req.getCategory(), start, end,
            req.getMinRate(), req.getMaxRate(), pageable
        );

        log.debug("Car search [city={}, dates={}/{}] → {} results (page {}/{})",
            req.getCity(), start, end, results.getTotalElements(), page, results.getTotalPages());

        return PageResponse.from(results.map(this::toResponse));
    }


    @Cacheable(value = RedisConfig.CACHE_CAR_DETAILS, key = "#id")
    @Transactional(readOnly = true)
    public CarResponse getById(UUID id) {
        return toResponse(findCarOrThrow(id));
    }


    @Cacheable(value = RedisConfig.CACHE_CITIES)
    @Transactional(readOnly = true)
    public List<String> getAllCities() {
        return carRepository.findAllCities();
    }


    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH, allEntries = true)
    public CarResponse create(CarRequest req) {
        if (carRepository.existsByLicensePlate(req.getLicensePlate())) {
            throw new ResourceAlreadyExistsException(
                "Car with license plate '" + req.getLicensePlate() + "' already exists");
        }
        Car car = Car.builder()
            .brand(req.getBrand())
            .model(req.getModel())
            .year(req.getYear())
            .licensePlate(req.getLicensePlate().toUpperCase())
            .category(req.getCategory())
            .status(CarStatus.AVAILABLE)
            .dailyRate(req.getDailyRate())
            .city(req.getCity())
            .seats(req.getSeats())
            .transmission(req.getTransmission())
            .fuelType(req.getFuelType())
            .description(req.getDescription())
            .imageUrl(req.getImageUrl())
            .build();

        Car saved = carRepository.save(car);
        log.info("Car created: {} {} ({})", saved.getBrand(), saved.getModel(), saved.getId());
        return toResponse(saved);
    }


    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_CAR_DETAILS, key = "#id"),
        @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH,  allEntries = true)
    })
    public CarResponse update(UUID id, CarRequest req) {
        Car car = findCarOrThrow(id);

        if (!car.getLicensePlate().equalsIgnoreCase(req.getLicensePlate())
                && carRepository.existsByLicensePlate(req.getLicensePlate())) {
            throw new ResourceAlreadyExistsException(
                "License plate '" + req.getLicensePlate() + "' is already in use");
        }

        car.setBrand(req.getBrand());
        car.setModel(req.getModel());
        car.setYear(req.getYear());
        car.setLicensePlate(req.getLicensePlate().toUpperCase());
        car.setCategory(req.getCategory());
        car.setDailyRate(req.getDailyRate());
        car.setCity(req.getCity());
        car.setSeats(req.getSeats());
        car.setTransmission(req.getTransmission());
        car.setFuelType(req.getFuelType());
        car.setDescription(req.getDescription());
        car.setImageUrl(req.getImageUrl());

        return toResponse(carRepository.save(car));
    }


    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_CAR_DETAILS, key = "#id"),
        @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH,  allEntries = true)
    })
    public CarResponse updateStatus(UUID id, CarStatus newStatus) {
        Car car = findCarOrThrow(id);
        CarStatus old = car.getStatus();
        car.setStatus(newStatus);
        carRepository.save(car);
        log.info("Car {} status changed: {} → {}", id, old, newStatus);
        return toResponse(car);
    }


    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.CACHE_CAR_DETAILS, key = "#id"),
        @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH,  allEntries = true)
    })
    public void delete(UUID id) {
        Car car = findCarOrThrow(id);
        carRepository.delete(car);
        log.info("Car soft-deleted: {}", id);
    }


    private Car findCarOrThrow(UUID id) {
        return carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car", id));
    }

    public CarResponse toResponse(Car car) {
        return CarResponse.builder()
            .id(car.getId())
            .brand(car.getBrand())
            .model(car.getModel())
            .year(car.getYear())
            .licensePlate(car.getLicensePlate())
            .category(car.getCategory())
            .status(car.getStatus())
            .dailyRate(car.getDailyRate())
            .city(car.getCity())
            .seats(car.getSeats())
            .transmission(car.getTransmission())
            .fuelType(car.getFuelType())
            .description(car.getDescription())
            .imageUrl(car.getImageUrl())
            .createdAt(car.getCreatedAt())
            .build();
    }
}
