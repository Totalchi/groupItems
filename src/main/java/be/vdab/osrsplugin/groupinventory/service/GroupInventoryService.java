package be.vdab.osrsplugin.groupinventory.service;

import be.vdab.osrsplugin.groupinventory.dto.GroupItemSummaryResponse;
import be.vdab.osrsplugin.groupinventory.dto.GroupOverviewResponse;
import be.vdab.osrsplugin.groupinventory.dto.InventoryUploadRequest;
import be.vdab.osrsplugin.groupinventory.dto.ItemQuantityRequest;
import be.vdab.osrsplugin.groupinventory.dto.ItemQuantityResponse;
import be.vdab.osrsplugin.groupinventory.dto.ManualAdjustmentRequest;
import be.vdab.osrsplugin.groupinventory.dto.ManualAdjustmentResponse;
import be.vdab.osrsplugin.groupinventory.dto.MemberInventoryResponse;
import be.vdab.osrsplugin.groupinventory.dto.CreateGroupResponse;
import be.vdab.osrsplugin.groupinventory.dto.TargetItemsRequest;
import be.vdab.osrsplugin.groupinventory.dto.TargetProgressResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class GroupInventoryService {
    private static final char[] CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    private final ConcurrentMap<String, GroupState> groups = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public CreateGroupResponse createGroup(String groupName) {
        var sanitizedGroupName = sanitizeGroupName(groupName);

        while (true) {
            var groupCode = generateGroupCode();
            var groupState = new GroupState(groupCode, sanitizedGroupName, Instant.now());
            if (groups.putIfAbsent(groupCode, groupState) == null) {
                return new CreateGroupResponse(groupCode, sanitizedGroupName, groupState.createdAt, "/groups/" + groupCode);
            }
        }
    }

    public GroupOverviewResponse updateMemberInventory(String groupCode, String memberName, InventoryUploadRequest request) {
        var groupState = findGroup(groupCode);
        synchronized (groupState) {
            var inventory = normalizeItems(request.items());
            var sanitizedMemberName = sanitizeMemberName(memberName);
            var memberKey = sanitizedMemberName.toLowerCase(Locale.ROOT);
            groupState.members.put(
                    memberKey,
                    new MemberInventory(sanitizedMemberName, Instant.now(), inventory)
            );
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse updateTargetItems(String groupCode, TargetItemsRequest request) {
        var groupState = findGroup(groupCode);
        synchronized (groupState) {
            groupState.targetItems = normalizeItems(request.items());
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse adjustItemQuantity(String groupCode, ManualAdjustmentRequest request) {
        var groupState = findGroup(groupCode);
        synchronized (groupState) {
            if (request.delta() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjustment delta cannot be zero");
            }

            var itemName = sanitizeItemName(request.itemName());
            var key = itemName.toLowerCase(Locale.ROOT);
            var existing = groupState.manualAdjustments.get(key);
            var nextQuantity = (existing == null ? 0 : existing.quantity()) + request.delta();

            if (nextQuantity == 0) {
                groupState.manualAdjustments.remove(key);
            } else {
                groupState.manualAdjustments.put(key, new NamedQuantity(itemName, nextQuantity));
            }

            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse getOverview(String groupCode) {
        var groupState = findGroup(groupCode);
        synchronized (groupState) {
            return buildOverview(groupState);
        }
    }

    private GroupOverviewResponse buildOverview(GroupState groupState) {
        var members = groupState.members.values().stream()
                .sorted(Comparator.comparing(MemberInventory::memberName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toMemberResponse)
                .toList();

        var memberNames = members.stream()
                .map(MemberInventoryResponse::memberName)
                .toList();

        var itemIndex = new LinkedHashMap<String, AggregatedItem>();
        for (var member : members) {
            for (var item : member.items()) {
                var aggregated = itemIndex.computeIfAbsent(
                        item.itemName().toLowerCase(Locale.ROOT),
                        ignored -> new AggregatedItem(item.itemName())
                );
                aggregated.loggedQuantity += item.quantity();
                aggregated.owners.add(member.memberName());
            }
        }

        for (var adjustment : groupState.manualAdjustments.values()) {
            var aggregated = itemIndex.computeIfAbsent(
                    adjustment.name().toLowerCase(Locale.ROOT),
                    ignored -> new AggregatedItem(adjustment.name())
            );
            aggregated.manualAdjustmentQuantity += adjustment.quantity();
        }

        var itemSummaries = itemIndex.values().stream()
                .filter(item -> item.loggedQuantity > 0 || item.manualAdjustmentQuantity > 0)
                .sorted(Comparator.comparing(AggregatedItem::displayName, String.CASE_INSENSITIVE_ORDER))
                .map(item -> new GroupItemSummaryResponse(
                        item.displayName,
                        item.loggedQuantity,
                        item.manualAdjustmentQuantity,
                        Math.max(item.loggedQuantity + item.manualAdjustmentQuantity, 0),
                        List.copyOf(item.owners),
                        memberNames.stream().filter(member -> !item.owners.contains(member)).toList()
                ))
                .toList();

        var targetProgress = groupState.targetItems.values().stream()
                .sorted(Comparator.comparing(NamedQuantity::name, String.CASE_INSENSITIVE_ORDER))
                .map(target -> {
                    var aggregated = itemIndex.get(target.name().toLowerCase(Locale.ROOT));
                    var currentQuantity = aggregated == null
                            ? 0
                            : Math.max(aggregated.loggedQuantity + aggregated.manualAdjustmentQuantity, 0);
                    var owners = aggregated == null ? List.<String>of() : List.copyOf(aggregated.owners);
                    return new TargetProgressResponse(
                            target.name(),
                            target.quantity(),
                            currentQuantity,
                            Math.max(target.quantity() - currentQuantity, 0),
                            owners
                    );
                })
                .toList();

        var manualAdjustments = groupState.manualAdjustments.values().stream()
                .sorted(Comparator.comparing(NamedQuantity::name, String.CASE_INSENSITIVE_ORDER))
                .map(adjustment -> new ManualAdjustmentResponse(adjustment.name(), adjustment.quantity()))
                .toList();

        return new GroupOverviewResponse(
                groupState.groupCode,
                groupState.groupName,
                Instant.now(),
                members.size(),
                members,
                itemSummaries,
                targetProgress,
                manualAdjustments
        );
    }

    private MemberInventoryResponse toMemberResponse(MemberInventory memberInventory) {
        var items = memberInventory.items().values().stream()
                .sorted(Comparator.comparing(NamedQuantity::name, String.CASE_INSENSITIVE_ORDER))
                .map(item -> new ItemQuantityResponse(item.name(), item.quantity()))
                .toList();
        var totalItemCount = items.stream().mapToInt(ItemQuantityResponse::quantity).sum();
        return new MemberInventoryResponse(
                memberInventory.memberName(),
                memberInventory.updatedAt(),
                items.size(),
                totalItemCount,
                items
        );
    }

    private LinkedHashMap<String, NamedQuantity> normalizeItems(Collection<ItemQuantityRequest> itemRequests) {
        var normalizedItems = new LinkedHashMap<String, NamedQuantity>();
        for (var itemRequest : itemRequests) {
            var itemName = sanitizeItemName(itemRequest.itemName());
            var key = itemName.toLowerCase(Locale.ROOT);
            var existing = normalizedItems.get(key);
            var quantity = existing == null ? itemRequest.quantity() : existing.quantity() + itemRequest.quantity();
            normalizedItems.put(key, new NamedQuantity(itemName, quantity));
        }
        return normalizedItems;
    }

    private GroupState findGroup(String groupCode) {
        var normalizedGroupCode = normalizeGroupCode(groupCode);
        var groupState = groups.get(normalizedGroupCode);
        if (groupState == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No group found for code " + normalizedGroupCode);
        }
        return groupState;
    }

    private String normalizeGroupCode(String groupCode) {
        var normalized = sanitizeAndCollapseWhitespace(groupCode).toLowerCase(Locale.ROOT);
        if (normalized.length() < 6 || normalized.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group code must be between 6 and 32 characters");
        }
        if (!normalized.matches("[a-z0-9-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group code may only contain letters, digits and hyphens");
        }
        return normalized;
    }

    private String sanitizeGroupName(String groupName) {
        var normalized = sanitizeAndCollapseWhitespace(groupName);
        if (normalized.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name cannot be longer than 64 characters");
        }
        return normalized;
    }

    private String sanitizeMemberName(String memberName) {
        var normalized = sanitizeAndCollapseWhitespace(memberName);
        if (normalized.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member name cannot be longer than 32 characters");
        }
        return normalized;
    }

    private String sanitizeItemName(String itemName) {
        var normalized = sanitizeAndCollapseWhitespace(itemName);
        if (normalized.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item name cannot be longer than 80 characters");
        }
        return normalized;
    }

    private String sanitizeAndCollapseWhitespace(String value) {
        var normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value cannot be blank");
        }
        return normalized;
    }

    private String generateGroupCode() {
        return "grp-" + randomSegment(4) + "-" + randomSegment(4);
    }

    private String randomSegment(int length) {
        var builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private static final class GroupState {
        private final String groupCode;
        private final String groupName;
        private final Instant createdAt;
        private final Map<String, MemberInventory> members = new LinkedHashMap<>();
        private Map<String, NamedQuantity> targetItems = Map.of();
        private final Map<String, NamedQuantity> manualAdjustments = new LinkedHashMap<>();

        private GroupState(String groupCode, String groupName, Instant createdAt) {
            this.groupCode = groupCode;
            this.groupName = groupName;
            this.createdAt = createdAt;
        }
    }

    private record MemberInventory(String memberName, Instant updatedAt, Map<String, NamedQuantity> items) {
    }

    private record NamedQuantity(String name, int quantity) {
    }

    private static final class AggregatedItem {
        private final String displayName;
        private final LinkedHashSet<String> owners = new LinkedHashSet<>();
        private int loggedQuantity;
        private int manualAdjustmentQuantity;

        private AggregatedItem(String displayName) {
            this.displayName = displayName;
        }

        private String displayName() {
            return displayName;
        }
    }
}
