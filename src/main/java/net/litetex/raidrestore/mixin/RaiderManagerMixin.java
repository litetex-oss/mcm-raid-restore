package net.litetex.raidrestore.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.GameRules;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;


@Mixin(RaidManager.class)
public abstract class RaiderManagerMixin
{
    @Shadow
    @Final
    private ServerWorld world;
    
    @Shadow
    protected abstract Raid getOrCreateRaid(ServerWorld world, BlockPos pos);
    
    @Shadow
    @Final
    private Map<Integer, Raid> raids;
    
    @SuppressWarnings({"UnreachableCode", "java:S125"})
    @Inject(method = "startRaid", at = @At("HEAD"), cancellable = true)
    protected void startRaid(
        final ServerPlayerEntity player,
        final BlockPos pos,
        final CallbackInfoReturnable<Raid> cir)
    {
        if(player.isSpectator()
            || this.world.getGameRules().getBoolean(GameRules.DISABLE_RAIDS)
            || !player.getWorld().getDimension().hasRaids())
        {
            cir.setReturnValue(null);
            return;
        }
        
        int i = 0;
        Vec3d vec3d = Vec3d.ZERO;
        for(final PointOfInterest pointOfInterest : this.world.getPointOfInterestStorage()
            .getInCircle(
                poiType -> poiType.isIn(PointOfInterestTypeTags.VILLAGE),
                pos,
                64,
                PointOfInterestStorage.OccupationStatus.IS_OCCUPIED)
            .toList())
        {
            final BlockPos blockPos = pointOfInterest.getPos();
            vec3d = vec3d.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            i++;
        }
        final Raid raid = this.getOrCreateRaid(
            player.getServerWorld(),
            i > 0 ? BlockPos.ofFloored(vec3d.multiply(1.0 / i)) : pos);
        
        boolean startRaid = false;
        if(!raid.hasStarted())
        {
            if(!this.raids.containsKey(raid.getRaidId()))
            {
                this.raids.put(raid.getRaidId(), raid);
            }
            startRaid = true;
        }
        else if(raid.getBadOmenLevel() < raid.getMaxAcceptableBadOmenLevel())
        {
            startRaid = true;
        }
        else
        {
            player.removeStatusEffect(StatusEffects.BAD_OMEN);
            // EntityStatus missing
            // player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, EntityStatuses.ADD_CLOUD_PARTICLES));
        }
        if(startRaid)
        {
            // More like "prepareStart"...
            raid.start(player);
            // EntityStatus missing
            // player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, EntityStatuses.ADD_CLOUD_PARTICLES));
            
            if(!raid.hasSpawned())
            {
                player.incrementStat(Stats.RAID_TRIGGER);
                // Buggy doesn't seem to be triggered correctly and has no effect
                // Looks like a bug in AbstractCriterion (progressions is empty)
                // Criteria.VOLUNTARY_EXILE.trigger(player);
            }
        }
        ((RaidManager)(Object)this).markDirty();
        cir.setReturnValue(raid);
    }
}
