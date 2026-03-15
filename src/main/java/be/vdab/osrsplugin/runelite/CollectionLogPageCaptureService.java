package be.vdab.osrsplugin.runelite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

@Singleton
class CollectionLogPageCaptureService
{
	private final Client client;
	private final ItemManager itemManager;

	@Inject
	CollectionLogPageCaptureService(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	CapturedPage captureCurrentPage()
	{
		Widget header = client.getWidget(InterfaceID.Collection.HEADER_TEXT);
		if (header == null)
		{
			return null;
		}

		Widget titleWidget = header.getChild(0);
		if (titleWidget == null || titleWidget.getText() == null || titleWidget.getText().isBlank())
		{
			return null;
		}

		String bossName = Text.removeTags(titleWidget.getText()).trim();
		List<BossUniqueCatalog.BossUniqueDefinition> definitions = BossUniqueCatalog.forBoss(bossName);
		if (definitions.isEmpty())
		{
			return null;
		}

		Widget itemsContents = client.getWidget(InterfaceID.Collection.ITEMS_CONTENTS);
		if (itemsContents == null || itemsContents.getChildren() == null)
		{
			return null;
		}

		Map<String, Integer> quantities = new LinkedHashMap<>();
		for (BossUniqueCatalog.BossUniqueDefinition definition : definitions)
		{
			quantities.put(definition.getNormalizedItemName(), 0);
		}

		boolean sawRelevantWidget = false;
		for (Widget child : itemsContents.getChildren())
		{
			if (child == null || child.getItemId() <= 0)
			{
				continue;
			}

			int canonicalId = itemManager.canonicalize(child.getItemId());
			ItemComposition itemComposition = itemManager.getItemComposition(canonicalId);
			String itemName = sanitizeItemName(itemComposition);
			String normalizedItemName = BossUniqueCatalog.normalizeItem(itemName);
			if (!quantities.containsKey(normalizedItemName))
			{
				continue;
			}

			sawRelevantWidget = true;
			int quantity = child.getOpacity() == 0 ? Math.max(child.getItemQuantity(), 1) : 0;
			quantities.put(normalizedItemName, Math.max(quantities.get(normalizedItemName), quantity));
		}

		if (!sawRelevantWidget)
		{
			return null;
		}

		StringBuilder signature = new StringBuilder(BossUniqueCatalog.normalizeBoss(bossName)).append('|');
		for (BossUniqueCatalog.BossUniqueDefinition definition : definitions)
		{
			signature.append(definition.getNormalizedItemName())
				.append('=')
				.append(quantities.getOrDefault(definition.getNormalizedItemName(), 0))
				.append(';');
		}

		return new CapturedPage(bossName, quantities, signature.toString());
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

	static final class CapturedPage
	{
		private final String bossName;
		private final Map<String, Integer> quantitiesByNormalizedName;
		private final String signature;

		private CapturedPage(String bossName, Map<String, Integer> quantitiesByNormalizedName, String signature)
		{
			this.bossName = bossName;
			this.quantitiesByNormalizedName = quantitiesByNormalizedName;
			this.signature = signature;
		}

		String getBossName()
		{
			return bossName;
		}

		Map<String, Integer> getQuantitiesByNormalizedName()
		{
			return quantitiesByNormalizedName;
		}

		String getSignature()
		{
			return signature;
		}
	}
}
