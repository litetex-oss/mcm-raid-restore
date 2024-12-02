package net.litetex.raidrestore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.raid.Raid;


@Mixin(Raid.class)
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:StaticVariableName"})
public abstract class RaidStartMixin
{
	@Inject(method = "start", at = @At("HEAD"), cancellable = true)
	protected void start(
		final ServerPlayerEntity player,
		final CallbackInfoReturnable<Boolean> cir)
	{
		final StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.BAD_OMEN);
		if(statusEffect != null)
		{
			this.badOmenLevel = MathHelper.clamp(
				this.badOmenLevel + statusEffect.getAmplifier() + 1,
				0,
				this.getMaxAcceptableBadOmenLevel());
		}
		player.removeStatusEffect(StatusEffects.BAD_OMEN);
		
		cir.setReturnValue(true);
	}
	
	@Shadow
	private int badOmenLevel;
	
	@Shadow
	public abstract int getMaxAcceptableBadOmenLevel();
}
