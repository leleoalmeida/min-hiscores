package com.minhiscores;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.hiscore.HiscoreEndpoint;

@Getter
@RequiredArgsConstructor
public enum HiscoreMode
{
	NORMAL("Normal", HiscoreEndpoint.NORMAL),
	IRONMAN("Ironman", HiscoreEndpoint.IRONMAN),
	HARDCORE_IRONMAN("Hardcore Ironman", HiscoreEndpoint.HARDCORE_IRONMAN),
	ULTIMATE_IRONMAN("Ultimate Ironman", HiscoreEndpoint.ULTIMATE_IRONMAN),
	SEASONAL("Leagues", HiscoreEndpoint.SEASONAL),
	DEADMAN("Deadman", HiscoreEndpoint.DEADMAN);

	private final String displayName;
	private final HiscoreEndpoint endpoint;

	@Override
	public String toString()
	{
		return displayName;
	}
}
