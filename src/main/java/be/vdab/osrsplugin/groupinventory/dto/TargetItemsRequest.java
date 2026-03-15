package be.vdab.osrsplugin.groupinventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TargetItemsRequest(
        @NotNull List<@Valid ItemQuantityRequest> items
) {
}
