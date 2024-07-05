package de.hysky.skyblocker.stp.predicates;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.stp.SkyblockerPredicateType;
import de.hysky.skyblocker.stp.SkyblockerPredicateTypes;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

/**
 * Allows for matching to the {@code id} (UUID) field of a profile component.
 * 
 * @since 1.22.0
 */
public record ProfileComponentPredicate(UUID uuid) implements SkyblockerTexturePredicate {
	public static final Identifier ID = Identifier.of(SkyblockerMod.NAMESPACE, "profile_component");
	public static final MapCodec<ProfileComponentPredicate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Uuids.STRING_CODEC.fieldOf("uuid").forGetter(ProfileComponentPredicate::uuid))
			.apply(instance, ProfileComponentPredicate::new));
	public static final Codec<ProfileComponentPredicate> CODEC = MAP_CODEC.codec();

	@Override
	public boolean test(ItemStack stack) {
		if (stack.contains(DataComponentTypes.PROFILE)) {
			ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);

			return profile.id().isPresent() ? profile.id().get().equals(uuid) : false;
		}

		return false;
	}

	@Override
	public SkyblockerPredicateType<?> getType() {
		return SkyblockerPredicateTypes.PROFILE_COMPONENT;
	}
}
