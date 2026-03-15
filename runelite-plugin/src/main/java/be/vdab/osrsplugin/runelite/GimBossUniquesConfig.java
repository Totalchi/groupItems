package be.vdab.osrsplugin.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GimBossUniquesConfig.GROUP)
public interface GimBossUniquesConfig extends Config
{
	String GROUP = "gimBossUniques";

	@ConfigItem(
		keyName = "syncServerBaseUrl",
		name = "Sync server URL",
		description = "Base URL of the shared sync server, for example http://localhost:8080"
	)
	default String syncServerBaseUrl()
	{
		return "http://localhost:8080";
	}

	@ConfigItem(
		keyName = "groupCode",
		name = "Group code",
		description = "Private join code for the group created on the sync server"
	)
	default String groupCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "autoSyncCollectionLog",
		name = "Auto-sync collection log",
		description = "Automatically sync scanned boss collection-log pages when you open them"
	)
	default boolean autoSyncCollectionLog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "uploadDelayMs",
		name = "Upload delay (ms)",
		description = "Debounce delay before uploading updated collection-log data"
	)
	default int uploadDelayMs()
	{
		return 1500;
	}

	@ConfigItem(
		keyName = "showOnlyMissing",
		name = "Show only missing",
		description = "Only show unique items that are not yet owned by every synced member"
	)
	default boolean showOnlyMissing()
	{
		return false;
	}
}
