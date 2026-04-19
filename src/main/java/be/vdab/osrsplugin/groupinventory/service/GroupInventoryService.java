package be.vdab.osrsplugin.groupinventory.service;

import be.vdab.osrsplugin.groupinventory.dto.GroupBossGroupResponse;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GroupInventoryService {
    private static final char[] CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    /**
     * Maps normalized item name → boss name for web UI grouping.
     * Kept in sync with the plugin's BossUniqueCatalog.
     */
    private static final Map<String, String> BOSS_BY_ITEM = buildBossByItemMap();

    private static Map<String, String> buildBossByItemMap() {
        var map = new LinkedHashMap<String, String>();
        // GWD
        for (var item : List.of("Bandos chestplate","Bandos tassets","Bandos boots","Bandos hilt"))
            map.put(normalize(item), "General Graardor");
        for (var item : List.of("Armadyl helmet","Armadyl chestplate","Armadyl chainskirt","Armadyl hilt"))
            map.put(normalize(item), "Kree'arra");
        for (var item : List.of("Armadyl crossbow","Saradomin sword","Saradomin's light","Saradomin hilt"))
            map.put(normalize(item), "Commander Zilyana");
        for (var item : List.of("Zamorakian spear","Staff of the dead","Steam battlestaff","Zamorak hilt"))
            map.put(normalize(item), "K'ril Tsutsaroth");
        // Nex
        for (var item : List.of("Torva full helm","Torva platebody","Torva platelegs","Nihil horn","Zaryte vambraces","Ancient hilt"))
            map.put(normalize(item), "Nex");
        // Corp
        for (var item : List.of("Spectral sigil","Arcane sigil","Elysian sigil","Holy elixir"))
            map.put(normalize(item), "Corporeal Beast");
        // Zulrah / Vorkath
        for (var item : List.of("Tanzanite fang","Magic fang","Serpentine visage","Uncut onyx"))
            map.put(normalize(item), "Zulrah");
        for (var item : List.of("Skeletal visage","Dragonbone necklace"))
            map.put(normalize(item), "Vorkath");
        // Alch Hydra
        for (var item : List.of("Hydra's claw","Hydra leather","Hydra tail","Dragon knife","Dragon thrownaxe"))
            map.put(normalize(item), "Alchemical Hydra");
        // Demonic gorillas
        map.put(normalize("Zenyte shard"), "Demonic gorillas");
        // Cerberus
        for (var item : List.of("Primordial crystal","Pegasian crystal","Eternal crystal","Smouldering stone"))
            map.put(normalize(item), "Cerberus");
        // Abyssal Sire
        for (var item : List.of("Abyssal dagger","Bludgeon spine","Bludgeon claw","Bludgeon axon","Unsired"))
            map.put(normalize(item), "Abyssal Sire");
        // Grotesque Guardians
        for (var item : List.of("Granite ring","Granite hammer","Black tourmaline core"))
            map.put(normalize(item), "Grotesque Guardians");
        // Phantom Muspah
        for (var item : List.of("Venator shard","Ancient icon","Saturated heart"))
            map.put(normalize(item), "Phantom Muspah");
        // The Gauntlet
        for (var item : List.of("Enhanced crystal weapon seed","Armour seed","Weapon seed"))
            map.put(normalize(item), "The Gauntlet");
        // The Nightmare
        for (var item : List.of("Inquisitor's great helm","Inquisitor's hauberk","Inquisitor's plateskirt","Nightmare staff","Harmonised orb","Volatile orb","Eldritch orb"))
            map.put(normalize(item), "The Nightmare");
        // Dagannoth Kings
        for (var item : List.of("Berserker ring","Seers ring","Warrior ring","Archers ring","Dragon axe","Mud battlestaff","Seercull"))
            map.put(normalize(item), "Dagannoth Kings");
        // Raids 1
        for (var item : List.of("Twisted bow","Elder maul","Kodai insignia","Dragon hunter crossbow","Twisted buckler","Dinh's bulwark","Ancestral hat","Ancestral robe top","Ancestral robe bottom","Dragon claws","Dexterous prayer scroll","Arcane prayer scroll"))
            map.put(normalize(item), "Chambers of Xeric");
        // Raids 2
        for (var item : List.of("Avernic defender hilt","Ghrazi rapier","Scythe of vitur","Sanguinesti staff","Justiciar faceguard","Justiciar chestguard","Justiciar legguards","Lil' Zik"))
            map.put(normalize(item), "Theatre of Blood");
        // Raids 3
        for (var item : List.of("Tumeken's shadow","Elidinis' ward","Masori mask","Masori body","Masori chaps","Osmumten's fang","Lightbearer","Thread of elidinis","Breach of the scarab"))
            map.put(normalize(item), "Tombs of Amascut");
        // DT2
        for (var item : List.of("Magus ring","Virtus mask","Virtus robe top","Virtus robe bottom"))
            map.put(normalize(item), "Duke Sucellus");
        for (var item : List.of("Venator ring","Leviathan's lure"))
            map.put(normalize(item), "The Leviathan");
        for (var item : List.of("Bellator ring","Siren's staff"))
            map.put(normalize(item), "The Whisperer");
        for (var item : List.of("Ultor ring","Executioner's axe head","Chromium ingot"))
            map.put(normalize(item), "Vardorvis");
        // Barrows
        for (var item : List.of("Ahrim's hood","Ahrim's staff","Ahrim's robetop","Ahrim's robe skirt","Dharok's helm","Dharok's platebody","Dharok's platelegs","Dharok's greataxe","Guthan's helm","Guthan's platebody","Guthan's chainskirt","Guthan's warspear","Karil's coif","Karil's leathertop","Karil's leatherskirt","Karil's crossbow","Torag's helm","Torag's platebody","Torag's platelegs","Torag's hammers","Verac's helm","Verac's brassard","Verac's plateskirt","Verac's flail"))
            map.put(normalize(item), "Barrows Chests");
        // Other bosses
        for (var item : List.of("Sarachnis cudgel","Jar of eyes","Giant egg sac"))
            map.put(normalize(item), "Sarachnis");
        for (var item : List.of("Dragon chainbody","KQ head","Jar of sand","Kalphite princess"))
            map.put(normalize(item), "Kalphite Queen");
        for (var item : List.of("Draconic visage","KBD heads","Prince black dragon"))
            map.put(normalize(item), "King Black Dragon");
        for (var item : List.of("Mole skin","Mole claw"))
            map.put(normalize(item), "Giant Mole");
        for (var item : List.of("Skotos","Dark claw","Jar of darkness"))
            map.put(normalize(item), "Skotizo");
        for (var item : List.of("Dragon pickaxe","Pet chaos elemental"))
            map.put(normalize(item), "Chaos Elemental");
        for (var item : List.of("Kraken tentacle","Jar of dirt","Pet kraken"))
            map.put(normalize(item), "Kraken");
        for (var item : List.of("Odium shard 3","Malediction shard 3","Scorpia's offspring"))
            map.put(normalize(item), "Scorpia");
        for (var item : List.of("Tyrannical ring","Callisto cub"))
            map.put(normalize(item), "Callisto");
        for (var item : List.of("Treasonous ring","Venenatis spiderling"))
            map.put(normalize(item), "Venenatis");
        for (var item : List.of("Ring of the gods","Vet'ion jr."))
            map.put(normalize(item), "Vet'ion");
        for (var item : List.of("Spirit angler headband","Spirit angler top","Spirit angler waders","Spirit angler boots","Tome of water","Tiny tempor"))
            map.put(normalize(item), "Tempoross");
        for (var item : List.of("Crystal tool seed","Zalcano shard"))
            map.put(normalize(item), "Zalcano");
        for (var item : List.of("Sunfire fanatic helm","Sunfire fanatic cuirass","Sunfire fanatic chausses","Echo crystal","Tonalztics of ralos","Dizana's quiver","Smol heredit"))
            map.put(normalize(item), "Sol Heredit");
        return Map.copyOf(map);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private final Map<String, GroupState> groups = new LinkedHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final GroupInventoryPersistence persistence;

    public GroupInventoryService(GroupInventoryPersistence persistence) {
        this.persistence = persistence;
        loadPersistedGroups();
    }

    public CreateGroupResponse createGroup(String groupName) {
        synchronized (groups) {
            var sanitizedGroupName = sanitizeGroupName(groupName);

            while (true) {
                var groupCode = generateGroupCode();
                var groupState = new GroupState(groupCode, sanitizedGroupName, Instant.now());
                if (!groups.containsKey(groupCode)) {
                    groups.put(groupCode, groupState);
                    persistGroups();
                    return new CreateGroupResponse(groupCode, sanitizedGroupName, groupState.createdAt, "/groups/" + groupCode);
                }
            }
        }
    }

    public GroupOverviewResponse updateMemberInventory(String groupCode, String memberName, InventoryUploadRequest request) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            var inventory = normalizeItems(request.items());
            var sanitizedMemberName = sanitizeMemberName(memberName);
            var memberKey = sanitizedMemberName.toLowerCase(Locale.ROOT);
            groupState.members.put(
                    memberKey,
                    new MemberInventory(sanitizedMemberName, Instant.now(), inventory)
            );
            persistGroups();
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse addTargetItem(String groupCode, String itemName, int quantity) {
        synchronized (groups) {
            if (quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target quantity must be greater than zero");
            }
            var groupState = findGroup(groupCode);
            var sanitizedItemName = sanitizeItemName(itemName);
            var itemKey = sanitizedItemName.toLowerCase(Locale.ROOT);
            var mutableTargets = new LinkedHashMap<>(groupState.targetItems);
            mutableTargets.put(itemKey, new NamedQuantity(sanitizedItemName, quantity));
            groupState.targetItems = mutableTargets;
            persistGroups();
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse removeTargetItem(String groupCode, String itemName) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            var sanitizedItemName = sanitizeItemName(itemName);
            var itemKey = sanitizedItemName.toLowerCase(Locale.ROOT);
            var mutableTargets = new LinkedHashMap<>(groupState.targetItems);
            mutableTargets.remove(itemKey);
            groupState.targetItems = mutableTargets;
            persistGroups();
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse renameGroup(String groupCode, String newGroupName) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            var sanitizedGroupName = sanitizeGroupName(newGroupName);
            var updated = new GroupState(
                    groupState.groupCode,
                    sanitizedGroupName,
                    groupState.createdAt,
                    groupState.members,
                    groupState.targetItems,
                    groupState.manualAdjustments
            );
            groups.put(groupState.groupCode, updated);
            persistGroups();
            return buildOverview(updated);
        }
    }

    public GroupOverviewResponse updateTargetItems(String groupCode, TargetItemsRequest request) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            groupState.targetItems = normalizeItems(request.items());
            persistGroups();
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse adjustItemQuantity(String groupCode, ManualAdjustmentRequest request) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            if (request.delta() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjustment delta cannot be zero");
            }

            var itemName = sanitizeItemName(request.itemName());
            var key = itemName.toLowerCase(Locale.ROOT);
            var existing = groupState.manualAdjustments.get(key);
            var nextQuantity = safeAdd(
                    existing == null ? 0 : existing.quantity(),
                    request.delta(),
                    "Adjustment quantity is too large for " + itemName
            );

            if (nextQuantity == 0) {
                groupState.manualAdjustments.remove(key);
            } else {
                groupState.manualAdjustments.put(key, new NamedQuantity(itemName, nextQuantity));
            }

            persistGroups();
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse getOverview(String groupCode) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse createMember(String groupCode, String memberName) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            var sanitizedMemberName = sanitizeMemberName(memberName);
            var memberKey = sanitizedMemberName.toLowerCase(Locale.ROOT);
            groupState.members.putIfAbsent(
                    memberKey,
                    new MemberInventory(sanitizedMemberName, Instant.now(), new LinkedHashMap<>())
            );
            persistGroups();
            return buildOverview(groupState);
        }
    }

    public GroupOverviewResponse addMemberItem(String groupCode, String memberName, String itemName, int quantity) {
        synchronized (groups) {
            if (quantity == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot be zero");
            }
            var groupState = findGroup(groupCode);
            var sanitizedMemberName = sanitizeMemberName(memberName);
            var memberKey = sanitizedMemberName.toLowerCase(Locale.ROOT);
            var sanitizedItemName = sanitizeItemName(itemName);
            var itemKey = sanitizedItemName.toLowerCase(Locale.ROOT);

            var existing = groupState.members.get(memberKey);
            var currentItems = existing == null
                    ? new LinkedHashMap<String, NamedQuantity>()
                    : new LinkedHashMap<>(existing.items());

            var existingItem = currentItems.get(itemKey);
            var newQuantity = safeAdd(
                    existingItem == null ? 0 : existingItem.quantity(),
                    quantity,
                    "Quantity is too large for " + sanitizedItemName
            );

            if (newQuantity <= 0) {
                currentItems.remove(itemKey);
            } else {
                currentItems.put(itemKey, new NamedQuantity(sanitizedItemName, newQuantity));
            }

            groupState.members.put(memberKey, new MemberInventory(sanitizedMemberName, Instant.now(), currentItems));
            persistGroups();
            return buildOverview(groupState);
        }
    }

    public void deleteGroup(String groupCode) {
        synchronized (groups) {
            var normalizedCode = normalizeGroupCode(groupCode);
            if (groups.remove(normalizedCode) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No group found for code " + normalizedCode);
            }
            persistGroups();
        }
    }

    public GroupOverviewResponse removeMember(String groupCode, String memberName) {
        synchronized (groups) {
            var groupState = findGroup(groupCode);
            var sanitizedMemberName = sanitizeMemberName(memberName);
            var memberKey = sanitizedMemberName.toLowerCase(Locale.ROOT);
            if (groupState.members.remove(memberKey) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No member found with name " + sanitizedMemberName);
            }
            persistGroups();
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
                aggregated.loggedQuantity = safeAdd(
                        aggregated.loggedQuantity,
                        item.quantity(),
                        "Logged quantity is too large for " + item.itemName()
                );
                aggregated.owners.add(member.memberName());
            }
        }

        for (var adjustment : groupState.manualAdjustments.values()) {
            var aggregated = itemIndex.computeIfAbsent(
                    adjustment.name().toLowerCase(Locale.ROOT),
                    ignored -> new AggregatedItem(adjustment.name())
            );
            aggregated.manualAdjustmentQuantity = safeAdd(
                    aggregated.manualAdjustmentQuantity,
                    adjustment.quantity(),
                    "Manual adjustment quantity is too large for " + adjustment.name()
            );
        }

        var itemSummaries = itemIndex.values().stream()
                .filter(item -> item.loggedQuantity > 0 || item.manualAdjustmentQuantity > 0)
                .sorted(Comparator.comparing(AggregatedItem::displayName, String.CASE_INSENSITIVE_ORDER))
                .map(item -> new GroupItemSummaryResponse(
                        item.displayName,
                        item.loggedQuantity,
                        item.manualAdjustmentQuantity,
                        Math.max(
                                safeAdd(
                                        item.loggedQuantity,
                                        item.manualAdjustmentQuantity,
                                        "Total quantity is too large for " + item.displayName
                                ),
                                0
                        ),
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
                            : Math.max(
                                    safeAdd(
                                            aggregated.loggedQuantity,
                                            aggregated.manualAdjustmentQuantity,
                                            "Current quantity is too large for " + target.name()
                                    ),
                                    0
                            );
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

        var bossGroups = buildBossGroups(itemSummaries);

        return new GroupOverviewResponse(
                groupState.groupCode,
                groupState.groupName,
                Instant.now(),
                members.size(),
                members,
                itemSummaries,
                targetProgress,
                manualAdjustments,
                bossGroups
        );
    }

    private List<GroupBossGroupResponse> buildBossGroups(List<GroupItemSummaryResponse> itemSummaries) {
        var grouped = new LinkedHashMap<String, List<GroupItemSummaryResponse>>();
        for (var item : itemSummaries) {
            var boss = BOSS_BY_ITEM.getOrDefault(normalize(item.itemName()), "Other");
            grouped.computeIfAbsent(boss, ignored -> new ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream()
                .map(entry -> new GroupBossGroupResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
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
            var quantity = safeAdd(
                    existing == null ? 0 : existing.quantity(),
                    itemRequest.quantity(),
                    "Quantity is too large for " + itemName
            );
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

    private void loadPersistedGroups() {
        for (var entry : persistence.load().entrySet()) {
            groups.put(entry.getKey(), toGroupState(entry.getValue()));
        }
    }

    private void persistGroups() {
        var persistedGroups = new LinkedHashMap<String, GroupInventoryPersistence.StoredGroupState>();
        for (var entry : groups.entrySet()) {
            persistedGroups.put(entry.getKey(), toStoredGroupState(entry.getValue()));
        }
        persistence.save(persistedGroups);
    }

    private GroupState toGroupState(GroupInventoryPersistence.StoredGroupState storedGroupState) {
        var memberMap = new LinkedHashMap<String, MemberInventory>();
        if (storedGroupState.members() != null) {
            for (var entry : storedGroupState.members().entrySet()) {
                var storedMember = entry.getValue();
                memberMap.put(
                        entry.getKey(),
                        new MemberInventory(
                                storedMember.memberName(),
                                storedMember.updatedAt(),
                                toNamedQuantityMap(storedMember.items())
                        )
                );
            }
        }

        return new GroupState(
                storedGroupState.groupCode(),
                storedGroupState.groupName(),
                storedGroupState.createdAt(),
                memberMap,
                toNamedQuantityMap(storedGroupState.targetItems()),
                toNamedQuantityMap(storedGroupState.manualAdjustments())
        );
    }

    private GroupInventoryPersistence.StoredGroupState toStoredGroupState(GroupState groupState) {
        var storedMembers = new LinkedHashMap<String, GroupInventoryPersistence.StoredMemberInventory>();
        for (var entry : groupState.members.entrySet()) {
            var member = entry.getValue();
            storedMembers.put(
                    entry.getKey(),
                    new GroupInventoryPersistence.StoredMemberInventory(
                            member.memberName(),
                            member.updatedAt(),
                            toStoredNamedQuantityMap(member.items())
                    )
            );
        }

        return new GroupInventoryPersistence.StoredGroupState(
                groupState.groupCode,
                groupState.groupName,
                groupState.createdAt,
                storedMembers,
                toStoredNamedQuantityMap(groupState.targetItems),
                toStoredNamedQuantityMap(groupState.manualAdjustments)
        );
    }

    private LinkedHashMap<String, NamedQuantity> toNamedQuantityMap(
            Map<String, GroupInventoryPersistence.StoredNamedQuantity> storedItems
    ) {
        var namedQuantities = new LinkedHashMap<String, NamedQuantity>();
        if (storedItems == null) {
            return namedQuantities;
        }

        for (var entry : storedItems.entrySet()) {
            var storedQuantity = entry.getValue();
            namedQuantities.put(entry.getKey(), new NamedQuantity(storedQuantity.name(), storedQuantity.quantity()));
        }
        return namedQuantities;
    }

    private LinkedHashMap<String, GroupInventoryPersistence.StoredNamedQuantity> toStoredNamedQuantityMap(
            Map<String, NamedQuantity> items
    ) {
        var storedItems = new LinkedHashMap<String, GroupInventoryPersistence.StoredNamedQuantity>();
        for (var entry : items.entrySet()) {
            var quantity = entry.getValue();
            storedItems.put(
                    entry.getKey(),
                    new GroupInventoryPersistence.StoredNamedQuantity(quantity.name(), quantity.quantity())
            );
        }
        return storedItems;
    }

    private int safeAdd(int base, int delta, String message) {
        try {
            return Math.addExact(base, delta);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, exception);
        }
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

        private GroupState(
                String groupCode,
                String groupName,
                Instant createdAt,
                Map<String, MemberInventory> members,
                Map<String, NamedQuantity> targetItems,
                Map<String, NamedQuantity> manualAdjustments
        ) {
            this.groupCode = groupCode;
            this.groupName = groupName;
            this.createdAt = createdAt;
            this.members.putAll(members);
            this.targetItems = new LinkedHashMap<>(targetItems);
            this.manualAdjustments.putAll(manualAdjustments);
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
