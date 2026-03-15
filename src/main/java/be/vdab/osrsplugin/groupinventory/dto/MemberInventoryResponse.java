package be.vdab.osrsplugin.groupinventory.dto;

import java.time.Instant;
import java.util.List;

public record MemberInventoryResponse(
        String memberName,
        Instant updatedAt,
        int distinctItemCount,
        int totalItemCount,
        List<ItemQuantityResponse> items
) {
}
