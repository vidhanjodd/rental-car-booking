package com.rentalcar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rentalcar.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private UUID    id;
    private String  username;
    private String  email;
    private String  firstName;
    private String  lastName;
    private String  phone;
    private Role    role;
    private boolean enabled;
    private Instant createdAt;
}
