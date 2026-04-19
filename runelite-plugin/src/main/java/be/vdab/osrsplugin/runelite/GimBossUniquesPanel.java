package be.vdab.osrsplugin.runelite;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class GimBossUniquesPanel extends PluginPanel
{
	private static final String COLOR_COMPLETE = "#22cc44";
	private static final String COLOR_PARTIAL  = "#f0c040";
	private static final String COLOR_MISSING  = "#cc4444";
	private static final String COLOR_TARGET_DONE = "#22cc44";
	private static final String COLOR_TARGET_PROGRESS = "#f0c040";
	private static final String COLOR_TARGET_MISSING  = "#cc4444";

	private final JLabel statusValue = valueLabel();
	private final JLabel groupValue = valueLabel();
	private final JLabel syncValue = valueLabel();
	private final JEditorPane overviewPane = new JEditorPane("text/html", "");

	GimBossUniquesPanel(Runnable uploadAction, Runnable refreshAction)
	{
		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Shared Boss Uniques", SwingConstants.LEFT);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel summaryPanel = new JPanel(new GridLayout(0, 2, 8, 6));
		summaryPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		summaryPanel.add(keyLabel("Status"));
		summaryPanel.add(statusValue);
		summaryPanel.add(keyLabel("Group"));
		summaryPanel.add(groupValue);
		summaryPanel.add(keyLabel("Last sync"));
		summaryPanel.add(syncValue);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
		buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton uploadButton = new JButton("Sync scanned log");
		uploadButton.addActionListener(event -> uploadAction.run());

		JButton refreshButton = new JButton("Refresh overview");
		refreshButton.addActionListener(event -> refreshAction.run());

		buttonPanel.add(uploadButton);
		buttonPanel.add(refreshButton);

		top.add(title);
		top.add(Box.createRigidArea(new Dimension(0, 8)));
		top.add(summaryPanel);
		top.add(Box.createRigidArea(new Dimension(0, 8)));
		top.add(buttonPanel);

		overviewPane.setEditable(false);
		overviewPane.setOpaque(true);
		overviewPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		overviewPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		overviewPane.setFont(FontManager.getRunescapeSmallFont());
		overviewPane.setText(renderPlaceholder("Configure a sync server and group code, then open boss pages in your collection log."));

		JScrollPane scrollPane = new JScrollPane(overviewPane);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		add(top, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	void render(String status, String groupKey, String syncText, BossUniqueOverviewBuilder.GroupOverviewViewModel viewModel)
	{
		statusValue.setText(status);
		groupValue.setText(groupKey == null || groupKey.isBlank() ? "-" : groupKey + " (" + viewModel.getMemberCount() + " synced)");
		syncValue.setText(syncText == null || syncText.isBlank() ? "-" : syncText);
		overviewPane.setText(renderOverview(viewModel));
		overviewPane.setCaretPosition(0);
	}

	void renderPlaceholderState(String status, String groupKey, String syncText, String message)
	{
		statusValue.setText(status);
		groupValue.setText(groupKey == null || groupKey.isBlank() ? "-" : groupKey);
		syncValue.setText(syncText == null || syncText.isBlank() ? "-" : syncText);
		overviewPane.setText(renderPlaceholder(message));
		overviewPane.setCaretPosition(0);
	}

	private JLabel keyLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		return label;
	}

	private JLabel valueLabel()
	{
		JLabel label = new JLabel("-");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private String renderOverview(BossUniqueOverviewBuilder.GroupOverviewViewModel viewModel)
	{
		int denominator = Math.max(viewModel.getMemberCount(), 1);
		StringBuilder html = new StringBuilder();
		html.append("<html><body style='font-family:sans-serif; color:#d8d8d8; background:#1e1e1e;'>");

		// Members
		html.append("<h3 style='margin-top:0;'>Group members</h3>");
		if (viewModel.getMembers().isEmpty())
		{
			html.append("<p>No member snapshots uploaded yet.</p>");
		}
		else
		{
			html.append("<p>").append(escapeHtml(String.join(", ", viewModel.getMembers()))).append("</p>");
		}

		// Target progress
		List<SyncModels.TargetProgressResponse> targets = viewModel.getTargetProgress();
		if (!targets.isEmpty())
		{
			html.append("<h3 style='margin-bottom:6px;'>Target progress</h3>");
			html.append("<table width='100%' cellspacing='0' cellpadding='4' style='border-collapse:collapse; margin-bottom:10px;'>");
			html.append("<tr style='background:#2a2a2a;'>")
				.append("<th align='left'>Item</th>")
				.append("<th align='left'>Progress</th>")
				.append("<th align='left'>Remaining</th>")
				.append("</tr>");

			for (SyncModels.TargetProgressResponse target : targets)
			{
				String color = target.missingQuantity == 0 ? COLOR_TARGET_DONE
					: target.currentQuantity > 0 ? COLOR_TARGET_PROGRESS
					: COLOR_TARGET_MISSING;
				html.append("<tr>")
					.append("<td style='color:").append(color).append(";'>").append(escapeHtml(target.itemName)).append("</td>")
					.append("<td>").append(target.currentQuantity).append("/").append(target.targetQuantity).append("</td>")
					.append("<td>").append(target.missingQuantity).append("</td>")
					.append("</tr>");
			}
			html.append("</table>");
		}

		// Boss sections
		for (BossUniqueOverviewBuilder.BossSection section : viewModel.getSections())
		{
			if (section.getRows().isEmpty())
			{
				continue;
			}

			html.append("<h3 style='margin-bottom:6px;'>").append(escapeHtml(section.getBossName())).append("</h3>");
			html.append("<table width='100%' cellspacing='0' cellpadding='4' style='border-collapse:collapse; margin-bottom:10px;'>");
			html.append("<tr style='background:#2a2a2a;'>")
				.append("<th align='left'>Item</th>")
				.append("<th align='left'>Owners</th>")
				.append("<th align='left'>Total</th>")
				.append("<th align='left'>Members</th>")
				.append("</tr>");

			for (BossUniqueOverviewBuilder.BossUniqueRow row : section.getRows())
			{
				String color;
				if (viewModel.getMemberCount() > 0 && row.getOwnerCount() >= viewModel.getMemberCount())
				{
					color = COLOR_COMPLETE;
				}
				else if (row.getOwnerCount() > 0)
				{
					color = COLOR_PARTIAL;
				}
				else
				{
					color = COLOR_MISSING;
				}

				html.append("<tr>")
					.append("<td style='color:").append(color).append(";'>").append(escapeHtml(row.getItemName())).append("</td>")
					.append("<td>").append(row.getOwnerCount()).append("/").append(denominator).append("</td>")
					.append("<td>").append(row.getTotalQuantity()).append("</td>")
					.append("<td>").append(escapeHtml(row.getOwners().isEmpty() ? "-" : String.join(", ", row.getOwners()))).append("</td>")
					.append("</tr>");
			}
			html.append("</table>");
		}

		html.append("</body></html>");
		return html.toString();
	}

	private String renderPlaceholder(String message)
	{
		return "<html><body style='font-family:sans-serif; color:#d8d8d8; background:#1e1e1e;'>"
			+ "<p style='margin-top:0;'>" + escapeHtml(message) + "</p></body></html>";
	}

	private String escapeHtml(String value)
	{
		String escaped = value == null ? "" : value;
		return escaped
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}
}
