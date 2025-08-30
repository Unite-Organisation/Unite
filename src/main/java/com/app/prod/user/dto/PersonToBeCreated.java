package com.app.prod.user.dto;

import java.util.UUID;

public record PersonToBeCreated(
        String firstName,
        String lastName,
        UUID buildingId
){
}