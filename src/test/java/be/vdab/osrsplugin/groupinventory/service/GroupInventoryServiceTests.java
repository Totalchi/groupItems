package be.vdab.osrsplugin.groupinventory.service;

import be.vdab.osrsplugin.groupinventory.dto.InventoryUploadRequest;
import be.vdab.osrsplugin.groupinventory.dto.ItemQuantityRequest;
import be.vdab.osrsplugin.groupinventory.dto.ManualAdjustmentRequest;
import be.vdab.osrsplugin.groupinventory.dto.TargetItemsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupInventoryServiceTests {
    @TempDir
    Path tempDir;

    private GroupInventoryService groupInventoryService;

    @BeforeEach
    void setUp() {
        groupInventoryService = new GroupInventoryService(
                new GroupInventoryPersistence(
                        tempDir.resolve("group-state.json").toString(),
                        new ObjectMapper()
                )
        );
    }

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
        assertThat(brewSummary.loggedQuantity()).isEqualTo(14);
        assertThat(brewSummary.manualAdjustmentQuantity()).isZero();
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
        assertThat(overview.manualAdjustments()).isEmpty();
    }

    @Test
    void manualAdjustmentsChangeGroupTotalsWithoutChangingOwners() {
        var createdGroup = groupInventoryService.createGroup("Team Beta");

        groupInventoryService.updateMemberInventory(createdGroup.groupCode(), "Alice", new InventoryUploadRequest(List.of(
                new ItemQuantityRequest("Bandos tassets", 4)
        )));
        groupInventoryService.updateTargetItems(createdGroup.groupCode(), new TargetItemsRequest(List.of(
                new ItemQuantityRequest("Bandos tassets", 5)
        )));

        groupInventoryService.adjustItemQuantity(createdGroup.groupCode(),
                new be.vdab.osrsplugin.groupinventory.dto.ManualAdjustmentRequest("Bandos tassets", -1));

        var adjustedOverview = groupInventoryService.getOverview(createdGroup.groupCode());
        var tassetsSummary = adjustedOverview.itemSummaries().stream()
                .filter(item -> item.itemName().equals("Bandos tassets"))
                .findFirst()
                .orElseThrow();
        assertThat(tassetsSummary.loggedQuantity()).isEqualTo(4);
        assertThat(tassetsSummary.manualAdjustmentQuantity()).isEqualTo(-1);
        assertThat(tassetsSummary.totalQuantity()).isEqualTo(3);
        assertThat(tassetsSummary.owners()).containsExactly("Alice");

        var tassetsTarget = adjustedOverview.targetProgress().stream()
                .filter(target -> target.itemName().equals("Bandos tassets"))
                .findFirst()
                .orElseThrow();
        assertThat(tassetsTarget.currentQuantity()).isEqualTo(3);
        assertThat(tassetsTarget.missingQuantity()).isEqualTo(2);
        assertThat(adjustedOverview.manualAdjustments()).extracting(adjustment -> adjustment.itemName())
                .containsExactly("Bandos tassets");

        groupInventoryService.adjustItemQuantity(createdGroup.groupCode(),
                new be.vdab.osrsplugin.groupinventory.dto.ManualAdjustmentRequest("Bandos tassets", 1));

        var resetOverview = groupInventoryService.getOverview(createdGroup.groupCode());
        var resetSummary = resetOverview.itemSummaries().stream()
                .filter(item -> item.itemName().equals("Bandos tassets"))
                .findFirst()
                .orElseThrow();
        assertThat(resetSummary.manualAdjustmentQuantity()).isZero();
        assertThat(resetSummary.totalQuantity()).isEqualTo(4);
        assertThat(resetOverview.manualAdjustments()).isEmpty();
    }

    @Test
    void stateSurvivesServiceRestart() {
        var createdGroup = groupInventoryService.createGroup("Team Gamma");
        groupInventoryService.updateMemberInventory(createdGroup.groupCode(), "Alice", new InventoryUploadRequest(List.of(
                new ItemQuantityRequest("Bandos tassets", 2)
        )));
        groupInventoryService.adjustItemQuantity(createdGroup.groupCode(), new ManualAdjustmentRequest("Bandos tassets", -1));

        var reloadedService = new GroupInventoryService(
                new GroupInventoryPersistence(
                        tempDir.resolve("group-state.json").toString(),
                        new ObjectMapper()
                )
        );

        var overview = reloadedService.getOverview(createdGroup.groupCode());
        assertThat(overview.groupName()).isEqualTo("Team Gamma");
        assertThat(overview.members()).extracting(member -> member.memberName()).containsExactly("Alice");
        assertThat(overview.manualAdjustments()).extracting(adjustment -> adjustment.adjustmentQuantity())
                .containsExactly(-1);
        assertThat(overview.itemSummaries()).extracting(item -> item.totalQuantity()).containsExactly(1);
    }

    @Test
    void createMemberAddsEmptyMemberToGroup() {
        var createdGroup = groupInventoryService.createGroup("Team Epsilon");
        groupInventoryService.createMember(createdGroup.groupCode(), "Alice");

        var overview = groupInventoryService.getOverview(createdGroup.groupCode());
        assertThat(overview.memberCount()).isEqualTo(1);
        assertThat(overview.members()).extracting(m -> m.memberName()).containsExactly("Alice");
        assertThat(overview.members().get(0).items()).isEmpty();
    }

    @Test
    void createMemberIsIdempotentForExistingMember() {
        var createdGroup = groupInventoryService.createGroup("Team Zeta");
        groupInventoryService.updateMemberInventory(createdGroup.groupCode(), "Alice", new InventoryUploadRequest(List.of(
                new ItemQuantityRequest("Bandos chestplate", 1)
        )));
        groupInventoryService.createMember(createdGroup.groupCode(), "Alice");

        var overview = groupInventoryService.getOverview(createdGroup.groupCode());
        assertThat(overview.memberCount()).isEqualTo(1);
        assertThat(overview.members().get(0).items()).hasSize(1);
    }

    @Test
    void addMemberItemCreatesNewMemberWhenNotExists() {
        var createdGroup = groupInventoryService.createGroup("Team Eta");
        groupInventoryService.addMemberItem(createdGroup.groupCode(), "Alice", "Bandos tassets", 1);

        var overview = groupInventoryService.getOverview(createdGroup.groupCode());
        assertThat(overview.memberCount()).isEqualTo(1);
        assertThat(overview.members().get(0).memberName()).isEqualTo("Alice");
        var tassets = overview.members().get(0).items().get(0);
        assertThat(tassets.itemName()).isEqualTo("Bandos tassets");
        assertThat(tassets.quantity()).isEqualTo(1);
    }

    @Test
    void addMemberItemIncrementsExistingItem() {
        var createdGroup = groupInventoryService.createGroup("Team Theta");
        groupInventoryService.updateMemberInventory(createdGroup.groupCode(), "Alice", new InventoryUploadRequest(List.of(
                new ItemQuantityRequest("Bandos tassets", 3)
        )));
        groupInventoryService.addMemberItem(createdGroup.groupCode(), "Alice", "Bandos tassets", 2);

        var tassets = groupInventoryService.getOverview(createdGroup.groupCode())
                .members().get(0).items().get(0);
        assertThat(tassets.quantity()).isEqualTo(5);
    }

    @Test
    void addMemberItemWithNegativeQuantityRemovesItemWhenReachingZero() {
        var createdGroup = groupInventoryService.createGroup("Team Iota");
        groupInventoryService.addMemberItem(createdGroup.groupCode(), "Alice", "Bandos tassets", 2);
        groupInventoryService.addMemberItem(createdGroup.groupCode(), "Alice", "Bandos tassets", -2);

        var overview = groupInventoryService.getOverview(createdGroup.groupCode());
        assertThat(overview.members().get(0).items()).isEmpty();
    }

    @Test
    void addMemberItemRejectsZeroQuantity() {
        var createdGroup = groupInventoryService.createGroup("Team Kappa");
        assertThatThrownBy(() -> groupInventoryService.addMemberItem(
                createdGroup.groupCode(), "Alice", "Bandos tassets", 0
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void adjustmentsRejectIntegerOverflow() {
        var createdGroup = groupInventoryService.createGroup("Team Delta");

        groupInventoryService.adjustItemQuantity(createdGroup.groupCode(), new ManualAdjustmentRequest("Bandos tassets", Integer.MAX_VALUE));

        assertThatThrownBy(() -> groupInventoryService.adjustItemQuantity(
                createdGroup.groupCode(),
                new ManualAdjustmentRequest("Bandos tassets", 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Adjustment quantity is too large");
    }
}
