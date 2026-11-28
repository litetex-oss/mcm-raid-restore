package net.litetex.raidrestore;

import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;


public final class RaidRestoreGameRules
{
	public static final GameRule<Integer> RAIDER_BAD_OMEN_EFFECT_SEC =
		GameRules.registerInteger(
			"raider_bad_omen_effect_sec",
			GameRuleCategory.MOBS,
			100 * 60,
			1,
			1_000_000
		);
	
	public static void init()
	{
		// Just load the class and register the gamerule
	}
	
	private RaidRestoreGameRules()
	{
	}
}
