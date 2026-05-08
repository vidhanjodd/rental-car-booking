package com.rentalcar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String       accessToken;
    private String       refreshToken;
    private String       tokenType;
    private long         expiresIn;      // seconds
    private UUID         userId;
    private String       username;
    private String       email;
    private String       role;
}
