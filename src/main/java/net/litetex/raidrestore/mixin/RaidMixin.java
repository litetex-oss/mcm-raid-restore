package net.litetex.raidrestore.mixin;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raid.class)
public abstract class RaidMixin {

    @Shadow private int badOmenLevel;

    @Shadow public abstract int getMaxAcceptableBadOmenLevel();

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    protected void start(
            ServerPlayerEntity player,
            CallbackInfoReturnable<Boolean> cir)
    {
        final StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.BAD_OMEN);
        if(statusEffect != null) {
            this.badOmenLevel = MathHelper.clamp(
                    this.badOmenLevel + statusEffect.getAmplifier() + 1,
                    0,
                    this.getMaxAcceptableBadOmenLevel());
        }
        player.removeStatusEffect(StatusEffects.BAD_OMEN);

        cir.setReturnValue(true);
    }
}
