package be.vdab.osrsplugin.runelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BossUniqueOverviewBuilder
{
	GroupOverviewViewModel build(SyncModels.GroupOverviewResponse overviewResponse, boolean showOnlyMissing)
	{
		Map<String, SyncModels.GroupItemSummaryResponse> summaryIndex = new LinkedHashMap<>();
		for (SyncModels.GroupItemSummaryResponse itemSummary : safeList(overviewResponse.itemSummaries))
		{
			if (itemSummary == null || itemSummary.itemName == null)
			{
				continue;
			}
			summaryIndex.put(normalize(itemSummary.itemName), itemSummary);
		}

		List<BossSection> sections = new ArrayList<>();
		String currentBoss = null;
		BossSection currentSection = null;

		for (BossUniqueCatalog.BossUniqueDefinition definition : BossUniqueCatalog.all())
		{
			SyncModels.GroupItemSummaryResponse itemSummary = summaryIndex.get(definition.getNormalizedItemName());
			int ownerCount = itemSummary == null ? 0 : safeList(itemSummary.owners).size();
			int totalQuantity = itemSummary == null ? 0 : itemSummary.totalQuantity;

			if (showOnlyMissing && overviewResponse.memberCount > 0 && ownerCount >= overviewResponse.memberCount)
			{
				continue;
			}

			if (!definition.getBossName().equals(currentBoss))
			{
				currentBoss = definition.getBossName();
				currentSection = new BossSection(currentBoss);
				sections.add(currentSection);
			}

			currentSection.rows.add(new BossUniqueRow(
				definition.getItemName(),
				ownerCount,
				totalQuantity,
				itemSummary == null ? Collections.<String>emptyList() : new ArrayList<>(safeList(itemSummary.owners))
			));
		}

		List<String> members = new ArrayList<>();
		for (SyncModels.MemberInventoryResponse member : safeList(overviewResponse.members))
		{
			if (member != null && member.memberName != null && !member.memberName.isBlank())
			{
				members.add(member.memberName);
			}
		}

		return new GroupOverviewViewModel(overviewResponse.groupKey, overviewResponse.groupName, overviewResponse.memberCount, members, sections);
	}

	private <T> List<T> safeList(List<T> values)
	{
		return values == null ? Collections.<T>emptyList() : values;
	}

	private String normalize(String itemName)
	{
		return itemName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	static final class GroupOverviewViewModel
	{
		private final String groupCode;
		private final String groupName;
		private final int memberCount;
		private final List<String> members;
		private final List<BossSection> sections;

		private GroupOverviewViewModel(String groupCode, String groupName, int memberCount, List<String> members, List<BossSection> sections)
		{
			this.groupCode = groupCode;
			this.groupName = groupName;
			this.memberCount = memberCount;
			this.members = Collections.unmodifiableList(new ArrayList<>(members));
			this.sections = Collections.unmodifiableList(new ArrayList<>(sections));
		}

		String getGroupCode()
		{
			return groupCode;
		}

		String getGroupLabel(String fallbackCode)
		{
			String resolvedCode = groupCode == null || groupCode.isBlank() ? fallbackCode : groupCode;
			if (groupName == null || groupName.isBlank())
			{
				return resolvedCode;
			}
			if (resolvedCode == null || resolvedCode.isBlank())
			{
				return groupName;
			}
			return groupName + " [" + resolvedCode + "]";
		}

		int getMemberCount()
		{
			return memberCount;
		}

		List<String> getMembers()
		{
			return members;
		}

		List<BossSection> getSections()
		{
			return sections;
		}
	}

	static final class BossSection
	{
		private final String bossName;
		private final List<BossUniqueRow> rows = new ArrayList<>();

		private BossSection(String bossName)
		{
			this.bossName = bossName;
		}

		String getBossName()
		{
			return bossName;
		}

		List<BossUniqueRow> getRows()
		{
			return Collections.unmodifiableList(rows);
		}
	}

	static final class BossUniqueRow
	{
		private final String itemName;
		private final int ownerCount;
		private final int totalQuantity;
		private final List<String> owners;

		private BossUniqueRow(String itemName, int ownerCount, int totalQuantity, List<String> owners)
		{
			this.itemName = itemName;
			this.ownerCount = ownerCount;
			this.totalQuantity = totalQuantity;
			this.owners = Collections.unmodifiableList(new ArrayList<>(owners));
		}

		String getItemName()
		{
			return itemName;
		}

		int getOwnerCount()
		{
			return ownerCount;
		}

		int getTotalQuantity()
		{
			return totalQuantity;
		}

		List<String> getOwners()
		{
			return owners;
		}
	}
}
