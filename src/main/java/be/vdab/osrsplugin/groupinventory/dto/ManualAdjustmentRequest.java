package be.vdab.osrsplugin.groupinventory.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualAdjustmentRequest(
        @NotBlank String itemName,
        int delta
) {
}
