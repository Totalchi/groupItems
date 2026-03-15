package be.vdab.osrsplugin.runelite;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BossUniqueOverviewBuilderTest
{
	@Test
	public void buildShouldMapOwnedUniqueCountsIntoBossSections()
	{
		SyncModels.GroupOverviewResponse overviewResponse = new SyncModels.GroupOverviewResponse();
		overviewResponse.groupKey = "grp-ab12-cd34";
		overviewResponse.groupName = "Alpha Team";
		overviewResponse.memberCount = 2;

		SyncModels.MemberInventoryResponse alice = new SyncModels.MemberInventoryResponse();
		alice.memberName = "Alice";
		SyncModels.MemberInventoryResponse bob = new SyncModels.MemberInventoryResponse();
		bob.memberName = "Bob";
		overviewResponse.members = Arrays.asList(alice, bob);

		SyncModels.GroupItemSummaryResponse tassets = new SyncModels.GroupItemSummaryResponse();
		tassets.itemName = "Bandos tassets";
		tassets.totalQuantity = 3;
		tassets.owners = Arrays.asList("Alice", "Bob");

		overviewResponse.itemSummaries = Arrays.asList(tassets);

		BossUniqueOverviewBuilder.GroupOverviewViewModel viewModel = new BossUniqueOverviewBuilder().build(overviewResponse, false);
		BossUniqueOverviewBuilder.BossSection bandosSection = viewModel.getSections().get(0);
		BossUniqueOverviewBuilder.BossUniqueRow tassetsRow = bandosSection.getRows().get(1);

		assertEquals("General Graardor", bandosSection.getBossName());
		assertEquals("Bandos tassets", tassetsRow.getItemName());
		assertEquals(2, tassetsRow.getOwnerCount());
		assertEquals(3, tassetsRow.getTotalQuantity());
		assertEquals(Arrays.asList("Alice", "Bob"), tassetsRow.getOwners());
	}

	@Test
	public void buildShouldFilterCompletedRowsWhenConfigured()
	{
		SyncModels.GroupOverviewResponse overviewResponse = new SyncModels.GroupOverviewResponse();
		overviewResponse.groupKey = "grp-ab12-cd34";
		overviewResponse.groupName = "Alpha Team";
		overviewResponse.memberCount = 5;

		SyncModels.GroupItemSummaryResponse tassets = new SyncModels.GroupItemSummaryResponse();
		tassets.itemName = "Bandos tassets";
		tassets.totalQuantity = 5;
		tassets.owners = Arrays.asList("A", "B", "C", "D", "E");

		overviewResponse.itemSummaries = Arrays.asList(tassets);

		BossUniqueOverviewBuilder.GroupOverviewViewModel viewModel = new BossUniqueOverviewBuilder().build(overviewResponse, true);

		assertTrue(viewModel.getSections().get(0).getRows().stream().noneMatch(row -> "Bandos tassets".equals(row.getItemName())));
	}
}
