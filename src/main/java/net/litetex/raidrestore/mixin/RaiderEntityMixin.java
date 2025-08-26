package net.litetex.raidrestore.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.litetex.raidrestore.RaidRestoreGameRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.GameRules;


@Mixin(RaiderEntity.class)
public abstract class RaiderEntityMixin
{
	@Shadow
	public abstract Raid getRaid();
	
	@Inject(method = "onDeath",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/mob/PatrolEntity;onDeath(Lnet/minecraft/entity/damage/DamageSource;)V"))
	@SuppressWarnings({
		"UnreachableCode",
		"java:S3776",
		"javabugs:S6320",
		"checkstyle:MagicNumber",
		"PMD.CognitiveComplexity"})
	protected void onDeath(final DamageSource damageSource, final CallbackInfo ci)
	{
		final RaiderEntity current = (RaiderEntity)(Object)this;
		if(current.getEntityWorld() instanceof final ServerWorld serverWorld
			&& current.isPatrolLeader()
			&& this.getRaid() == null
			&& serverWorld.getRaidAt(current.getBlockPos()) == null)
		{
			final ItemStack itemStack = current.getEquippedStack(EquipmentSlot.HEAD);
			if(!itemStack.isEmpty()
				&& ItemStack.areEqual(
				itemStack,
				Raid.createOminousBanner(current.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))))
			{
				final PlayerEntity attackingPlayer = this.getAttackingPlayer(damageSource);
				if(attackingPlayer != null)
				{
					final StatusEffectInstance statusEffectInstance =
						attackingPlayer.getStatusEffect(StatusEffects.BAD_OMEN);
					int amplifier = 1;
					if(statusEffectInstance != null)
					{
						amplifier += statusEffectInstance.getAmplifier();
						attackingPlayer.removeStatusEffectInternal(StatusEffects.BAD_OMEN);
					}
					else
					{
						amplifier--;
					}
					if(!serverWorld.getGameRules().getBoolean(GameRules.DISABLE_RAIDS))
					{
						attackingPlayer.addStatusEffect(new StatusEffectInstance(
							StatusEffects.BAD_OMEN,
							serverWorld.getGameRules().getInt(RaidRestoreGameRules.RAIDER_BAD_OMEN_EFFECT_SEC) * 20,
							MathHelper.clamp(amplifier, 0, 4),
							false,
							false,
							true));
					}
				}
			}
		}
	}
	
	@Unique
	private @Nullable PlayerEntity getAttackingPlayer(final DamageSource damageSource)
	{
		final Entity attacker = damageSource.getAttacker();
		if(attacker instanceof final PlayerEntity playerEntity)
		{
			return playerEntity;
		}
		else if(attacker instanceof final WolfEntity wolfEntity
			&& wolfEntity.isTamed()
			&& wolfEntity.getOwner() instanceof final PlayerEntity wolfOwnerPlayerEntity)
		{
			return wolfOwnerPlayerEntity;
		}
		return null;
	}
}
