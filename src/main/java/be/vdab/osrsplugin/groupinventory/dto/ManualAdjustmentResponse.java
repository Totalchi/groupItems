package be.vdab.osrsplugin.groupinventory.dto;

public record ManualAdjustmentResponse(
        String itemName,
        int adjustmentQuantity
) {
}
