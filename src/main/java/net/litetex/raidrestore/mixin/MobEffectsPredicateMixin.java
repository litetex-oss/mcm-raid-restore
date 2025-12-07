package net.litetex.raidrestore.mixin;

import java.util.Map;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.UnboundedMapCodec;

import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;


@Mixin(MobEffectsPredicate.class)
public abstract class MobEffectsPredicateMixin
{
	@Redirect(
		method = "<clinit>",
		at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs"
			+ "/UnboundedMapCodec;xmap(Ljava/util/function/Function;Ljava/util/function/Function;)"
			+ "Lcom/mojang/serialization/Codec;"))
	private static Codec<MobEffectsPredicate> xmap(
		final UnboundedMapCodec<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> instance,
		final Function<Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate>, MobEffectsPredicate> to,
		final Function<MobEffectsPredicate, Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate>>
			from)
	{
		return instance.xmap(
			map -> to.apply(
				// Remove Raid omen from advancements
				map.containsKey(MobEffects.RAID_OMEN)
					? map.entrySet()
					.stream()
					.filter(e -> e.getKey() != MobEffects.RAID_OMEN)
					.collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
					: map),
			from);
	}
}
