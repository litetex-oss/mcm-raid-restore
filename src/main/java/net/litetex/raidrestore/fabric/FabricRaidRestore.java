package net.litetex.raidrestore.fabric;

import net.fabricmc.api.ModInitializer;
import net.litetex.raidrestore.RaidRestoreGameRules;


public class FabricRaidRestore implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		RaidRestoreGameRules.init();
	}
}
