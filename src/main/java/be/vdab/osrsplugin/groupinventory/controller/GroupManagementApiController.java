package be.vdab.osrsplugin.groupinventory.controller;

import be.vdab.osrsplugin.groupinventory.dto.CreateGroupRequest;
import be.vdab.osrsplugin.groupinventory.dto.CreateGroupResponse;
import be.vdab.osrsplugin.groupinventory.service.GroupInventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupManagementApiController {
    private final GroupInventoryService groupInventoryService;

    public GroupManagementApiController(GroupInventoryService groupInventoryService) {
        this.groupInventoryService = groupInventoryService;
    }

    @PostMapping
    public CreateGroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return groupInventoryService.createGroup(request.groupName());
    }
}
