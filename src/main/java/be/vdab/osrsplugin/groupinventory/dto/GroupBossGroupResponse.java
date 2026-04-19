package be.vdab.osrsplugin.groupinventory.dto;

import java.util.List;

public record GroupBossGroupResponse(
        String bossName,
        List<GroupItemSummaryResponse> items
) {
}
