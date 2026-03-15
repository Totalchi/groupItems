package be.vdab.osrsplugin.groupinventory.dto;

import java.util.List;

public record GroupItemSummaryResponse(
        String itemName,
        int totalQuantity,
        List<String> owners,
        List<String> missingMembers
) {
}
