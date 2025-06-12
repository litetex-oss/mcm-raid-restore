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

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;


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
		"PMD.NPathComplexity"})
	public void tick(final ServerWorld serverWorld, final CallbackInfo ci)
	{
		if(this.hasStopped())
		{
			ci.cancel();
			return;
		}
		
		// region Ongoing
		if(this.status == Raid.Status.ONGOING)
		{
			final boolean wasActive = this.active;
			this.active = serverWorld.isChunkLoaded(this.center);
			if(serverWorld.getDifficulty() == Difficulty.PEACEFUL)
			{
				this.invalidate();
				ci.cancel();
				return;
			}
			
			if(wasActive != this.active)
			{
				this.bar.setVisible(this.active);
			}
			
			if(!this.active)
			{
				ci.cancel();
				return;
			}
			
			if(!serverWorld.isNearOccupiedPointOfInterest(this.center))
			{
				this.moveRaidCenter(serverWorld);
			}
			
			if(!serverWorld.isNearOccupiedPointOfInterest(this.center))
			{
				if(this.wavesSpawned > 0)
				{
					this.status = Raid.Status.LOSS;
				}
				else
				{
					this.invalidate();
				}
			}
			
			this.ticksActive++;
			if(this.ticksActive >= RAID_TIMEOUT_TICKS)
			{
				this.invalidate();
				ci.cancel();
				return;
			}
			
			final int raiderCount = this.getRaiderCount();
			// region Pre-Raid
			if(raiderCount == 0 && this.shouldSpawnMoreGroups())
			{
				if(this.preRaidTicks <= 0)
				{
					if(this.preRaidTicks == 0 && this.wavesSpawned > 0)
					{
						this.preRaidTicks = PRE_RAID_TICKS;
						this.bar.setName(EVENT_TEXT);
						ci.cancel();
						return;
					}
				}
				else
				{
					final boolean hasPreCalculatedRaiderSpawnLocation =
						this.preCalculatedRaidersSpawnLocation.isPresent();
					boolean needNewRaiderSpawnLocation =
						!hasPreCalculatedRaiderSpawnLocation && this.preRaidTicks % 5 == 0;
					if(hasPreCalculatedRaiderSpawnLocation
						&& !serverWorld.shouldTickEntityAt(this.preCalculatedRaidersSpawnLocation.get()))
					{
						needNewRaiderSpawnLocation = true;
					}
					
					if(needNewRaiderSpawnLocation)
					{
						int proximity = 0;
						if(this.preRaidTicks < 100)
						{
							proximity = 1;
						}
						else if(this.preRaidTicks < 40)
						{
							proximity = 2;
						}
						
						this.preCalculatedRaidersSpawnLocation = this.getRaidersSpawnLocation(serverWorld, proximity);
					}
					
					if(this.preRaidTicks == PRE_RAID_TICKS || this.preRaidTicks % 20 == 0)
					{
						this.updateBarToPlayers(serverWorld);
					}
					
					this.preRaidTicks--;
					this.bar.setPercent(MathHelper.clamp((PRE_RAID_TICKS - this.preRaidTicks) / 300.0F, 0.0F, 1.0F));
				}
			}
			// endregion
			
			if(this.ticksActive % 20L == 0L)
			{
				this.updateBarToPlayers(serverWorld);
				this.removeObsoleteRaiders(serverWorld);
				this.bar.setName(raiderCount > 0 && raiderCount <= 2
					? EVENT_TEXT.copy()
					.append(" - ")
					.append(Text.translatable("event.minecraft.raid.raiders_remaining", raiderCount))
					: EVENT_TEXT);
			}
			
			// region Spawn
			boolean playedRaidHorn = false;
			int proximity = 0;
			
			while(this.canSpawnRaiders())
			{
				final BlockPos blockPos = this.preCalculatedRaidersSpawnLocation.isPresent()
					? this.preCalculatedRaidersSpawnLocation.get()
					: this.findRandomRaidersSpawnLocation(serverWorld, proximity, 20);
				if(blockPos != null)
				{
					this.started = true;
					this.spawnNextWave(serverWorld, blockPos);
					if(!playedRaidHorn)
					{
						this.playRaidHorn(serverWorld, blockPos);
						playedRaidHorn = true;
					}
				}
				else
				{
					proximity++;
				}
				
				if(proximity > 3)
				{
					this.invalidate();
					break;
				}
			}
			// endregion
			// region Post-Raid
			if(this.hasStarted() && !this.shouldSpawnMoreGroups() && raiderCount == 0)
			{
				if(this.postRaidTicks < 40)
				{
					this.postRaidTicks++;
				}
				else
				{
					this.status = Raid.Status.VICTORY;
					
					for(final UUID uUID : this.heroesOfTheVillage)
					{
						final Entity entity = serverWorld.getEntity(uUID);
						if(entity instanceof final LivingEntity livingEntity && !entity.isSpectator())
						{
							livingEntity.addStatusEffect(new StatusEffectInstance(
								StatusEffects.HERO_OF_THE_VILLAGE,
								48000,
								this.raidOmenLevel - 1,
								false,
								false,
								true));
							if(livingEntity instanceof final ServerPlayerEntity serverPlayerEntity)
							{
								serverPlayerEntity.incrementStat(Stats.RAID_WIN);
								Criteria.HERO_OF_THE_VILLAGE.trigger(serverPlayerEntity);
							}
						}
					}
				}
			}
			// endregion
			
			this.markDirty(serverWorld);
		}
		// endregion
		// region Finished
		else if(this.isFinished())
		{
			this.finishCooldown++;
			if(this.finishCooldown >= 600)
			{
				this.invalidate();
				ci.cancel();
				return;
			}
			
			if(this.finishCooldown % 20 == 0)
			{
				this.updateBarToPlayers(serverWorld);
				this.bar.setVisible(true);
				if(this.hasWon())
				{
					this.bar.setPercent(0.0F);
					this.bar.setName(VICTORY_TITLE);
				}
				else
				{
					this.bar.setName(DEFEAT_TITLE);
				}
			}
		}
		// endregion
		
		ci.cancel();
	}
	
	@Unique
	protected Optional<BlockPos> getRaidersSpawnLocation(
		final ServerWorld serverWorld,
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
		final ServerWorld serverWorld,
		final int proximity,
		final int tries)
	{
		final int invertedProximity = proximity == 0 ? 2 : 2 - proximity;
		
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		for(int j = 0; j < tries; j++)
		{
			final float f = serverWorld.random.nextFloat() * (float)(Math.PI * 2);
			
			final int x = this.center.getX()
				+ MathHelper.floor(MathHelper.cos(f) * 32.0F * invertedProximity)
				+ serverWorld.random.nextInt(5);
			final int z = this.center.getZ()
				+ MathHelper.floor(MathHelper.sin(f) * 32.0F * invertedProximity)
				+ serverWorld.random.nextInt(5);
			final int y = serverWorld.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			
			mutable.set(x, y, z);
			
			if(!serverWorld.isNearOccupiedPointOfInterest(mutable) || proximity >= 2)
			{
				final int offset = 10;
				if(serverWorld.isRegionLoaded(
					mutable.getX() - offset,
					mutable.getZ() - offset,
					mutable.getX() + offset,
					mutable.getZ() + offset)
					&& serverWorld.shouldTickEntityAt(mutable)
					&& (
					SpawnRestriction.getLocation(EntityType.RAVAGER)
						.isSpawnPositionOk(serverWorld, mutable, EntityType.RAVAGER)
						|| serverWorld.getBlockState(mutable.down()).isOf(Blocks.SNOW)
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
	private static Text DEFEAT_TITLE;
	@Shadow
	@Final
	private static Text VICTORY_TITLE;
	@Shadow
	@Final
	private static Text EVENT_TEXT;
	
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
	private Optional<BlockPos> preCalculatedRaidersSpawnLocation;
	
	@Shadow
	private boolean active;
	@Shadow
	private Raid.Status status;
	
	@Shadow
	private int raidOmenLevel;
	
	@Shadow
	@Final
	private ServerBossBar bar;
	
	@Shadow
	private int wavesSpawned;
	@Shadow
	private int finishCooldown;
	
	@Shadow
	private int preRaidTicks;
	
	@Shadow
	private long ticksActive;
	
	@Shadow
	public abstract boolean hasWon();
	
	@Shadow
	protected abstract void markDirty(final ServerWorld serverWorld);
	
	@Shadow
	public abstract boolean hasStarted();
	
	@Shadow
	protected abstract boolean canSpawnRaiders();
	
	@Shadow
	protected abstract void removeObsoleteRaiders(final ServerWorld serverWorld);
	
	@Shadow
	protected abstract void updateBarToPlayers(final ServerWorld serverWorld);
	
	@Shadow
	protected abstract boolean shouldSpawnMoreGroups();
	
	@Shadow
	public abstract int getRaiderCount();
	
	@Shadow
	protected abstract void moveRaidCenter(final ServerWorld serverWorld);
	
	@Shadow
	public abstract boolean isFinished();
	
	@Shadow
	public abstract void invalidate();
	
	@Shadow
	public abstract boolean hasStopped();
	
	@Shadow
	protected abstract void playRaidHorn(final ServerWorld serverWorld, final BlockPos pos);
	
	@Shadow
	protected abstract void spawnNextWave(final ServerWorld serverWorld, final BlockPos pos);
}
