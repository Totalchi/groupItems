package be.vdab.osrsplugin.groupinventory.dto;

import java.util.List;

public record TargetProgressResponse(
        String itemName,
        int targetQuantity,
        int currentQuantity,
        int missingQuantity,
        List<String> owners
) {
}
