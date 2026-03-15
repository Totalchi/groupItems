package be.vdab.osrsplugin.groupinventory.dto;

import java.time.Instant;

public record CreateGroupResponse(
        String groupCode,
        String groupName,
        Instant createdAt,
        String overviewPath
) {
}
