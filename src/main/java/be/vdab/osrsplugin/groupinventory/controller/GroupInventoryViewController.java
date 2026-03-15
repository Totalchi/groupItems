package be.vdab.osrsplugin.groupinventory.controller;

import be.vdab.osrsplugin.groupinventory.service.GroupInventoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
public class GroupInventoryViewController {
    private final GroupInventoryService groupInventoryService;

    public GroupInventoryViewController(GroupInventoryService groupInventoryService) {
        this.groupInventoryService = groupInventoryService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/groups")
    public String createGroup(@RequestParam("groupName") @NotBlank @Size(max = 64) String groupName) {
        var createdGroup = groupInventoryService.createGroup(groupName);
        return "redirect:" + createdGroup.overviewPath();
    }

    @GetMapping("/groups/{groupKey}")
    public String groupOverview(
            @PathVariable @NotBlank @Size(max = 64) String groupKey,
            Model model
    ) {
        model.addAttribute("overview", groupInventoryService.getOverview(groupKey));
        return "group-overview";
    }
}
