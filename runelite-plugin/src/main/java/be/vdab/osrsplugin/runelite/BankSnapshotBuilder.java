package be.vdab.osrsplugin.runelite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

@Singleton
class BankSnapshotBuilder
{
	private final ItemManager itemManager;

	@Inject
	BankSnapshotBuilder(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	BankSnapshot build(ItemContainer bankContainer)
	{
		Map<String, NamedQuantity> aggregated = new LinkedHashMap<>();

		for (Item item : bankContainer.getItems())
		{
			if (item == null || item.getQuantity() <= 0 || item.getId() <= 0)
			{
				continue;
			}

			int canonicalId = itemManager.canonicalize(item.getId());
			if (canonicalId == ItemID.BANK_FILLER)
			{
				continue;
			}

			ItemComposition itemComposition = itemManager.getItemComposition(canonicalId);
			if (itemComposition.getPlaceholderTemplateId() != -1)
			{
				continue;
			}

			String itemName = sanitizeItemName(itemComposition);
			if (itemName.isEmpty())
			{
				continue;
			}

			String key = normalize(itemName);
			NamedQuantity existing = aggregated.get(key);
			int quantity = existing == null ? item.getQuantity() : existing.quantity + item.getQuantity();
			aggregated.put(key, new NamedQuantity(itemName, quantity));
		}

		ArrayList<SyncModels.ItemQuantityPayload> payload = new ArrayList<>();
		aggregated.values().stream()
			.sorted(Comparator.comparing(namedQuantity -> namedQuantity.name, String.CASE_INSENSITIVE_ORDER))
			.forEach(namedQuantity -> payload.add(new SyncModels.ItemQuantityPayload(namedQuantity.name, namedQuantity.quantity)));

		StringBuilder fingerprint = new StringBuilder();
		for (SyncModels.ItemQuantityPayload item : payload)
		{
			fingerprint.append(normalize(item.getItemName()))
				.append('=')
				.append(item.getQuantity())
				.append(';');
		}

		return new BankSnapshot(payload, fingerprint.toString());
	}

	private String sanitizeItemName(ItemComposition itemComposition)
	{
		String itemName = itemComposition.getMembersName();
		if (itemName == null || itemName.isBlank())
		{
			itemName = itemComposition.getName();
		}
		return itemName == null ? "" : itemName.trim();
	}

	private String normalize(String itemName)
	{
		return itemName.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
	}

	private static final class NamedQuantity
	{
		private final String name;
		private final int quantity;

		private NamedQuantity(String name, int quantity)
		{
			this.name = name;
			this.quantity = quantity;
		}
	}
}
