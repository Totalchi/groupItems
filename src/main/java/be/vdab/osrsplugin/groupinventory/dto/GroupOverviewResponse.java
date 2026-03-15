package be.vdab.osrsplugin.groupinventory.dto;

import java.time.Instant;
import java.util.List;

public record GroupOverviewResponse(
        String groupKey,
        String groupName,
        Instant generatedAt,
        int memberCount,
        List<MemberInventoryResponse> members,
        List<GroupItemSummaryResponse> itemSummaries,
        List<TargetProgressResponse> targetProgress,
        List<ManualAdjustmentResponse> manualAdjustments
) {
}
