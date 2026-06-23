package com.minhiscores;

import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MinHiscoresPanel extends PluginPanel
{
	private enum SortState
	{
		DEFAULT("Sort"),
		DESCENDING("Sort ↓"),
		ASCENDING("Sort ↑");

		final String buttonLabel;

		SortState(String buttonLabel) { this.buttonLabel = buttonLabel; }

		SortState next()
		{
			SortState[] v = values();
			return v[(ordinal() + 1) % v.length];
		}
	}

	private static final NumberFormat XP_FORMAT = NumberFormat.getNumberInstance();
	static final Color ON_COLOR = new Color(0, 180, 0);
	static final Color OFF_COLOR = new Color(200, 50, 50);
	static final Color WARN_COLOR = new Color(200, 160, 0);

	private final MinHiscoresPlugin plugin;
	private final Map<Skill, SkillRow> rows = new EnumMap<>(Skill.class);
	private final JLabel playerNameLabel;
	private final JButton loadButton;
	private final JButton refreshButton;
	private final JButton sortButton;
	private final JPanel contentPanel;
	private final JPanel emptyCard;
	private final JPanel skillsPanel;
	private final OverallRow overallRow;

	private SortState sortState = SortState.DEFAULT;
	private boolean hideRanked = false;

	MinHiscoresPanel(MinHiscoresPlugin plugin, SkillIconManager skillIconManager)
	{
		super(false);
		this.plugin = plugin;
		setLayout(new BorderLayout(0, 0));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		playerNameLabel = new JLabel("Not logged in");
		playerNameLabel.setForeground(Color.GRAY);
		playerNameLabel.setFont(FontManager.getRunescapeSmallFont());

		loadButton = new JButton("Load HiScores");
		loadButton.setEnabled(false);
		loadButton.addActionListener(e -> plugin.fetchData());

		refreshButton = new JButton("Refresh");
		refreshButton.setFont(FontManager.getRunescapeSmallFont());
		refreshButton.setFocusPainted(false);
		refreshButton.setEnabled(false);
		refreshButton.addActionListener(e -> plugin.fetchData());

		sortButton = new JButton(SortState.DEFAULT.buttonLabel);
		sortButton.setFont(FontManager.getRunescapeSmallFont());
		sortButton.setFocusPainted(false);
		sortButton.addActionListener(e ->
		{
			sortState = sortState.next();
			sortButton.setText(sortState.buttonLabel);
			SwingUtilities.invokeLater(this::refreshOrder);
		});

		add(buildHeader(), BorderLayout.NORTH);

		overallRow = new OverallRow();
		emptyCard = buildEmptyCard();
		skillsPanel = buildSkillsPanel(skillIconManager, plugin);

		contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.add(emptyCard, BorderLayout.CENTER);

		JScrollPane scroll = new JScrollPane(contentPanel);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		add(scroll, BorderLayout.CENTER);
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Am I There Yet?");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.add(title);

		header.add(Box.createVerticalStrut(4));

		JPanel nameRow = new JPanel(new BorderLayout(4, 0));
		nameRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		nameRow.add(playerNameLabel, BorderLayout.CENTER);
		nameRow.add(refreshButton, BorderLayout.EAST);
		header.add(nameRow);

		header.add(Box.createVerticalStrut(6));

		JPanel controlRow = new JPanel(new BorderLayout(8, 0));
		controlRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		controlRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JCheckBox hideBox = new JCheckBox("Hide ranked");
		hideBox.setForeground(Color.LIGHT_GRAY);
		hideBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hideBox.setFont(FontManager.getRunescapeSmallFont());
		hideBox.setFocusPainted(false);
		hideBox.addActionListener(e ->
		{
			hideRanked = hideBox.isSelected();
			SwingUtilities.invokeLater(this::refreshOrder);
		});

		controlRow.add(sortButton, BorderLayout.WEST);
		controlRow.add(hideBox, BorderLayout.CENTER);
		header.add(controlRow);

		return header;
	}

	private JPanel buildEmptyCard()
	{
		JPanel card = new JPanel(new GridBagLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel inner = new JPanel();
		inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
		inner.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel hint = new JLabel("Click Load to fetch your HiScores data.");
		hint.setForeground(Color.GRAY);
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setAlignmentX(Component.CENTER_ALIGNMENT);

		loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		inner.add(hint);
		inner.add(Box.createVerticalStrut(10));
		inner.add(loadButton);

		card.add(inner);
		return card;
	}

	private JPanel buildSkillsPanel(SkillIconManager skillIconManager, MinHiscoresPlugin plugin)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		boolean alternate = false;
		for (Skill skill : Skill.values())
		{
			SkillRow row = new SkillRow(skill, alternate, skillIconManager, plugin);
			rows.put(skill, row);
			alternate = !alternate;
		}

		return panel;
	}

	// Rebuilds the skillsPanel order, inserting the Overall row at the correct
	// sort position (or at the top in DEFAULT mode).
	private void refreshOrder()
	{
		boolean showOverall = !(hideRanked && plugin.overallRank > 0);

		List<Skill> skillList;
		if (sortState == SortState.DEFAULT)
		{
			skillList = Arrays.stream(Skill.values())
				.filter(rows::containsKey)
				.collect(Collectors.toList());
		}
		else
		{
			skillList = Arrays.stream(Skill.values())
				.filter(rows::containsKey)
				.sorted(this::compareSkillsDescending)
				.collect(Collectors.toList());
			if (sortState == SortState.ASCENDING)
			{
				java.util.Collections.reverse(skillList);
			}
		}

		if (hideRanked)
		{
			skillList.removeIf(s -> plugin.playerRank.getOrDefault(s, -1) > 0);
		}

		skillsPanel.removeAll();
		boolean alternate = false;

		if (sortState == SortState.DEFAULT)
		{
			// Overall pinned at top in default view
			if (showOverall)
			{
				overallRow.updateBackground(alternate);
				skillsPanel.add(overallRow);
				alternate = !alternate;
			}
			for (Skill skill : skillList)
			{
				SkillRow row = rows.get(skill);
				row.updateBackground(alternate);
				skillsPanel.add(row);
				alternate = !alternate;
			}
		}
		else
		{
			// Find where overall sorts into the list
			int insertIdx = skillList.size();
			if (showOverall)
			{
				long oXp = Math.max(0L, plugin.overallXp);
				int oBucket = sortBucket(oXp, plugin.overallRank, plugin.overallMinXp);

				for (int i = 0; i < skillList.size(); i++)
				{
					Skill s = skillList.get(i);
					int sXp = plugin.currentXp.getOrDefault(s, 0);
					int sRank = plugin.playerRank.getOrDefault(s, -1);
					long sMinXp = plugin.minimumXp.getOrDefault(s, -1L);
					int sBucket = sortBucket(sXp, sRank, sMinXp);

					int cmp;
					if (oBucket != sBucket)
					{
						cmp = Integer.compare(oBucket, sBucket);
					}
					else
					{
						switch (oBucket)
						{
							case 0: // more needed comes first in descending
								cmp = Long.compare(sMinXp - sXp, plugin.overallMinXp - oXp);
								break;
							case 1: // worst rank (higher number) comes first in descending
								cmp = Integer.compare(sRank, plugin.overallRank);
								break;
							default:
								cmp = 0;
						}
					}

					// DESCENDING: overall before skill when cmp <= 0 (overall's key >= skill's key)
					// ASCENDING (reversed list): overall before skill when cmp >= 0
					boolean overallFirst = sortState == SortState.DESCENDING ? cmp <= 0 : cmp >= 0;
					if (overallFirst)
					{
						insertIdx = i;
						break;
					}
				}
			}

			for (int i = 0; i <= skillList.size(); i++)
			{
				if (showOverall && i == insertIdx)
				{
					overallRow.updateBackground(alternate);
					skillsPanel.add(overallRow);
					alternate = !alternate;
				}
				if (i < skillList.size())
				{
					SkillRow row = rows.get(skillList.get(i));
					row.updateBackground(alternate);
					skillsPanel.add(row);
					alternate = !alternate;
				}
			}
		}

		skillsPanel.revalidate();
		skillsPanel.repaint();
	}

	private int compareSkillsDescending(Skill a, Skill b)
	{
		int xpA = plugin.currentXp.getOrDefault(a, 0);
		int xpB = plugin.currentXp.getOrDefault(b, 0);
		int rankA = plugin.playerRank.getOrDefault(a, -1);
		int rankB = plugin.playerRank.getOrDefault(b, -1);
		long minXpA = plugin.minimumXp.getOrDefault(a, -1L);
		long minXpB = plugin.minimumXp.getOrDefault(b, -1L);

		int bucketA = sortBucket(xpA, rankA, minXpA);
		int bucketB = sortBucket(xpB, rankB, minXpB);

		if (bucketA != bucketB)
		{
			return Integer.compare(bucketA, bucketB);
		}
		switch (bucketA)
		{
			case 0: return Long.compare(minXpB - xpB, minXpA - xpA); // most needed first
			case 1: return Integer.compare(rankB, rankA);             // worst rank first
			default: return 0;
		}
	}

	// xp is long to handle overall total XP which can exceed Integer.MAX_VALUE.
	// 0 = needs XP, 1 = ranked, 2 = pending update, 3 = <200K ranked, 4 = fetching/no data
	private static int sortBucket(long xp, int rank, long minXp)
	{
		if (minXp > 0 && minXp - xp > 0) return 0;
		if (rank > 0) return 1;
		if (minXp > 0) return 2;
		if (minXp == 0) return 3;
		return 4;
	}

	// --- public API called by plugin ---

	void onLoggedIn(String username)
	{
		SwingUtilities.invokeLater(() ->
		{
			playerNameLabel.setText(username != null ? username : "Loading…");
			playerNameLabel.setForeground(username != null ? Color.WHITE : Color.GRAY);
			loadButton.setEnabled(true);
			refreshButton.setEnabled(true);
		});
	}

	void onLoggedOut()
	{
		SwingUtilities.invokeLater(() ->
		{
			playerNameLabel.setText("Not logged in");
			playerNameLabel.setForeground(Color.GRAY);
			loadButton.setEnabled(false);
			refreshButton.setEnabled(false);
			contentPanel.removeAll();
			contentPanel.add(emptyCard, BorderLayout.CENTER);
			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	void showDataCard()
	{
		SwingUtilities.invokeLater(() ->
		{
			refreshOrder();
			contentPanel.removeAll();
			contentPanel.add(skillsPanel, BorderLayout.CENTER);
			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	void updateAll()
	{
		SwingUtilities.invokeLater(() ->
		{
			overallRow.update(plugin);
			rows.values().forEach(r -> r.update(plugin));
			refreshOrder();
		});
	}

	void updateRow(Skill skill)
	{
		SwingUtilities.invokeLater(() ->
		{
			SkillRow row = rows.get(skill);
			if (row != null)
			{
				row.update(plugin);
				if (sortState != SortState.DEFAULT)
				{
					refreshOrder();
				}
			}
		});
	}

	void updateOverall()
	{
		SwingUtilities.invokeLater(() ->
		{
			overallRow.update(plugin);
			refreshOrder();
		});
	}

	// --- overall row ---

	private static class OverallRow extends JPanel
	{
		private final JLabel mainLabel;
		private final JLabel subLabel;
		private final JPanel center;

		OverallRow()
		{
			setLayout(new BorderLayout(6, 0));
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			setBorder(new CompoundBorder(
				new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
				new EmptyBorder(5, 8, 5, 8)
			));

			JLabel iconLabel = new JLabel(new ImageIcon(buildBarIcon()));
			iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
			iconLabel.setPreferredSize(new Dimension(32, 32));
			add(iconLabel, BorderLayout.WEST);

			center = new JPanel(new GridLayout(2, 1, 0, 1));
			center.setBackground(ColorScheme.DARK_GRAY_COLOR);

			mainLabel = new JLabel("—");
			mainLabel.setForeground(Color.GRAY);
			mainLabel.setFont(FontManager.getRunescapeFont());

			subLabel = new JLabel("—");
			subLabel.setForeground(Color.GRAY);
			subLabel.setFont(FontManager.getRunescapeSmallFont());

			center.add(mainLabel);
			center.add(subLabel);
			add(center, BorderLayout.CENTER);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		void updateBackground(boolean alternate)
		{
			Color bg = alternate ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR;
			setBackground(bg);
			center.setBackground(bg);
		}

		void update(MinHiscoresPlugin plugin)
		{
			long xp = Math.max(0L, plugin.overallXp);
			long minXp = plugin.overallMinXp;
			int rank = plugin.overallRank;

			subLabel.setText(xp > 0 ? XP_FORMAT.format(xp) + " total xp" : "—");
			subLabel.setForeground(Color.GRAY);

			if (rank > 0)
			{
				mainLabel.setText("Rank #" + XP_FORMAT.format(rank));
				mainLabel.setForeground(ON_COLOR);
			}
			else if (minXp == 0)
			{
				mainLabel.setText("< 2M players ranked");
				mainLabel.setForeground(Color.GRAY);
			}
			else if (minXp < 0)
			{
				mainLabel.setText("Fetching...");
				mainLabel.setForeground(Color.GRAY);
			}
			else
			{
				long needed = minXp - xp;
				if (needed <= 0)
				{
					mainLabel.setText("Pending update");
					mainLabel.setForeground(WARN_COLOR);
				}
				else
				{
					mainLabel.setText(XP_FORMAT.format(needed) + " xp to go");
					mainLabel.setForeground(OFF_COLOR);
				}
			}
		}

		// Draws a simple three-bar chart icon representing the skills/stats tab.
		private static BufferedImage buildBarIcon()
		{
			BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			// Red bar (tallest)
			g.setColor(new Color(200, 50, 50));
			g.fillRect(0, 2, 4, 14);
			// Green bar (medium)
			g.setColor(new Color(50, 180, 50));
			g.fillRect(6, 5, 4, 11);
			// Blue bar (shortest)
			g.setColor(new Color(50, 100, 220));
			g.fillRect(12, 8, 4, 8);
			g.dispose();
			return img;
		}
	}

	// --- skill row ---

	private static class SkillRow extends JPanel
	{
		private final Skill skill;
		private final JLabel mainLabel;
		private final JLabel currentXpLabel;
		private final JPanel center;
		private final JButton trackButton;

		SkillRow(Skill skill, boolean alternate, SkillIconManager iconManager, MinHiscoresPlugin plugin)
		{
			this.skill = skill;
			setLayout(new BorderLayout(6, 0));
			Color bg = alternate ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR;
			setBackground(bg);
			setBorder(new CompoundBorder(
				new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
				new EmptyBorder(5, 8, 5, 8)
			));

			add(buildIconLabel(iconManager, skill), BorderLayout.WEST);

			center = new JPanel(new GridLayout(2, 1, 0, 1));
			center.setBackground(bg);

			mainLabel = new JLabel("—");
			mainLabel.setForeground(Color.GRAY);
			mainLabel.setFont(FontManager.getRunescapeFont());

			currentXpLabel = new JLabel("—");
			currentXpLabel.setForeground(Color.GRAY);
			currentXpLabel.setFont(FontManager.getRunescapeSmallFont());

			center.add(mainLabel);
			center.add(currentXpLabel);
			add(center, BorderLayout.CENTER);

			trackButton = new JButton("Track");
			trackButton.setFont(FontManager.getRunescapeSmallFont());
			trackButton.setFocusPainted(false);
			trackButton.setEnabled(false);
			trackButton.setVisible(false);
			trackButton.setToolTipText("Set as XP Tracker goal");
			trackButton.addActionListener(e ->
			{
				plugin.setXpGoal(skill);
				trackButton.setText("Done!");
				trackButton.setEnabled(false);
				Timer timer = new Timer(1500, ev ->
				{
					trackButton.setText("Track");
					trackButton.setEnabled(true);
				});
				timer.setRepeats(false);
				timer.start();
			});
			add(trackButton, BorderLayout.EAST);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		void updateBackground(boolean alternate)
		{
			Color bg = alternate ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR;
			setBackground(bg);
			center.setBackground(bg);
		}

		private static JLabel buildIconLabel(SkillIconManager iconManager, Skill skill)
		{
			JLabel label = new JLabel();
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setPreferredSize(new Dimension(32, 32));
			label.setOpaque(false);

			BufferedImage img = iconManager.getSkillImage(skill, false);
			if (img != null)
			{
				label.setIcon(new ImageIcon(img));
			}
			else
			{
				label.setText(skill.getName().substring(0, Math.min(3, skill.getName().length())));
				label.setForeground(Color.WHITE);
				label.setFont(FontManager.getRunescapeSmallFont());
			}
			return label;
		}

		void update(MinHiscoresPlugin plugin)
		{
			int xp = plugin.currentXp.getOrDefault(skill, -1);
			long minXp = plugin.minimumXp.getOrDefault(skill, -1L);
			int rank = plugin.playerRank.getOrDefault(skill, -1);

			if (xp < 0)
			{
				mainLabel.setText("—");
				mainLabel.setForeground(Color.GRAY);
				currentXpLabel.setText("—");
				currentXpLabel.setForeground(Color.GRAY);
				trackButton.setVisible(false);
				return;
			}

			currentXpLabel.setText(XP_FORMAT.format(xp) + " current xp");
			currentXpLabel.setForeground(Color.GRAY);

			if (rank > 0)
			{
				mainLabel.setText("Rank #" + XP_FORMAT.format(rank));
				mainLabel.setForeground(ON_COLOR);
				trackButton.setVisible(false);
			}
			else if (minXp == 0)
			{
				mainLabel.setText("< 2M players ranked");
				mainLabel.setForeground(Color.GRAY);
				trackButton.setVisible(false);
			}
			else if (minXp < 0)
			{
				mainLabel.setText("Fetching...");
				mainLabel.setForeground(Color.GRAY);
				trackButton.setVisible(false);
			}
			else
			{
				long needed = minXp - xp;
				if (needed <= 0)
				{
					mainLabel.setText("Pending update");
					mainLabel.setForeground(WARN_COLOR);
					trackButton.setVisible(false);
				}
				else
				{
					mainLabel.setText(XP_FORMAT.format(needed) + " xp to go");
					mainLabel.setForeground(OFF_COLOR);
					trackButton.setVisible(true);
					trackButton.setEnabled(true);
				}
			}
		}
	}
}
