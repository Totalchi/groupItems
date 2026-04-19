package be.vdab.osrsplugin.runelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SyncModels
{
	private SyncModels()
	{
	}

	static final class ItemQuantityPayload
	{
		private final String itemName;
		private final int quantity;

		ItemQuantityPayload(String itemName, int quantity)
		{
			this.itemName = itemName;
			this.quantity = quantity;
		}

		String getItemName()
		{
			return itemName;
		}

		int getQuantity()
		{
			return quantity;
		}
	}

	static final class InventoryUploadRequest
	{
		private final List<ItemQuantityPayload> items;

		InventoryUploadRequest(List<ItemQuantityPayload> items)
		{
			this.items = Collections.unmodifiableList(new ArrayList<>(items));
		}

		List<ItemQuantityPayload> getItems()
		{
			return items;
		}
	}

	static final class GroupOverviewResponse
	{
		String groupKey;
		String groupName;
		int memberCount;
		List<MemberInventoryResponse> members = Collections.emptyList();
		List<GroupItemSummaryResponse> itemSummaries = Collections.emptyList();
		List<TargetProgressResponse> targetProgress = Collections.emptyList();
	}

	static final class MemberInventoryResponse
	{
		String memberName;
	}

	static final class GroupItemSummaryResponse
	{
		String itemName;
		int totalQuantity;
		List<String> owners = Collections.emptyList();
		List<String> missingMembers = Collections.emptyList();
	}

	static final class TargetProgressResponse
	{
		String itemName;
		int targetQuantity;
		int currentQuantity;
		int missingQuantity;
		List<String> owners = Collections.emptyList();
	}
}
