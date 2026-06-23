package com.minhiscores;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MinHiscoresPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MinHiscoresPlugin.class);
		RuneLite.main(args);
	}
}
