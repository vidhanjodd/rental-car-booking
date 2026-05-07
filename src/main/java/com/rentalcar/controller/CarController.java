package com.rentalcar.controller;

import com.rentalcar.dto.request.CarRequest;
import com.rentalcar.dto.request.CarSearchRequest;
import com.rentalcar.dto.response.CarResponse;
import com.rentalcar.dto.response.PageResponse;
import com.rentalcar.enums.CarStatus;
import com.rentalcar.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Car search and admin fleet management")
public class CarController {

    private final CarService carService;

    // ── Public endpoints (no auth) ─────────────────────────────────────────

    /**
     * GET /api/cars/search?city=Chennai&startDate=2024-12-01&endDate=2024-12-05
     * Results are Redis-cached — identical queries don't hit the DB.
     */
    @GetMapping("/search")
    @Operation(summary = "Search available cars — results are Redis-cached for 10 minutes")
    public ResponseEntity<PageResponse<CarResponse>> search(
            CarSearchRequest request,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(carService.searchCars(request, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car details by ID — cached for 30 minutes")
    public ResponseEntity<CarResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(carService.getById(id));
    }

    @GetMapping("/cities")
    @Operation(summary = "List all available cities — cached for 1 hour")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(carService.getAllCities());
    }

    // ── Admin endpoints ────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Add a new car to the fleet")
    public ResponseEntity<CarResponse> create(@Valid @RequestBody CarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Update car details — evicts car-details + car-search caches")
    public ResponseEntity<CarResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CarRequest request) {
        return ResponseEntity.ok(carService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Change car status (AVAILABLE / MAINTENANCE / RETIRED)")
    public ResponseEntity<CarResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam CarStatus status) {
        return ResponseEntity.ok(carService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Soft-delete a car from the fleet")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
