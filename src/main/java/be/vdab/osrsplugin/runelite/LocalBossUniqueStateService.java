package be.vdab.osrsplugin.runelite;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
class LocalBossUniqueStateService
{
	private static final String STATE_KEY = "localBossUniqueState";
	private static final Type STATE_TYPE = new TypeToken<Map<String, Integer>>() { }.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	private Map<String, Integer> quantitiesByNormalizedName;

	@Inject
	LocalBossUniqueStateService(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	boolean applyPage(CollectionLogPageCaptureService.CapturedPage capturedPage)
	{
		ensureLoaded();

		boolean changed = false;
		for (BossUniqueCatalog.BossUniqueDefinition definition : BossUniqueCatalog.forBoss(capturedPage.getBossName()))
		{
			String key = definition.getNormalizedItemName();
			int nextQuantity = capturedPage.getQuantitiesByNormalizedName().getOrDefault(key, 0);
			Integer existingQuantity = quantitiesByNormalizedName.get(key);

			if (existingQuantity == null || nextQuantity > existingQuantity)
			{
				quantitiesByNormalizedName.put(key, nextQuantity);
				changed = true;
			}
		}

		if (changed)
		{
			persist();
		}

		return changed;
	}

	BankSnapshot buildUploadSnapshot()
	{
		ensureLoaded();

		List<SyncModels.ItemQuantityPayload> items = new ArrayList<>();
		StringBuilder fingerprint = new StringBuilder();

		for (BossUniqueCatalog.BossUniqueDefinition definition : BossUniqueCatalog.all())
		{
			int quantity = quantitiesByNormalizedName.getOrDefault(definition.getNormalizedItemName(), 0);
			fingerprint.append(definition.getNormalizedItemName()).append('=').append(quantity).append(';');
			if (quantity > 0)
			{
				items.add(new SyncModels.ItemQuantityPayload(definition.getItemName(), quantity));
			}
		}

		return new BankSnapshot(items, fingerprint.toString());
	}

	int getScannedBossCount()
	{
		ensureLoaded();
		int scannedBosses = 0;
		for (String bossName : distinctBossNames())
		{
			boolean allKnown = true;
			for (BossUniqueCatalog.BossUniqueDefinition definition : BossUniqueCatalog.forBoss(bossName))
			{
				if (!quantitiesByNormalizedName.containsKey(definition.getNormalizedItemName()))
				{
					allKnown = false;
					break;
				}
			}
			if (allKnown)
			{
				scannedBosses++;
			}
		}
		return scannedBosses;
	}

	boolean hasAnyKnownData()
	{
		ensureLoaded();
		return !quantitiesByNormalizedName.isEmpty();
	}

	private List<String> distinctBossNames()
	{
		List<String> bossNames = new ArrayList<>();
		String previous = null;
		for (BossUniqueCatalog.BossUniqueDefinition definition : BossUniqueCatalog.all())
		{
			if (!definition.getBossName().equals(previous))
			{
				bossNames.add(definition.getBossName());
				previous = definition.getBossName();
			}
		}
		return bossNames;
	}

	private void ensureLoaded()
	{
		if (quantitiesByNormalizedName != null)
		{
			return;
		}

		String json = configManager.getConfiguration(GimBossUniquesConfig.GROUP, STATE_KEY);
		if (json == null || json.isBlank())
		{
			quantitiesByNormalizedName = new LinkedHashMap<>();
			return;
		}

		Map<String, Integer> loaded = gson.fromJson(json, STATE_TYPE);
		quantitiesByNormalizedName = loaded == null ? new LinkedHashMap<>() : new LinkedHashMap<>(loaded);
	}

	private void persist()
	{
		configManager.setConfiguration(
			GimBossUniquesConfig.GROUP,
			STATE_KEY,
			gson.toJson(quantitiesByNormalizedName, STATE_TYPE)
		);
	}
}
