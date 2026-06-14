package com.moviebooking.movieservice.dto;

import com.moviebooking.movieservice.entity.enums.CastRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CastRequest {
    @NotBlank(message = "Cast member name is required")
    private String name;

    @NotNull(message = "Role is required")
    private CastRole role;

    private String characterName; // Optional, for ACTOR role
}
