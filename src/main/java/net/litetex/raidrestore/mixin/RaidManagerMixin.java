package net.litetex.raidrestore.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;


@Mixin(Raids.class)
public abstract class RaidManagerMixin
{
	@SuppressWarnings({"UnreachableCode", "java:S125", "checkstyle:MagicNumber"})
	@Inject(method = "createOrExtendRaid", at = @At("HEAD"), cancellable = true)
	protected void startRaid(
		final ServerPlayer player,
		final BlockPos pos,
		final CallbackInfoReturnable<Raid> cir)
	{
		final ServerLevel serverWorld = player.level();
		if(player.isSpectator()
			|| !serverWorld.getGameRules().get(GameRules.RAIDS)
			|| !serverWorld.environmentAttributes().getValue(EnvironmentAttributes.CAN_START_RAID, pos))
		{
			cir.setReturnValue(null);
			return;
		}
		
		int i = 0;
		Vec3 vec3d = Vec3.ZERO;
		for(final PoiRecord pointOfInterest : serverWorld.getPoiManager()
			.getInRange(
				poiType -> poiType.is(PoiTypeTags.VILLAGE),
				pos,
				64,
				PoiManager.Occupancy.IS_OCCUPIED)
			.toList())
		{
			final BlockPos blockPos = pointOfInterest.getPos();
			vec3d = vec3d.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			i++;
		}
		final Raid raid = this.getOrCreateRaid(
			serverWorld,
			i > 0 ? BlockPos.containing(vec3d.scale(1.0 / i)) : pos);
		
		boolean startRaid = false;
		if(!raid.isStarted())
		{
			if(!this.raidMap.containsValue(raid))
			{
				this.raidMap.put(this.getUniqueId(), raid);
			}
			startRaid = true;
		}
		else if(raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel())
		{
			startRaid = true;
		}
		else
		{
			player.removeEffect(MobEffects.BAD_OMEN);
		}
		
		if(startRaid)
		{
			// More like "prepareStart"...
			raid.absorbRaidOmen(player);
			
			if(!raid.hasFirstWaveSpawned())
			{
				player.awardStat(Stats.RAID_TRIGGER);
				// Buggy doesn't seem to be triggered correctly and has no effect
				// Looks like a bug in AbstractCriterion (progressions is empty)
				// Criteria.VOLUNTARY_EXILE.trigger(player);
			}
		}
		((Raids)(Object)this).setDirty();
		cir.setReturnValue(raid);
	}
	
	@Shadow
	@Final
	private Int2ObjectMap<Raid> raidMap;
	
	@Shadow
	protected abstract int getUniqueId();
	
	@Shadow
	protected abstract Raid getOrCreateRaid(ServerLevel world, BlockPos pos);
}
