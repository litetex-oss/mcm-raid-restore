package net.litetex.raidrestore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    @SuppressWarnings({"UnreachableCode", "java:S3776", "checkstyle:MagicNumber"})
    protected void onDeath(final DamageSource damageSource, final CallbackInfo ci)
    {
        final RaiderEntity current = (RaiderEntity)(Object)this;
        if(current.getWorld() instanceof final ServerWorld serverWorld
            && current.isPatrolLeader()
            && this.getRaid() == null
            && serverWorld.getRaidAt(current.getBlockPos()) == null)
        {
            final ItemStack itemStack = current.getEquippedStack(EquipmentSlot.HEAD);
            if(!itemStack.isEmpty()
                && ItemStack.areEqual(
                itemStack,
                Raid.createOminousBanner(current.getRegistryManager()
                    .getOrThrow(RegistryKeys.BANNER_PATTERN))))
            {
                final Entity attacker = damageSource.getAttacker();
                PlayerEntity attackingPlayer = null;
                if(attacker instanceof final PlayerEntity playerEntity)
                {
                    attackingPlayer = playerEntity;
                }
                else if(attacker instanceof final WolfEntity wolfEntity
                    && wolfEntity.isTamed()
                    && wolfEntity.getOwner() instanceof final PlayerEntity wolfOwnerPlayerEntity)
                {
                    attackingPlayer = wolfOwnerPlayerEntity;
                }
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
                            // Min x Sec x TPS
                            100 * 60 * 20,
                            MathHelper.clamp(amplifier, 0, 4),
                            false,
                            false,
                            true));
                    }
                }
            }
        }
    }
}
