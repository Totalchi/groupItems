package be.vdab.osrsplugin.runelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BankSnapshot
{
	private final List<SyncModels.ItemQuantityPayload> items;
	private final String fingerprint;

	BankSnapshot(List<SyncModels.ItemQuantityPayload> items, String fingerprint)
	{
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
		this.fingerprint = fingerprint;
	}

	List<SyncModels.ItemQuantityPayload> getItems()
	{
		return items;
	}

	String getFingerprint()
	{
		return fingerprint;
	}

	boolean isEmpty()
	{
		return items.isEmpty();
	}

	SyncModels.InventoryUploadRequest toUploadRequest()
	{
		return new SyncModels.InventoryUploadRequest(items);
	}
}
