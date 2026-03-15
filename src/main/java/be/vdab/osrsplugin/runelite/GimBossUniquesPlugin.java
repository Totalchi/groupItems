package be.vdab.osrsplugin.runelite;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Shared Boss Uniques",
	description = "Uploads boss collection-log data and shows shared unique progress for all participating players",
	tags = {"group", "collection", "boss", "uniques", "shared"}
)
public class GimBossUniquesPlugin extends Plugin
{
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Logger log = LoggerFactory.getLogger(GimBossUniquesPlugin.class);

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GimBossUniquesConfig config;

	@Inject
	private CollectionLogPageCaptureService collectionLogPageCaptureService;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private LocalBossUniqueStateService localBossUniqueStateService;

	@Inject
	private GroupSyncClient groupSyncClient;

	private final Object uploadLock = new Object();
	private final BossUniqueOverviewBuilder overviewBuilder = new BossUniqueOverviewBuilder();

	private NavigationButton navigationButton;
	private GimBossUniquesPanel panel;
	private ScheduledFuture<?> pendingUploadTask;
	private BankSnapshot pendingSnapshot;
	private String pendingMemberName;
	private boolean pendingForceUpload;
	private String lastSuccessfulFingerprint;
	private Instant lastSyncAt;
	private String lastObservedCollectionLogSignature;

	@Provides
	GimBossUniquesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GimBossUniquesConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new GimBossUniquesPanel(this::requestManualUpload, this::refreshOverview);
		navigationButton = NavigationButton.builder()
			.tooltip("Shared Boss Uniques")
			.icon(createNavigationIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		renderPlaceholder("Configure a sync server URL and group code, then open boss pages in your collection log.");
	}

	@Override
	protected void shutDown()
	{
		synchronized (uploadLock)
		{
			if (pendingUploadTask != null)
			{
				pendingUploadTask.cancel(false);
				pendingUploadTask = null;
			}
		}

		lastSuccessfulFingerprint = null;
		pendingSnapshot = null;
		pendingMemberName = null;
		pendingForceUpload = false;
		lastSyncAt = null;
		lastObservedCollectionLogSignature = null;

		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && isConfigured())
		{
			refreshOverview();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		CollectionLogPageCaptureService.CapturedPage capturedPage = collectionLogPageCaptureService.captureCurrentPage();
		if (capturedPage == null)
		{
			lastObservedCollectionLogSignature = null;
			return;
		}

		if (capturedPage.getSignature().equals(lastObservedCollectionLogSignature))
		{
			return;
		}
		lastObservedCollectionLogSignature = capturedPage.getSignature();

		boolean changed = localBossUniqueStateService.applyPage(capturedPage);
		if (changed)
		{
			renderPlaceholder("Captured " + capturedPage.getBossName() + ". " + localProgressText());
			if (config.autoSyncCollectionLog())
			{
				queueCurrentStateUpload(false);
			}
		}
		else
		{
			renderPlaceholder("Viewed " + capturedPage.getBossName() + ". " + localProgressText());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!GimBossUniquesConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (!isConfigured())
		{
			renderPlaceholder("Configure a sync server URL and group code, then open boss pages in your collection log.");
			return;
		}

		refreshOverview();
	}

	private void requestManualUpload()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			renderPlaceholder("Log into the game before syncing your collection log.");
			return;
		}

		if (!localBossUniqueStateService.hasAnyKnownData())
		{
			renderPlaceholder("Open at least one boss page in your collection log first.");
			return;
		}

		queueUpload(localBossUniqueStateService.buildUploadSnapshot(), localPlayer.getName(), true);
		renderPlaceholder("Manual collection-log sync queued...");
	}

	private void refreshOverview()
	{
		if (!isConfigured())
		{
			renderPlaceholder("Configure a sync server URL and group code first.");
			return;
		}

		renderPlaceholder("Refreshing group overview...");
		executor.execute(() ->
		{
			try
			{
				SyncModels.GroupOverviewResponse overview = groupSyncClient.fetchOverview(config.syncServerBaseUrl(), config.groupCode().trim());
				renderOverview(overview, "Overview refreshed");
			}
			catch (IOException e)
			{
				renderPlaceholder("Overview refresh failed: " + e.getMessage() + ". " + localProgressText());
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				renderPlaceholder("Overview refresh interrupted.");
			}
			catch (RuntimeException e)
			{
				log.warn("Overview refresh failed", e);
				renderPlaceholder("Overview refresh failed: " + formatRuntimeFailure(e) + ". " + localProgressText());
			}
		});
	}

	private void queueCurrentStateUpload(boolean forceUpload)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || !localBossUniqueStateService.hasAnyKnownData())
		{
			return;
		}

		queueUpload(localBossUniqueStateService.buildUploadSnapshot(), localPlayer.getName(), forceUpload);
	}

	private void queueUpload(BankSnapshot snapshot, String memberName, boolean forceUpload)
	{
		synchronized (uploadLock)
		{
			pendingSnapshot = snapshot;
			pendingMemberName = memberName;
			pendingForceUpload = pendingForceUpload || forceUpload;

			if (pendingUploadTask != null)
			{
				pendingUploadTask.cancel(false);
			}

			int delay = forceUpload ? 0 : Math.max(config.uploadDelayMs(), 250);
			pendingUploadTask = executor.schedule(this::flushPendingUpload, delay, TimeUnit.MILLISECONDS);
		}
	}

	private void flushPendingUpload()
	{
		BankSnapshot snapshot;
		String memberName;
		boolean forceUpload;

		synchronized (uploadLock)
		{
			snapshot = pendingSnapshot;
			memberName = pendingMemberName;
			forceUpload = pendingForceUpload;
			pendingForceUpload = false;
			pendingUploadTask = null;
		}

		if (snapshot == null || memberName == null)
		{
			return;
		}

		if (!forceUpload && snapshot.getFingerprint().equals(lastSuccessfulFingerprint))
		{
			renderPlaceholder("Collection log already synced. No changes detected. " + localProgressText());
			return;
		}

		if (!isConfigured())
		{
			renderPlaceholder("Configure a sync server URL and group code first.");
			return;
		}

		renderPlaceholder("Uploading collection log for " + memberName + "...");

		try
		{
			SyncModels.GroupOverviewResponse overview = groupSyncClient.uploadBank(
				config.syncServerBaseUrl(),
				config.groupCode().trim(),
				memberName,
				snapshot
			);
			lastSuccessfulFingerprint = snapshot.getFingerprint();
			lastSyncAt = Instant.now();
			renderOverview(overview, "Collection log synced for " + memberName);
		}
		catch (IOException e)
		{
			log.warn("Collection log upload failed", e);
			renderPlaceholder("Collection-log upload failed: " + e.getMessage() + ". " + localProgressText());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			renderPlaceholder("Collection-log upload interrupted.");
		}
		catch (RuntimeException e)
		{
			log.warn("Collection log upload failed", e);
			renderPlaceholder("Collection-log upload failed: " + formatRuntimeFailure(e) + ". " + localProgressText());
		}
	}

	private void renderOverview(SyncModels.GroupOverviewResponse overview, String status)
	{
		BossUniqueOverviewBuilder.GroupOverviewViewModel viewModel = overviewBuilder.build(overview, config.showOnlyMissing());
		String syncText = lastSyncAt == null ? "-" : DATE_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(lastSyncAt);

		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.render(status, viewModel.getGroupLabel(valueOrFallback(viewModel.getGroupCode(), config.groupCode())), syncText, viewModel);
			}
		});
	}

	private void renderPlaceholder(String message)
	{
		String syncText = lastSyncAt == null ? "-" : DATE_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(lastSyncAt);
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.renderPlaceholderState(message, config.groupCode(), syncText, message);
			}
		});
	}

	private boolean isConfigured()
	{
		return config.syncServerBaseUrl() != null
			&& !config.syncServerBaseUrl().trim().isEmpty()
			&& config.groupCode() != null
			&& !config.groupCode().trim().isEmpty();
	}

	private String localProgressText()
	{
		return "Scanned bosses: " + localBossUniqueStateService.getScannedBossCount() + "/" + BossUniqueCatalog.bossCount() + ".";
	}

	private String valueOrFallback(String preferred, String fallback)
	{
		if (preferred != null && !preferred.isBlank())
		{
			return preferred;
		}
		return fallback == null ? "" : fallback;
	}

	private String formatRuntimeFailure(RuntimeException exception)
	{
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

	private BufferedImage createNavigationIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(34, 34, 34));
		graphics.fillRoundRect(0, 0, 16, 16, 6, 6);
		graphics.setColor(new Color(214, 175, 77));
		graphics.fillOval(2, 2, 4, 4);
		graphics.fillOval(6, 2, 4, 4);
		graphics.fillOval(10, 2, 4, 4);
		graphics.setColor(new Color(92, 184, 92));
		graphics.fillOval(4, 8, 4, 4);
		graphics.fillOval(8, 8, 4, 4);
		graphics.dispose();
		return image;
	}
}
