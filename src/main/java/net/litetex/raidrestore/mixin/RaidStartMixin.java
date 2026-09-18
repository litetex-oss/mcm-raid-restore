package net.litetex.raidrestore.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Raid.class)
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:StaticVariableName"})
public abstract class RaidStartMixin
{
	@Inject(method = "absorbRaidOmen", at = @At("HEAD"), cancellable = true)
	protected void start(
		final ServerPlayer player,
		final CallbackInfoReturnable<Boolean> cir)
	{
		final MobEffectInstance statusEffect = player.getEffect(MobEffects.BAD_OMEN);
		if(statusEffect != null)
		{
			this.raidOmenLevel = Mth.clamp(
				this.raidOmenLevel + statusEffect.getAmplifier() + 1,
				0,
				this.getMaxRaidOmenLevel());
		}
		player.removeEffect(MobEffects.BAD_OMEN);
		
		cir.setReturnValue(true);
	}
	
	@Shadow
	private int raidOmenLevel;
	
	@Shadow
	public abstract int getMaxRaidOmenLevel();
}
