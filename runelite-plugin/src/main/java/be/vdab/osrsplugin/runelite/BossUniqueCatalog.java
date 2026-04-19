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
		// GWD
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

		// Nex
		item("Nex", "Torva full helm"),
		item("Nex", "Torva platebody"),
		item("Nex", "Torva platelegs"),
		item("Nex", "Nihil horn"),
		item("Nex", "Zaryte vambraces"),
		item("Nex", "Ancient hilt"),

		// Corp
		item("Corporeal Beast", "Spectral sigil"),
		item("Corporeal Beast", "Arcane sigil"),
		item("Corporeal Beast", "Elysian sigil"),
		item("Corporeal Beast", "Holy elixir"),

		// Zulrah / Vorkath
		item("Zulrah", "Tanzanite fang"),
		item("Zulrah", "Magic fang"),
		item("Zulrah", "Serpentine visage"),
		item("Zulrah", "Uncut onyx"),
		item("Vorkath", "Skeletal visage"),
		item("Vorkath", "Dragonbone necklace"),

		// Alch Hydra
		item("Alchemical Hydra", "Hydra's claw"),
		item("Alchemical Hydra", "Hydra leather"),
		item("Alchemical Hydra", "Hydra tail"),
		item("Alchemical Hydra", "Dragon knife"),
		item("Alchemical Hydra", "Dragon thrownaxe"),

		// Demonic gorillas
		item("Demonic gorillas", "Zenyte shard"),

		// Cerberus
		item("Cerberus", "Primordial crystal"),
		item("Cerberus", "Pegasian crystal"),
		item("Cerberus", "Eternal crystal"),
		item("Cerberus", "Smouldering stone"),

		// Abyssal Sire
		item("Abyssal Sire", "Abyssal dagger"),
		item("Abyssal Sire", "Bludgeon spine"),
		item("Abyssal Sire", "Bludgeon claw"),
		item("Abyssal Sire", "Bludgeon axon"),
		item("Abyssal Sire", "Unsired"),

		// Grotesque Guardians
		item("Grotesque Guardians", "Granite ring"),
		item("Grotesque Guardians", "Granite hammer"),
		item("Grotesque Guardians", "Black tourmaline core"),

		// Phantom Muspah
		item("Phantom Muspah", "Venator shard"),
		item("Phantom Muspah", "Ancient icon"),
		item("Phantom Muspah", "Saturated heart"),

		// The Gauntlet
		item("The Gauntlet", "Enhanced crystal weapon seed"),
		item("The Gauntlet", "Armour seed"),
		item("The Gauntlet", "Weapon seed"),

		// The Nightmare
		item("The Nightmare", "Inquisitor's great helm"),
		item("The Nightmare", "Inquisitor's hauberk"),
		item("The Nightmare", "Inquisitor's plateskirt"),
		item("The Nightmare", "Nightmare staff"),
		item("The Nightmare", "Harmonised orb"),
		item("The Nightmare", "Volatile orb"),
		item("The Nightmare", "Eldritch orb"),

		// Dagannoth Kings
		item("Dagannoth Kings", "Berserker ring"),
		item("Dagannoth Kings", "Seers ring"),
		item("Dagannoth Kings", "Warrior ring"),
		item("Dagannoth Kings", "Archers ring"),
		item("Dagannoth Kings", "Dragon axe"),
		item("Dagannoth Kings", "Mud battlestaff"),
		item("Dagannoth Kings", "Seercull"),

		// Raids 1 – Chambers of Xeric
		item("Chambers of Xeric", "Twisted bow"),
		item("Chambers of Xeric", "Elder maul"),
		item("Chambers of Xeric", "Kodai insignia"),
		item("Chambers of Xeric", "Dragon hunter crossbow"),
		item("Chambers of Xeric", "Twisted buckler"),
		item("Chambers of Xeric", "Dinh's bulwark"),
		item("Chambers of Xeric", "Ancestral hat"),
		item("Chambers of Xeric", "Ancestral robe top"),
		item("Chambers of Xeric", "Ancestral robe bottom"),
		item("Chambers of Xeric", "Dragon claws"),
		item("Chambers of Xeric", "Dexterous prayer scroll"),
		item("Chambers of Xeric", "Arcane prayer scroll"),

		// Raids 2 – Theatre of Blood
		item("Theatre of Blood", "Avernic defender hilt"),
		item("Theatre of Blood", "Ghrazi rapier"),
		item("Theatre of Blood", "Scythe of vitur"),
		item("Theatre of Blood", "Sanguinesti staff"),
		item("Theatre of Blood", "Justiciar faceguard"),
		item("Theatre of Blood", "Justiciar chestguard"),
		item("Theatre of Blood", "Justiciar legguards"),
		item("Theatre of Blood", "Lil' Zik"),

		// Raids 3 – Tombs of Amascut
		item("Tombs of Amascut", "Tumeken's shadow"),
		item("Tombs of Amascut", "Elidinis' ward"),
		item("Tombs of Amascut", "Masori mask"),
		item("Tombs of Amascut", "Masori body"),
		item("Tombs of Amascut", "Masori chaps"),
		item("Tombs of Amascut", "Osmumten's fang"),
		item("Tombs of Amascut", "Lightbearer"),
		item("Tombs of Amascut", "Thread of elidinis"),
		item("Tombs of Amascut", "Breach of the scarab"),

		// Desert Treasure II bosses
		item("Duke Sucellus", "Magus ring"),
		item("Duke Sucellus", "Virtus mask"),
		item("Duke Sucellus", "Virtus robe top"),
		item("Duke Sucellus", "Virtus robe bottom"),
		item("The Leviathan", "Venator ring"),
		item("The Leviathan", "Leviathan's lure"),
		item("The Whisperer", "Bellator ring"),
		item("The Whisperer", "Siren's staff"),
		item("Vardorvis", "Ultor ring"),
		item("Vardorvis", "Executioner's axe head"),
		item("Vardorvis", "Chromium ingot"),

		// Barrows
		item("Barrows Chests", "Ahrim's hood"),
		item("Barrows Chests", "Ahrim's staff"),
		item("Barrows Chests", "Ahrim's robetop"),
		item("Barrows Chests", "Ahrim's robe skirt"),
		item("Barrows Chests", "Dharok's helm"),
		item("Barrows Chests", "Dharok's platebody"),
		item("Barrows Chests", "Dharok's platelegs"),
		item("Barrows Chests", "Dharok's greataxe"),
		item("Barrows Chests", "Guthan's helm"),
		item("Barrows Chests", "Guthan's platebody"),
		item("Barrows Chests", "Guthan's chainskirt"),
		item("Barrows Chests", "Guthan's warspear"),
		item("Barrows Chests", "Karil's coif"),
		item("Barrows Chests", "Karil's leathertop"),
		item("Barrows Chests", "Karil's leatherskirt"),
		item("Barrows Chests", "Karil's crossbow"),
		item("Barrows Chests", "Torag's helm"),
		item("Barrows Chests", "Torag's platebody"),
		item("Barrows Chests", "Torag's platelegs"),
		item("Barrows Chests", "Torag's hammers"),
		item("Barrows Chests", "Verac's helm"),
		item("Barrows Chests", "Verac's brassard"),
		item("Barrows Chests", "Verac's plateskirt"),
		item("Barrows Chests", "Verac's flail"),

		// Other bosses
		item("Sarachnis", "Sarachnis cudgel"),
		item("Sarachnis", "Jar of eyes"),
		item("Sarachnis", "Giant egg sac"),

		item("Kalphite Queen", "Dragon chainbody"),
		item("Kalphite Queen", "KQ head"),
		item("Kalphite Queen", "Jar of sand"),
		item("Kalphite Queen", "Kalphite princess"),

		item("King Black Dragon", "Draconic visage"),
		item("King Black Dragon", "KBD heads"),
		item("King Black Dragon", "Prince black dragon"),

		item("Giant Mole", "Mole skin"),
		item("Giant Mole", "Mole claw"),

		item("Skotizo", "Skotos"),
		item("Skotizo", "Dark claw"),
		item("Skotizo", "Jar of darkness"),
		item("Skotizo", "Uncut onyx"),

		item("Chaos Elemental", "Dragon pickaxe"),
		item("Chaos Elemental", "Pet chaos elemental"),

		item("Kraken", "Kraken tentacle"),
		item("Kraken", "Jar of dirt"),
		item("Kraken", "Pet kraken"),

		item("Scorpia", "Odium shard 3"),
		item("Scorpia", "Malediction shard 3"),
		item("Scorpia", "Scorpia's offspring"),

		item("Callisto", "Tyrannical ring"),
		item("Callisto", "Callisto cub"),
		item("Artio", "Tyrannical ring"),
		item("Artio", "Callisto cub"),

		item("Venenatis", "Treasonous ring"),
		item("Venenatis", "Venenatis spiderling"),
		item("Spindel", "Treasonous ring"),
		item("Spindel", "Venenatis spiderling"),

		item("Vet'ion", "Ring of the gods"),
		item("Vet'ion", "Vet'ion jr."),
		item("Calvar'ion", "Ring of the gods"),
		item("Calvar'ion", "Vet'ion jr."),

		item("Tempoross", "Spirit angler headband"),
		item("Tempoross", "Spirit angler top"),
		item("Tempoross", "Spirit angler waders"),
		item("Tempoross", "Spirit angler boots"),
		item("Tempoross", "Tome of water"),
		item("Tempoross", "Tiny tempor"),

		item("Zalcano", "Crystal tool seed"),
		item("Zalcano", "Zalcano shard"),

		item("Sol Heredit", "Sunfire fanatic helm"),
		item("Sol Heredit", "Sunfire fanatic cuirass"),
		item("Sol Heredit", "Sunfire fanatic chausses"),
		item("Sol Heredit", "Echo crystal"),
		item("Sol Heredit", "Tonalztics of ralos"),
		item("Sol Heredit", "Dizana's quiver"),
		item("Sol Heredit", "Smol heredit")
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
