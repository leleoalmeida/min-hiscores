package com.minhiscores;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("min-hiscores")
public interface MinHiscoresConfig extends Config
{
	@ConfigItem(
		keyName = "hiscoreMode",
		name = "HiScores Mode",
		description = "Which HiScores table to compare against"
	)
	default HiscoreMode hiscoreMode()
	{
		return HiscoreMode.NORMAL;
	}
}
