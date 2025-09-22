package net.litetex.raidrestore;

import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.world.GameRules;


public final class RaidRestoreGameRules
{
	public static final GameRules.Key<GameRules.IntRule> RAIDER_BAD_OMEN_EFFECT_SEC =
		GameRules.register(
			"raiderBadOmenEffectSec",
			GameRules.Category.MOBS,
			GameRules.IntRule.create(
				100 * 60, 1, 1_000_000, FeatureSet.empty(), (server, rule) -> {
				}));
	
	public static void init()
	{
		// Just load the class and register the gamerule
	}
	
	private RaidRestoreGameRules()
	{
	}
}
