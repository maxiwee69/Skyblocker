package de.hysky.skyblocker.stp.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.stp.SkyblockerPredicateType;
import de.hysky.skyblocker.stp.SkyblockerPredicateTypes;
import de.hysky.skyblocker.utils.render.RenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Checks whether a position is within the bounding box specified by {@code pos1} and {@code pos2}.
 * 
 * @since 1.22.0
 */
public record BoundingBoxPredicate(BlockPos pos1, BlockPos pos2) implements SkyblockerTexturePredicate {
	public static final Identifier ID = Identifier.of(SkyblockerMod.NAMESPACE, "bounding_box");
	public static final MapCodec<BoundingBoxPredicate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos1").forGetter(BoundingBoxPredicate::pos1),
			BlockPos.CODEC.fieldOf("pos2").forGetter(BoundingBoxPredicate::pos2))
			.apply(instance, BoundingBoxPredicate::new));
	public static final Codec<BoundingBoxPredicate> CODEC = MAP_CODEC.codec();

	@Override
	public boolean test(ItemStack stack) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;

		if (player != null) return test(player.getBlockPos());

		return false;
	}

	/**
	 * Allows for testing on other positions.
	 */
	public boolean test(BlockPos pos) {
		return RenderHelper.pointIsInArea(pos.getX(), pos.getY(), pos.getZ(), pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
	}

	@Override
	public SkyblockerPredicateType<?> getType() {
		return SkyblockerPredicateTypes.BOUNDING_BOX;
	}

	@Override
	public boolean itemStackDependent() {
		return false;
	}
}
