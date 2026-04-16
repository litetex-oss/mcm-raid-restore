package net.litetex.raidrestore.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.litetex.raidrestore.RaidRestoreGameRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;


@Mixin(Raider.class)
public abstract class RaiderEntityMixin
{
	@Shadow
	public abstract Raid getCurrentRaid();
	
	@Inject(method = "die",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/monster/PatrollingMonster;"
				+ "die(Lnet/minecraft/world/damagesource/DamageSource;)V"))
	@SuppressWarnings({
		"UnreachableCode",
		"java:S3776",
		"javabugs:S6320",
		"checkstyle:MagicNumber",
		"PMD.CognitiveComplexity",
		"PMD.AvoidDeeplyNestedIfStmts"})
	protected void onDeath(final DamageSource damageSource, final CallbackInfo ci)
	{
		final Raider current = (Raider)(Object)this;
		if(current.level() instanceof final ServerLevel serverWorld
			&& current.isPatrolLeader()
			&& this.getCurrentRaid() == null
			&& serverWorld.getRaidAt(current.blockPosition()) == null)
		{
			final ItemStack itemStack = current.getItemBySlot(EquipmentSlot.HEAD);
			if(!itemStack.isEmpty()
				&& ItemStack.matches(
				itemStack,
				Raid.getOminousBannerInstance(current.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN))))
			{
				final Player attackingPlayer = this.getAttackingPlayer(damageSource);
				if(attackingPlayer != null)
				{
					final MobEffectInstance statusEffectInstance =
						attackingPlayer.getEffect(MobEffects.BAD_OMEN);
					int amplifier = 1;
					if(statusEffectInstance != null)
					{
						amplifier += statusEffectInstance.getAmplifier();
						attackingPlayer.removeEffectNoUpdate(MobEffects.BAD_OMEN);
					}
					else
					{
						amplifier--;
					}
					if(serverWorld.getGameRules().get(GameRules.RAIDS))
					{
						attackingPlayer.addEffect(new MobEffectInstance(
							MobEffects.BAD_OMEN,
							serverWorld.getGameRules().get(RaidRestoreGameRules.RAIDER_BAD_OMEN_EFFECT_SEC) * 20,
							Mth.clamp(amplifier, 0, 4),
							false,
							false,
							true));
					}
				}
			}
		}
	}
	
	@Unique
	private @Nullable Player getAttackingPlayer(final DamageSource damageSource)
	{
		final Entity attacker = damageSource.getEntity();
		if(attacker instanceof final Player playerEntity)
		{
			return playerEntity;
		}
		else if(attacker instanceof final Wolf wolfEntity
			&& wolfEntity.isTame()
			&& wolfEntity.getOwner() instanceof final Player wolfOwnerPlayerEntity)
		{
			return wolfOwnerPlayerEntity;
		}
		return null;
	}
}
