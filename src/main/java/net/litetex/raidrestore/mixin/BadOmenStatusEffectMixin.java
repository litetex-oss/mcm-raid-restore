package net.litetex.raidrestore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;


@Mixin(targets = "net.minecraft.world.effect.BadOmenMobEffect")
public abstract class BadOmenStatusEffectMixin
{
	@Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
	protected void applyUpdateEffect(
		final ServerLevel level,
		final LivingEntity mob,
		final int amplification,
		final CallbackInfoReturnable<Boolean> cir)
	{
		if(mob instanceof final ServerPlayer player
			&& !player.isSpectator()
			&& level.getDifficulty() != Difficulty.PEACEFUL)
		{
			final BlockPos playerBlockPos = player.blockPosition();
			if(level.isVillage(playerBlockPos))
			{
				level.getRaids().createOrExtendRaid(player, playerBlockPos);
			}
			// Returning false here would remove the effect, however this is handled by Raid#start
			// (to determine the raid level)
		}
		cir.setReturnValue(true);
	}
}
