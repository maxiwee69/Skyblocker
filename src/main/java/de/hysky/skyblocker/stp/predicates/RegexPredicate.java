package de.hysky.skyblocker.stp.predicates;

import java.util.List;
import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.stp.SkyblockerPredicateType;
import de.hysky.skyblocker.stp.SkyblockerPredicateTypes;
import de.hysky.skyblocker.utils.ItemUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

/**
 * Allows for matching via a regex on an item's name, lore, or for player heads also the texture base64; supports full, and partial regex matches.
 * 
 * @since 1.22.0
 */
public record RegexPredicate(MatchType matchType, MatchTarget target, Pattern regex) implements SkyblockerTexturePredicate {
	public static final Identifier ID = Identifier.of(SkyblockerMod.NAMESPACE, "regex");
	public static final MapCodec<RegexPredicate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			MatchType.CODEC.fieldOf("matchType").forGetter(RegexPredicate::matchType),
			MatchTarget.CODEC.fieldOf("target").forGetter(RegexPredicate::target),
			Codecs.REGULAR_EXPRESSION.fieldOf("regex").forGetter(RegexPredicate::regex))
			.apply(instance, RegexPredicate::new));
	public static final Codec<RegexPredicate> CODEC = MAP_CODEC.codec();

	@Override
	public boolean test(ItemStack stack) {
		try {
			switch (this.target) {
				case LORE -> {
					if (stack.contains(DataComponentTypes.LORE)) {
						List<Text> lore = stack.get(DataComponentTypes.LORE).lines();

						for (Text line : lore) {
							switch (this.matchType) {
								case FULL -> {
									if (this.regex.matcher(line.getString()).matches()) return true;
								}

								case PARTIAL -> {
									if (this.regex.matcher(line.getString()).find()) return true;
								}
							}
						}
					}
				}

				case NAME -> {
					return switch (this.matchType) {
						case FULL -> this.regex.matcher(stack.getName().getString()).matches();
						case PARTIAL -> this.regex.matcher(stack.getName().getString()).find();
					};
				}

				case TEXTURE -> {
					if (stack.isOf(Items.PLAYER_HEAD)) {
						String textureBase64 = ItemUtils.getHeadTexture(stack);

						if (!textureBase64.isEmpty()) {
							return switch (this.matchType) {
								case FULL -> this.regex.matcher(textureBase64).matches();
								case PARTIAL -> this.regex.matcher(textureBase64).find();
							};
						}
					}
				}
			}
		} catch (Exception ignored) {}

		return false;
	}

	@Override
	public SkyblockerPredicateType<?> getType() {
		return SkyblockerPredicateTypes.REGEX;
	}

	private enum MatchType implements StringIdentifiable {
		FULL,
		PARTIAL;

		private static final Codec<MatchType> CODEC = StringIdentifiable.createCodec(MatchType::values);

		@Override
		public String asString() {
			return name();
		}
	}
}
