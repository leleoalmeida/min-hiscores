package com.minhiscores;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Am I There Yet?",
	description = "Shows how much XP you need to appear on the HiScores for each skill",
	tags = {"hiscores", "xp", "goals", "skills", "tracker"}
)
public class MinHiscoresPlugin extends Plugin
{
	// Maps net.runelite.api.Skill → net.runelite.client.hiscore.HiscoreSkill by name.
	// Handles the RUNECRAFTING→RUNECRAFT mismatch as a fallback.
	private static final Map<Skill, HiscoreSkill> SKILL_TO_HISCORE;

	// Maps Skill → {startGoalVarpId, endGoalVarpId} using the same varp IDs as XpTrackerPlugin.
	private static final Map<Skill, int[]> SKILL_GOAL_VARPS;

	static
	{
		SKILL_TO_HISCORE = new EnumMap<>(Skill.class);
		for (Skill skill : Skill.values())
		{
			String name = skill.getName().toLowerCase();
			for (HiscoreSkill hs : HiscoreSkill.values())
			{
				if (hs.getName().toLowerCase().equals(name))
				{
					SKILL_TO_HISCORE.put(skill, hs);
					break;
				}
			}
		}

		SKILL_GOAL_VARPS = new EnumMap<>(Skill.class);
		SKILL_GOAL_VARPS.put(Skill.ATTACK,       new int[]{VarPlayerID.XPDROPS_ATTACK_START,       VarPlayerID.XPDROPS_ATTACK_END});
		SKILL_GOAL_VARPS.put(Skill.STRENGTH,     new int[]{VarPlayerID.XPDROPS_STRENGTH_START,     VarPlayerID.XPDROPS_STRENGTH_END});
		SKILL_GOAL_VARPS.put(Skill.DEFENCE,      new int[]{VarPlayerID.XPDROPS_DEFENCE_START,      VarPlayerID.XPDROPS_DEFENCE_END});
		SKILL_GOAL_VARPS.put(Skill.RANGED,       new int[]{VarPlayerID.XPDROPS_RANGED_START,       VarPlayerID.XPDROPS_RANGED_END});
		SKILL_GOAL_VARPS.put(Skill.PRAYER,       new int[]{VarPlayerID.XPDROPS_PRAYER_START,       VarPlayerID.XPDROPS_PRAYER_END});
		SKILL_GOAL_VARPS.put(Skill.MAGIC,        new int[]{VarPlayerID.XPDROPS_MAGIC_START,        VarPlayerID.XPDROPS_MAGIC_END});
		SKILL_GOAL_VARPS.put(Skill.RUNECRAFT,    new int[]{VarPlayerID.XPDROPS_RUNECRAFT_START,    VarPlayerID.XPDROPS_RUNECRAFT_END});
		SKILL_GOAL_VARPS.put(Skill.HITPOINTS,    new int[]{VarPlayerID.XPDROPS_HITPOINTS_START,    VarPlayerID.XPDROPS_HITPOINTS_END});
		SKILL_GOAL_VARPS.put(Skill.AGILITY,      new int[]{VarPlayerID.XPDROPS_AGILITY_START,      VarPlayerID.XPDROPS_AGILITY_END});
		SKILL_GOAL_VARPS.put(Skill.HERBLORE,     new int[]{VarPlayerID.XPDROPS_HERBLORE_START,     VarPlayerID.XPDROPS_HERBLORE_END});
		SKILL_GOAL_VARPS.put(Skill.THIEVING,     new int[]{VarPlayerID.XPDROPS_THIEVING_START,     VarPlayerID.XPDROPS_THIEVING_END});
		SKILL_GOAL_VARPS.put(Skill.CRAFTING,     new int[]{VarPlayerID.XPDROPS_CRAFTING_START,     VarPlayerID.XPDROPS_CRAFTING_END});
		SKILL_GOAL_VARPS.put(Skill.MINING,       new int[]{VarPlayerID.XPDROPS_MINING_START,       VarPlayerID.XPDROPS_MINING_END});
		SKILL_GOAL_VARPS.put(Skill.SMITHING,     new int[]{VarPlayerID.XPDROPS_SMITHING_START,     VarPlayerID.XPDROPS_SMITHING_END});
		SKILL_GOAL_VARPS.put(Skill.FISHING,      new int[]{VarPlayerID.XPDROPS_FISHING_START,      VarPlayerID.XPDROPS_FISHING_END});
		SKILL_GOAL_VARPS.put(Skill.COOKING,      new int[]{VarPlayerID.XPDROPS_COOKING_START,      VarPlayerID.XPDROPS_COOKING_END});
		SKILL_GOAL_VARPS.put(Skill.FIREMAKING,   new int[]{VarPlayerID.XPDROPS_FIREMAKING_START,   VarPlayerID.XPDROPS_FIREMAKING_END});
		SKILL_GOAL_VARPS.put(Skill.WOODCUTTING,  new int[]{VarPlayerID.XPDROPS_WOODCUTTING_START,  VarPlayerID.XPDROPS_WOODCUTTING_END});
		SKILL_GOAL_VARPS.put(Skill.FLETCHING,    new int[]{VarPlayerID.XPDROPS_FLETCHING_START,    VarPlayerID.XPDROPS_FLETCHING_END});
		SKILL_GOAL_VARPS.put(Skill.SLAYER,       new int[]{VarPlayerID.XPDROPS_SLAYER_START,       VarPlayerID.XPDROPS_SLAYER_END});
		SKILL_GOAL_VARPS.put(Skill.FARMING,      new int[]{VarPlayerID.XPDROPS_FARMING_START,      VarPlayerID.XPDROPS_FARMING_END});
		SKILL_GOAL_VARPS.put(Skill.CONSTRUCTION, new int[]{VarPlayerID.XPDROPS_CONSTRUCTION_START, VarPlayerID.XPDROPS_CONSTRUCTION_END});
		SKILL_GOAL_VARPS.put(Skill.HUNTER,       new int[]{VarPlayerID.XPDROPS_HUNTER_START,       VarPlayerID.XPDROPS_HUNTER_END});
		SKILL_GOAL_VARPS.put(Skill.SAILING,      new int[]{VarPlayerID.XPDROPS_SAILING_START,      VarPlayerID.XPDROPS_SAILING_END});
	}

	// The rank we use as the XP target for unranked skills.
	// URL: secure.runescape.com/m=<subdomain>/overall?table=N&page=P
	// Page is 1-indexed, 25 rows/page. Page 8000 = ranks 199,976–200,000.
	private static final int TARGET_RANK = 2_000_000;
	private static final int TARGET_PAGE = TARGET_RANK / 25; // = 80000

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OkHttpClient okHttpClient;
	@Inject private HiscoreClient hiscoreClient;
	@Inject private ClientToolbar clientToolbar;
	@Inject private SkillIconManager skillIconManager;
	@Inject private MinHiscoresConfig config;

	final Map<Skill, Integer> currentXp = new EnumMap<>(Skill.class);
	final Map<Skill, Integer> playerRank = new EnumMap<>(Skill.class);
	final Map<Skill, Long> minimumXp = new EnumMap<>(Skill.class);
	int overallRank = -1;
	long overallXp = -1;
	long overallMinXp = -1L;

	private MinHiscoresPanel panel;
	private NavigationButton navButton;

	// Whether we have already enabled the panel for the current login session.
	private boolean loggedInNotified;

	@Override
	protected void startUp()
	{
		panel = new MinHiscoresPanel(this, skillIconManager);
		navButton = NavigationButton.builder()
			.tooltip("Am I There Yet?")
			.icon(buildIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			notifyLoggedIn();
		}
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		currentXp.clear();
		playerRank.clear();
		minimumXp.clear();
	}

	@Provides
	MinHiscoresConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MinHiscoresConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			notifyLoggedIn();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			loggedInNotified = false;
			playerRank.clear();
			currentXp.clear();
			minimumXp.clear();
			overallRank = -1;
			overallXp = -1;
			overallMinXp = -1L;
			if (panel != null)
			{
				panel.onLoggedOut();
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// StatChanged fires reliably once the player is fully loaded, so use it
		// as a fallback to enable the panel if the name wasn't available at LOGGED_IN time.
		if (!loggedInNotified)
		{
			notifyLoggedIn();
		}
		currentXp.put(event.getSkill(), event.getXp());
		if (panel != null)
		{
			panel.updateRow(event.getSkill());
		}
	}

	void setXpGoal(Skill skill)
	{
		int[] varps = SKILL_GOAL_VARPS.get(skill);
		if (varps == null)
		{
			log.debug("No varp mapping for skill {}", skill);
			return;
		}

		int curXp = currentXp.getOrDefault(skill, -1);
		long minXp = minimumXp.getOrDefault(skill, -1L);
		if (curXp < 0 || minXp <= 0 || minXp - curXp <= 0)
		{
			return;
		}

		int startGoalVarp = varps[0];
		int endGoalVarp = varps[1];

		clientThread.invokeLater(() ->
		{
			int[] varpArray = client.getVarps();
			varpArray[startGoalVarp] = curXp;
			varpArray[endGoalVarp] = (int) minXp;
			client.queueChangedVarp(startGoalVarp);
			client.queueChangedVarp(endGoalVarp);
		});
	}

	private void notifyLoggedIn()
	{
		if (panel == null)
		{
			return;
		}
		String name = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		panel.onLoggedIn(name);
		if (name != null)
		{
			loggedInNotified = true;
		}
	}

	void fetchData()
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}
		String username = client.getLocalPlayer().getName();
		HiscoreEndpoint endpoint = config.hiscoreMode().getEndpoint();

		hiscoreClient.lookupAsync(username, endpoint).whenCompleteAsync((result, ex) ->
		{
			if (ex != null)
			{
				log.debug("HiScores lookup failed", ex);
				return;
			}
			if (result == null)
			{
				log.debug("Player {} not found on hiscores ({})", username, endpoint);
				return;
			}

			applyHiscoreResult(result, endpoint);
		});
	}

	private void applyHiscoreResult(HiscoreResult result, HiscoreEndpoint endpoint)
	{
		net.runelite.client.hiscore.Skill overall = result.getSkill(HiscoreSkill.OVERALL);
		if (overall != null)
		{
			overallRank = overall.getRank();
			overallXp = overall.getExperience();
		}

		for (Skill skill : Skill.values())
		{
			HiscoreSkill hs = SKILL_TO_HISCORE.get(skill);
			if (hs == null)
			{
				continue;
			}
			net.runelite.client.hiscore.Skill sr = result.getSkill(hs);
			if (sr != null)
			{
				playerRank.put(skill, sr.getRank());
				currentXp.putIfAbsent(skill, (int) Math.max(0, sr.getExperience()));
			}
		}

		if (panel != null)
		{
			panel.showDataCard();
			panel.updateAll();
		}

		// Fetch minimum XP only for skills not yet on hiscores
		for (Skill skill : Skill.values())
		{
			if (playerRank.getOrDefault(skill, -1) <= 0 && !minimumXp.containsKey(skill))
			{
				HiscoreSkill hs = SKILL_TO_HISCORE.get(skill);
				if (hs != null)
				{
					fetchMinXpForSkill(endpoint, skill, hs.ordinal());
				}
			}
		}

		if (overallRank <= 0 && overallMinXp == -1L)
		{
			fetchOverallMinXp(endpoint);
		}
	}

	private void fetchOverallMinXp(HiscoreEndpoint endpoint)
	{
		String subdomain = extractSubdomain(endpoint.getHiscoreURL());
		HttpUrl url = HttpUrl.get("https://secure.runescape.com/m=" + subdomain + "/overall")
			.newBuilder()
			.addQueryParameter("table", "0")
			.addQueryParameter("page", String.valueOf(TARGET_PAGE))
			.build();

		log.debug("Fetching rank-{} overall XP (page={})", TARGET_RANK, TARGET_PAGE);

		Request request = new Request.Builder().url(url).build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Overall ranking request failed: {}", e.getMessage());
				overallMinXp = 0L;
				if (panel != null) panel.updateOverall();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					String body = response.body() != null ? response.body().string() : "";
					if (!response.isSuccessful())
					{
						log.debug("Overall ranking HTTP {}", response.code());
						overallMinXp = 0L;
						if (panel != null) panel.updateOverall();
						return;
					}

					long[] parsed = parseHiscoresPage(body);
					long firstRank = parsed[0];
					long xp = parsed[1];
					if (xp > 0 && firstRank > 0 && firstRank < TARGET_RANK / 2L)
					{
						log.debug("Overall: page clipped (firstRank={}), storing minXp=1", firstRank);
						overallMinXp = 1L;
					}
					else
					{
						overallMinXp = xp > 0 ? xp : 0L;
					}
					log.debug("Rank-{} overall XP: {}", TARGET_RANK, overallMinXp);
					if (panel != null) panel.updateOverall();
				}
			}
		});
	}

	// Extracts the subdomain segment (e.g. "hiscore_oldschool") from a URL like
	// https://services.runescape.com/m=hiscore_oldschool/index_lite.json
	private static String extractSubdomain(HttpUrl url)
	{
		String path = url.encodedPath(); // /m=hiscore_oldschool/index_lite.json
		int start = path.indexOf("m=") + 2;
		int end = path.indexOf('/', start);
		return end < 0 ? path.substring(start) : path.substring(start, end);
	}

	// Fetches the XP of the player at rank TARGET_RANK for the given skill.
	// Uses secure.runescape.com/m=<subdomain>/overall?table=N&page=P (HTML response).
	private void fetchMinXpForSkill(HiscoreEndpoint endpoint, Skill skill, int table)
	{
		String subdomain = extractSubdomain(endpoint.getHiscoreURL());
		HttpUrl url = HttpUrl.get("https://secure.runescape.com/m=" + subdomain + "/overall")
			.newBuilder()
			.addQueryParameter("table", String.valueOf(table))
			.addQueryParameter("page", String.valueOf(TARGET_PAGE))
			.build();

		log.debug("Fetching rank-{} XP for {} (table={}, page={})", TARGET_RANK, skill, table, TARGET_PAGE);

		Request request = new Request.Builder().url(url).build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Ranking request failed for {}: {}", skill, e.getMessage());
				markUnavailable(skill);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					String body = response.body() != null ? response.body().string() : "";
					if (!response.isSuccessful())
					{
						log.debug("Ranking HTTP {} for {}", response.code(), skill);
						markUnavailable(skill);
						return;
					}

					long[] parsed = parseHiscoresPage(body);
					long firstRank = parsed[0];
					long xp = parsed[1];
					if (xp <= 0)
					{
						int snip = Math.min(body.length(), 400);
						log.debug("{}: failed to parse XP ({}b). Snippet: [{}]", skill, body.length(), body.substring(0, snip));
						markUnavailable(skill);
						return;
					}

					// If the first rank on this page is well below TARGET_RANK the site
					// clipped us to an earlier page — the skill has fewer than 2M players.
					// Store 1 so the player knows any XP at all will get them ranked.
					if (firstRank > 0 && firstRank < TARGET_RANK / 2L)
					{
						log.debug("{}: page clipped (firstRank={}), storing minXp=1", skill, firstRank);
						minimumXp.put(skill, 1L);
					}
					else
					{
						log.debug("Rank-{} XP for {}: {}", TARGET_RANK, skill, xp);
						minimumXp.put(skill, xp);
					}
					if (panel != null)
					{
						panel.updateRow(skill);
					}
				}
			}
		});
	}

	private void markUnavailable(Skill skill)
	{
		minimumXp.put(skill, 0L);
		if (panel != null)
		{
			panel.updateRow(skill);
		}
	}

	// Parses a hiscores ranking page and returns [firstRank, lastXp].
	// firstRank: the rank of the first data row (lets callers detect page clipping).
	// lastXp: the XP of the last data row, which is closest to TARGET_RANK.
	// XP is always the last numeric <td>; rank is the first numeric <td> of the first row.
	private static long[] parseHiscoresPage(String html)
	{
		long firstRank = -1;
		long lastXp = -1;
		int pos = 0;
		while (true)
		{
			int trStart = html.indexOf("<tr", pos);
			if (trStart < 0) break;
			int trEnd = html.indexOf("</tr>", trStart);
			if (trEnd < 0) break;
			String row = html.substring(trStart, trEnd);
			if (!row.contains("<td"))
			{
				pos = trEnd + 5;
				continue;
			}

			long rowFirstNum = -1;
			long rowLastNum = -1;
			int tdPos = 0;
			while (true)
			{
				int tdStart = row.indexOf("<td", tdPos);
				if (tdStart < 0) break;
				int gt = row.indexOf('>', tdStart);
				if (gt < 0) break;
				int tdEnd = row.indexOf("</td>", gt);
				if (tdEnd < 0) break;
				String content = row.substring(gt + 1, tdEnd)
					.replaceAll("<[^>]+>", "")
					.replaceAll("[^0-9]", "");
				if (!content.isEmpty())
				{
					try
					{
						long val = Long.parseLong(content);
						if (rowFirstNum < 0) rowFirstNum = val;
						rowLastNum = val;
					}
					catch (NumberFormatException ignored) {}
				}
				tdPos = tdEnd + 5;
			}

			if (rowLastNum > 0)
			{
				if (firstRank < 0 && rowFirstNum > 0) firstRank = rowFirstNum;
				lastXp = rowLastNum;
			}
			pos = trEnd + 5;
		}
		return new long[]{firstRank, lastXp};
	}

	private static BufferedImage buildIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(255, 185, 0));
		g.fillOval(2, 2, 12, 12);
		g.setColor(new Color(180, 120, 0));
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(2, 2, 12, 12);
		g.dispose();
		return img;
	}
}
