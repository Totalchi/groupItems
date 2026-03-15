package be.vdab.osrsplugin.groupinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ItemQuantityRequest(
        @NotBlank String itemName,
        @Positive int quantity
) {
}
