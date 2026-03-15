package be.vdab.osrsplugin.groupinventory.service;

import be.vdab.osrsplugin.groupinventory.dto.InventoryUploadRequest;
import be.vdab.osrsplugin.groupinventory.dto.ItemQuantityRequest;
import be.vdab.osrsplugin.groupinventory.dto.TargetItemsRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupInventoryServiceTests {
    private final GroupInventoryService groupInventoryService = new GroupInventoryService();

    @Test
    void overviewAggregatesItemsAndTargetsAcrossMembers() {
        var createdGroup = groupInventoryService.createGroup("Team Alpha");

        groupInventoryService.updateMemberInventory(createdGroup.groupCode(), " Alice ", new InventoryUploadRequest(List.of(
                new ItemQuantityRequest("Saradomin brew(4)", 4),
                new ItemQuantityRequest("Saradomin brew(4)", 8),
                new ItemQuantityRequest("Bandos chestplate", 1)
        )));
        groupInventoryService.updateMemberInventory(createdGroup.groupCode(), "Bob", new InventoryUploadRequest(List.of(
                new ItemQuantityRequest("Saradomin brew(4)", 2),
                new ItemQuantityRequest("Abyssal whip", 1)
        )));
        groupInventoryService.updateTargetItems(createdGroup.groupCode(), new TargetItemsRequest(List.of(
                new ItemQuantityRequest("Saradomin brew(4)", 20),
                new ItemQuantityRequest("Abyssal whip", 2)
        )));

        var overview = groupInventoryService.getOverview(createdGroup.groupCode());

        assertThat(overview.memberCount()).isEqualTo(2);
        assertThat(overview.groupName()).isEqualTo("Team Alpha");
        assertThat(overview.groupKey()).isEqualTo(createdGroup.groupCode());
        assertThat(overview.members()).extracting(member -> member.memberName())
                .containsExactly("Alice", "Bob");

        var brewSummary = overview.itemSummaries().stream()
                .filter(item -> item.itemName().equals("Saradomin brew(4)"))
                .findFirst()
                .orElseThrow();
        assertThat(brewSummary.totalQuantity()).isEqualTo(14);
        assertThat(brewSummary.owners()).containsExactly("Alice", "Bob");
        assertThat(brewSummary.missingMembers()).isEmpty();

        var bandosSummary = overview.itemSummaries().stream()
                .filter(item -> item.itemName().equals("Bandos chestplate"))
                .findFirst()
                .orElseThrow();
        assertThat(bandosSummary.totalQuantity()).isEqualTo(1);
        assertThat(bandosSummary.owners()).containsExactly("Alice");
        assertThat(bandosSummary.missingMembers()).containsExactly("Bob");

        var brewTarget = overview.targetProgress().stream()
                .filter(target -> target.itemName().equals("Saradomin brew(4)"))
                .findFirst()
                .orElseThrow();
        assertThat(brewTarget.currentQuantity()).isEqualTo(14);
        assertThat(brewTarget.missingQuantity()).isEqualTo(6);
    }
}
