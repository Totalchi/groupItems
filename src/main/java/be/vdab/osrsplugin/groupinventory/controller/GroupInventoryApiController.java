package be.vdab.osrsplugin.groupinventory.controller;

import be.vdab.osrsplugin.groupinventory.dto.GroupOverviewResponse;
import be.vdab.osrsplugin.groupinventory.dto.InventoryUploadRequest;
import be.vdab.osrsplugin.groupinventory.dto.TargetItemsRequest;
import be.vdab.osrsplugin.groupinventory.service.GroupInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@Validated
@RestController
@RequestMapping("/api/group-inventory")
public class GroupInventoryApiController {
    private static final String GROUP_CODE_HEADER = "X-Group-Code";
    private static final String GROUP_KEY_HEADER = "X-Group-Key";

    private final GroupInventoryService groupInventoryService;

    public GroupInventoryApiController(GroupInventoryService groupInventoryService) {
        this.groupInventoryService = groupInventoryService;
    }

    @PutMapping("/members/{memberName}")
    public GroupOverviewResponse uploadInventory(
            @RequestHeader(value = GROUP_CODE_HEADER, required = false) String groupCode,
            @RequestHeader(value = GROUP_KEY_HEADER, required = false) String legacyGroupKey,
            @org.springframework.web.bind.annotation.PathVariable @NotBlank @Size(max = 32) String memberName,
            @Valid @RequestBody InventoryUploadRequest request
    ) {
        return groupInventoryService.updateMemberInventory(resolveGroupCode(groupCode, legacyGroupKey), memberName, request);
    }

    @PutMapping("/targets")
    public GroupOverviewResponse updateTargets(
            @RequestHeader(value = GROUP_CODE_HEADER, required = false) String groupCode,
            @RequestHeader(value = GROUP_KEY_HEADER, required = false) String legacyGroupKey,
            @Valid @RequestBody TargetItemsRequest request
    ) {
        return groupInventoryService.updateTargetItems(resolveGroupCode(groupCode, legacyGroupKey), request);
    }

    @GetMapping
    public GroupOverviewResponse getOverview(
            @RequestHeader(value = GROUP_CODE_HEADER, required = false) String groupCode,
            @RequestHeader(value = GROUP_KEY_HEADER, required = false) String legacyGroupKey
    ) {
        return groupInventoryService.getOverview(resolveGroupCode(groupCode, legacyGroupKey));
    }

    private String resolveGroupCode(String groupCode, String legacyGroupKey) {
        if (groupCode != null && !groupCode.isBlank()) {
            return groupCode;
        }
        if (legacyGroupKey != null && !legacyGroupKey.isBlank()) {
            return legacyGroupKey;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either X-Group-Code or X-Group-Key is required");
    }
}
