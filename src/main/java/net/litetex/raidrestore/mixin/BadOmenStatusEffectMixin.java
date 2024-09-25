package net.litetex.raidrestore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;


@Mixin(targets = "net.minecraft.entity.effect.BadOmenStatusEffect")
public abstract class BadOmenStatusEffectMixin
{
    @Inject(method = "applyUpdateEffect", at = @At("HEAD"), cancellable = true)
    protected void applyUpdateEffect(
        final ServerWorld world,
        final LivingEntity entity,
        final int amplifier,
        final CallbackInfoReturnable<Boolean> cir)
    {
        if(entity instanceof final ServerPlayerEntity player && !player.isSpectator())
        {
            final BlockPos playerBlockPos = player.getBlockPos();
            if(world.getDifficulty() != Difficulty.PEACEFUL
                && world.isNearOccupiedPointOfInterest(playerBlockPos))
            {
                world.getRaidManager().startRaid(player, playerBlockPos);
            }
            cir.setReturnValue(true);
        }
        cir.setReturnValue(true);
    }
}
