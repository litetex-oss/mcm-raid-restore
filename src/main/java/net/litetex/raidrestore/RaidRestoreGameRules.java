package net.litetex.raidrestore;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;


public final class RaidRestoreGameRules
{
	public static final GameRules.Key<GameRules.IntegerValue> RAIDER_BAD_OMEN_EFFECT_SEC =
		GameRules.register(
			"raiderBadOmenEffectSec",
			GameRules.Category.MOBS,
			GameRules.IntegerValue.create(
				100 * 60, 1, 1_000_000, FeatureFlagSet.of(), (server, rule) -> {
				}));
	
	public static void init()
	{
		// Just load the class and register the gamerule
	}
	
	private RaidRestoreGameRules()
	{
	}
}
