package net.litetex.raidrestore.mixin;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;


@Mixin(Raid.class)
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:StaticVariableName", "java:S3008", "PMD.GodClass"})
public abstract class RaidTickMixin
{
	@Unique
	private static final long RAID_TIMEOUT_TICKS = 48000L;
	@Unique
	private static final int PRE_RAID_TICKS = 300;
	
	@Inject(
		method = "tick",
		at = @At("HEAD"),
		cancellable = true,
		// Increment order slightly to improve compatibility with Lithium
		// https://github.com/CaffeineMC/lithium-fabric/blob/mc1.21.1-0.13.0/src/main/java/me/jellysquid/mods/lithium/mixin/ai/raid/RaidMixin.java
		order = 1100)
	@SuppressWarnings({
		"deprecation",
		"java:S3776",
		"java:S6541",
		"checkstyle:MethodLength",
		"PMD.CognitiveComplexity",
		"PMD.CyclomaticComplexity",
		"PMD.NPathComplexity",
		"PMD.AvoidDeeplyNestedIfStmts"})
	public void tick(final ServerLevel serverWorld, final CallbackInfo ci)
	{
		if(this.isStopped())
		{
			ci.cancel();
			return;
		}
		
		// region Ongoing
		if(this.status == Raid.RaidStatus.ONGOING)
		{
			final boolean wasActive = this.active;
			this.active = serverWorld.hasChunkAt(this.center);
			if(serverWorld.getDifficulty() == Difficulty.PEACEFUL)
			{
				this.stop();
				ci.cancel();
				return;
			}
			
			if(wasActive != this.active)
			{
				this.raidEvent.setVisible(this.active);
			}
			
			if(!this.active)
			{
				ci.cancel();
				return;
			}
			
			if(!serverWorld.isVillage(this.center))
			{
				this.moveRaidCenterToNearbyVillageSection(serverWorld);
			}
			
			if(!serverWorld.isVillage(this.center))
			{
				if(this.groupsSpawned > 0)
				{
					this.status = Raid.RaidStatus.LOSS;
				}
				else
				{
					this.stop();
				}
			}
			
			this.ticksActive++;
			if(this.ticksActive >= RAID_TIMEOUT_TICKS)
			{
				this.stop();
				ci.cancel();
				return;
			}
			
			final int raiderCount = this.getTotalRaidersAlive();
			// region Pre-Raid
			if(raiderCount == 0 && this.hasMoreWaves())
			{
				if(this.raidCooldownTicks <= 0)
				{
					if(this.raidCooldownTicks == 0 && this.groupsSpawned > 0)
					{
						this.raidCooldownTicks = PRE_RAID_TICKS;
						this.raidEvent.setName(RAID_NAME_COMPONENT);
						ci.cancel();
						return;
					}
				}
				else
				{
					final boolean hasPreCalculatedRaiderSpawnLocation =
						this.waveSpawnPos.isPresent();
					boolean needNewRaiderSpawnLocation =
						!hasPreCalculatedRaiderSpawnLocation && this.raidCooldownTicks % 5 == 0;
					if(hasPreCalculatedRaiderSpawnLocation
						&& !serverWorld.isPositionEntityTicking(this.waveSpawnPos.get()))
					{
						needNewRaiderSpawnLocation = true;
					}
					
					if(needNewRaiderSpawnLocation)
					{
						int proximity = 0;
						if(this.raidCooldownTicks < 100)
						{
							proximity = 1;
						}
						else if(this.raidCooldownTicks < 40)
						{
							proximity = 2;
						}
						
						this.waveSpawnPos = this.getRaidersSpawnLocation(serverWorld, proximity);
					}
					
					if(this.raidCooldownTicks == PRE_RAID_TICKS || this.raidCooldownTicks % 20 == 0)
					{
						this.updatePlayers(serverWorld);
					}
					
					this.raidCooldownTicks--;
					this.raidEvent.setProgress(
						Mth.clamp((PRE_RAID_TICKS - this.raidCooldownTicks) / 300.0F, 0.0F, 1.0F));
				}
			}
			// endregion
			
			if(this.ticksActive % 20L == 0L)
			{
				this.updatePlayers(serverWorld);
				this.updateRaiders(serverWorld);
				this.raidEvent.setName(raiderCount > 0 && raiderCount <= 2
					? RAID_NAME_COMPONENT.copy()
					.append(" - ")
					.append(Component.translatable("event.minecraft.raid.raiders_remaining", raiderCount))
					: RAID_NAME_COMPONENT);
			}
			
			// region Spawn
			boolean playedRaidHorn = false;
			int proximity = 0;
			
			while(this.shouldSpawnGroup())
			{
				final BlockPos blockPos = this.waveSpawnPos.isPresent()
					? this.waveSpawnPos.get()
					: this.findRandomRaidersSpawnLocation(serverWorld, proximity, 20);
				if(blockPos != null)
				{
					this.started = true;
					this.spawnGroup(serverWorld, blockPos);
					if(!playedRaidHorn)
					{
						this.playSound(serverWorld, blockPos);
						playedRaidHorn = true;
					}
				}
				else
				{
					proximity++;
				}
				
				if(proximity > 3)
				{
					this.stop();
					break;
				}
			}
			// endregion
			// region Post-Raid
			if(this.isStarted() && !this.hasMoreWaves() && raiderCount == 0)
			{
				if(this.postRaidTicks < 40)
				{
					this.postRaidTicks++;
				}
				else
				{
					this.status = Raid.RaidStatus.VICTORY;
					
					for(final UUID uUID : this.heroesOfTheVillage)
					{
						final Entity entity = serverWorld.getPlayerByUUID(uUID);
						if(entity instanceof final LivingEntity livingEntity && !entity.isSpectator())
						{
							livingEntity.addEffect(new MobEffectInstance(
								MobEffects.HERO_OF_THE_VILLAGE,
								// Min x Sec x TPS
								40 * 60 * 20,
								this.raidOmenLevel - 1,
								false,
								false,
								true));
							if(livingEntity instanceof final ServerPlayer serverPlayerEntity)
							{
								serverPlayerEntity.awardStat(Stats.RAID_WIN);
								CriteriaTriggers.RAID_WIN.trigger(serverPlayerEntity);
							}
						}
					}
				}
			}
			// endregion
			
			this.setDirty(serverWorld);
		}
		// endregion
		// region Finished
		else if(this.isOver())
		{
			this.celebrationTicks++;
			if(this.celebrationTicks >= 600)
			{
				this.stop();
				ci.cancel();
				return;
			}
			
			if(this.celebrationTicks % 20 == 0)
			{
				this.updatePlayers(serverWorld);
				this.raidEvent.setVisible(true);
				if(this.isVictory())
				{
					this.raidEvent.setProgress(0.0F);
					this.raidEvent.setName(RAID_BAR_VICTORY_COMPONENT);
				}
				else
				{
					this.raidEvent.setName(RAID_BAR_DEFEAT_COMPONENT);
				}
			}
		}
		// endregion
		
		ci.cancel();
	}
	
	@Unique
	protected Optional<BlockPos> getRaidersSpawnLocation(
		final ServerLevel serverWorld,
		final int proximity)
	{
		for(int i = 0; i < 3; i++)
		{
			final BlockPos blockPos = this.findRandomRaidersSpawnLocation(serverWorld, proximity, 1);
			if(blockPos != null)
			{
				return Optional.of(blockPos);
			}
		}
		
		return Optional.empty();
	}
	
	@Unique
	@Nullable
	@SuppressWarnings("deprecation")
	protected BlockPos findRandomRaidersSpawnLocation(
		final ServerLevel serverWorld,
		final int proximity,
		final int tries)
	{
		final int invertedProximity = proximity == 0 ? 2 : 2 - proximity;
		
		final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for(int j = 0; j < tries; j++)
		{
			final float f = serverWorld.random.nextFloat() * (float)(Math.PI * 2);
			
			final int x = this.center.getX()
				+ Mth.floor(Mth.cos(f) * 32.0F * invertedProximity)
				+ serverWorld.random.nextInt(5);
			final int z = this.center.getZ()
				+ Mth.floor(Mth.sin(f) * 32.0F * invertedProximity)
				+ serverWorld.random.nextInt(5);
			final int y = serverWorld.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
			
			mutable.set(x, y, z);
			
			if(!serverWorld.isVillage(mutable) || proximity >= 2)
			{
				final int offset = 10;
				if(serverWorld.hasChunksAt(
					mutable.getX() - offset,
					mutable.getZ() - offset,
					mutable.getX() + offset,
					mutable.getZ() + offset)
					&& serverWorld.isPositionEntityTicking(mutable)
					&& (
					SpawnPlacements.getPlacementType(EntityType.RAVAGER)
						.isSpawnPositionOk(serverWorld, mutable, EntityType.RAVAGER)
						|| serverWorld.getBlockState(mutable.below()).is(Blocks.SNOW)
						&& serverWorld.getBlockState(mutable).isAir()
				))
				{
					return mutable;
				}
			}
		}
		
		return null;
	}
	
	@Shadow
	@Final
	private static Component RAID_BAR_DEFEAT_COMPONENT;
	@Shadow
	@Final
	private static Component RAID_BAR_VICTORY_COMPONENT;
	@Shadow
	@Final
	private static Component RAID_NAME_COMPONENT;
	
	@Shadow
	private BlockPos center;
	
	@Shadow
	@Final
	private Set<UUID> heroesOfTheVillage;
	@Shadow
	private int postRaidTicks;
	
	@Shadow
	private boolean started;
	@Shadow
	private Optional<BlockPos> waveSpawnPos;
	
	@Shadow
	private boolean active;
	@Shadow
	private Raid.RaidStatus status;
	
	@Shadow
	private int raidOmenLevel;
	
	@Shadow
	@Final
	private ServerBossEvent raidEvent;
	
	@Shadow
	private int groupsSpawned;
	@Shadow
	private int celebrationTicks;
	
	@Shadow
	private int raidCooldownTicks;
	
	@Shadow
	private long ticksActive;
	
	@Shadow
	public abstract boolean isVictory();
	
	@Shadow
	protected abstract void setDirty(final ServerLevel serverWorld);
	
	@Shadow
	public abstract boolean isStarted();
	
	@Shadow
	protected abstract boolean shouldSpawnGroup();
	
	@Shadow
	protected abstract void updateRaiders(final ServerLevel serverWorld);
	
	@Shadow
	protected abstract void updatePlayers(final ServerLevel serverWorld);
	
	@Shadow
	protected abstract boolean hasMoreWaves();
	
	@Shadow
	public abstract int getTotalRaidersAlive();
	
	@Shadow
	protected abstract void moveRaidCenterToNearbyVillageSection(final ServerLevel serverWorld);
	
	@Shadow
	public abstract boolean isOver();
	
	@Shadow
	public abstract void stop();
	
	@Shadow
	public abstract boolean isStopped();
	
	@Shadow
	protected abstract void playSound(final ServerLevel serverWorld, final BlockPos pos);
	
	@Shadow
	protected abstract void spawnGroup(final ServerLevel serverWorld, final BlockPos pos);
}
