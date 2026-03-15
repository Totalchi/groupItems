package be.vdab.osrsplugin.runelite;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class BossUniqueCatalog
{
	private static final List<BossUniqueDefinition> DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
		item("General Graardor", "Bandos chestplate"),
		item("General Graardor", "Bandos tassets"),
		item("General Graardor", "Bandos boots"),
		item("General Graardor", "Bandos hilt"),
		item("Kree'arra", "Armadyl helmet"),
		item("Kree'arra", "Armadyl chestplate"),
		item("Kree'arra", "Armadyl chainskirt"),
		item("Kree'arra", "Armadyl hilt"),
		item("Commander Zilyana", "Armadyl crossbow"),
		item("Commander Zilyana", "Saradomin sword"),
		item("Commander Zilyana", "Saradomin's light"),
		item("Commander Zilyana", "Saradomin hilt"),
		item("K'ril Tsutsaroth", "Zamorakian spear"),
		item("K'ril Tsutsaroth", "Staff of the dead"),
		item("K'ril Tsutsaroth", "Steam battlestaff"),
		item("K'ril Tsutsaroth", "Zamorak hilt"),
		item("Nex", "Torva full helm"),
		item("Nex", "Torva platebody"),
		item("Nex", "Torva platelegs"),
		item("Nex", "Nihil horn"),
		item("Nex", "Zaryte vambraces"),
		item("Nex", "Ancient hilt"),
		item("Corporeal Beast", "Spectral sigil"),
		item("Corporeal Beast", "Arcane sigil"),
		item("Corporeal Beast", "Elysian sigil"),
		item("Corporeal Beast", "Holy elixir"),
		item("Zulrah", "Tanzanite fang"),
		item("Zulrah", "Magic fang"),
		item("Zulrah", "Serpentine visage"),
		item("Zulrah", "Uncut onyx"),
		item("Vorkath", "Skeletal visage"),
		item("Vorkath", "Dragonbone necklace"),
		item("Alchemical Hydra", "Hydra's claw"),
		item("Alchemical Hydra", "Hydra leather"),
		item("Alchemical Hydra", "Hydra tail"),
		item("Alchemical Hydra", "Dragon knife"),
		item("Alchemical Hydra", "Dragon thrownaxe"),
		item("Demonic gorillas", "Zenyte shard"),
		item("Cerberus", "Primordial crystal"),
		item("Cerberus", "Pegasian crystal"),
		item("Cerberus", "Eternal crystal"),
		item("Cerberus", "Smouldering stone"),
		item("Abyssal Sire", "Abyssal dagger"),
		item("Abyssal Sire", "Bludgeon spine"),
		item("Abyssal Sire", "Bludgeon claw"),
		item("Abyssal Sire", "Bludgeon axon"),
		item("Abyssal Sire", "Unsired"),
		item("Grotesque Guardians", "Granite ring"),
		item("Grotesque Guardians", "Granite hammer"),
		item("Grotesque Guardians", "Black tourmaline core"),
		item("Phantom Muspah", "Venator shard"),
		item("Phantom Muspah", "Ancient icon"),
		item("Phantom Muspah", "Saturated heart"),
		item("The Gauntlet", "Enhanced crystal weapon seed"),
		item("The Gauntlet", "Armour seed"),
		item("The Gauntlet", "Weapon seed"),
		item("The Nightmare", "Inquisitor's great helm"),
		item("The Nightmare", "Inquisitor's hauberk"),
		item("The Nightmare", "Inquisitor's plateskirt"),
		item("The Nightmare", "Nightmare staff"),
		item("The Nightmare", "Harmonised orb"),
		item("The Nightmare", "Volatile orb"),
		item("The Nightmare", "Eldritch orb"),
		item("Dagannoth Kings", "Berserker ring"),
		item("Dagannoth Kings", "Seers ring"),
		item("Dagannoth Kings", "Warrior ring"),
		item("Dagannoth Kings", "Archers ring"),
		item("Dagannoth Kings", "Dragon axe"),
		item("Dagannoth Kings", "Mud battlestaff"),
		item("Dagannoth Kings", "Seercull")
	));
	private static final Map<String, List<BossUniqueDefinition>> DEFINITIONS_BY_BOSS = DEFINITIONS.stream()
		.collect(Collectors.collectingAndThen(
			Collectors.groupingBy(
				definition -> normalize(definition.getBossName()),
				LinkedHashMap::new,
				Collectors.toList()
			),
			Collections::unmodifiableMap
		));

	private BossUniqueCatalog()
	{
	}

	static List<BossUniqueDefinition> all()
	{
		return DEFINITIONS;
	}

	static int bossCount()
	{
		return DEFINITIONS_BY_BOSS.size();
	}

	static boolean containsBoss(String bossName)
	{
		return DEFINITIONS_BY_BOSS.containsKey(normalize(bossName));
	}

	static List<BossUniqueDefinition> forBoss(String bossName)
	{
		return DEFINITIONS_BY_BOSS.getOrDefault(normalize(bossName), Collections.emptyList());
	}

	static String normalizeBoss(String bossName)
	{
		return normalize(bossName);
	}

	static String normalizeItem(String itemName)
	{
		return normalize(itemName);
	}

	private static BossUniqueDefinition item(String bossName, String itemName)
	{
		return new BossUniqueDefinition(bossName, itemName, normalize(itemName));
	}

	private static String normalize(String itemName)
	{
		return itemName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	static final class BossUniqueDefinition
	{
		private final String bossName;
		private final String itemName;
		private final String normalizedItemName;

		private BossUniqueDefinition(String bossName, String itemName, String normalizedItemName)
		{
			this.bossName = bossName;
			this.itemName = itemName;
			this.normalizedItemName = normalizedItemName;
		}

		String getBossName()
		{
			return bossName;
		}

		String getItemName()
		{
			return itemName;
		}

		String getNormalizedItemName()
		{
			return normalizedItemName;
		}
	}
}
