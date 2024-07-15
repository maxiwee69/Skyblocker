package de.hysky.skyblocker.stp.predicates;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.stp.SkyblockerPredicateType;
import de.hysky.skyblocker.stp.SkyblockerPredicateTypes;
import de.hysky.skyblocker.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

/**
 * Allows for matching to either a regex or a specific value on some data inside of nan item's custom data by specifying
 * a {@code path} to it. Each compound name must be separated by a dot (.), if the field lives directly under the
 * custom data then just put the field name in as the path.
 * 
 * Example Paths: drill_data.fuel, dye_item, something.is.here.
 * 
 * @since 1.22.0
 */
public record CustomDataPredicate(String[] path, Optional<Pattern> regex, Optional<ValueMatcher> valueMatcher) implements SkyblockerTexturePredicate {
	public static final Identifier ID = Identifier.of(SkyblockerMod.NAMESPACE, "custom_data");
	public static final MapCodec<CustomDataPredicate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.validate(CustomDataPredicate::validatePath).fieldOf("path").forGetter(CustomDataPredicate::pathString),
			Codecs.REGULAR_EXPRESSION.optionalFieldOf("regex").forGetter(CustomDataPredicate::regex),
			ValueMatcher.CODEC.optionalFieldOf("valueMatcher").forGetter(CustomDataPredicate::valueMatcher))
			.apply(instance, CustomDataPredicate::new));
	public static final Codec<CustomDataPredicate> CODEC = MAP_CODEC.codec();
	private static final Pattern PATH_VALIDATION_PATTERN = Pattern.compile("[A-Za-z0-9_'.]+");

	private CustomDataPredicate(String path, Optional<Pattern> regex, Optional<ValueMatcher> valueMatcher) {
		this(path.split("\\."), regex, valueMatcher);
	}

	public String pathString() {
		return String.join(".", path);
	}

	@Override
	public boolean test(ItemStack stack) {
		NbtElement element = findElement(ItemUtils.getCustomData(stack));

		//The path was valid - the instanceof also acts here as an implicit not null check
		if (element instanceof NbtString || element instanceof AbstractNbtNumber) {
			if (regex.isPresent()) {
				String stringified = element.asString();

				return regex.get().matcher(stringified).matches();
			} else if (valueMatcher.isPresent()) {
				return valueMatcher.get().test(element);
			}
		}

		return false;
	}

	@Override
	public SkyblockerPredicateType<?> getType() {
		return SkyblockerPredicateTypes.CUSTOM_DATA;
	}

	private NbtElement findElement(NbtCompound customData) {
		String[] split = path;
		String finalElementName = split[split.length - 1];

		//Fast path for direct field lookup - avoids the other allocations/memory copies
		if (split.length == 1) {
			return customData.get(finalElementName);
		}

		String[] compounds2Traverse = new String[split.length - 1];

		//Copy the traversal path into that array
		System.arraycopy(split, 0, compounds2Traverse, 0, split.length - 1);

		NbtCompound compound = customData;

		//Hopefully arrive at the end
		for (String compoundName : compounds2Traverse) {
			compound = compound.getCompound(compoundName);
		}

		return compound.get(finalElementName);
	}

	/**
	 * @implNote bytes and shorts are covered by the {@code intValue}, I am not writing more primitive specializations.
	 */
	private record ValueMatcher(Optional<String> stringValue, OptionalInt intValue, OptionalLong longValue, Optional<Float> floatValue, OptionalDouble doubleValue) {
		static final Codec<ValueMatcher> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("stringValue").forGetter(ValueMatcher::stringValue),
				optionalInt(Codec.INT.optionalFieldOf("intValue")).forGetter(ValueMatcher::intValue),
				Codecs.optionalLong(Codec.LONG.optionalFieldOf("longValue")).forGetter(ValueMatcher::longValue),
				Codec.FLOAT.optionalFieldOf("floatValue").forGetter(ValueMatcher::floatValue),
				optionalDouble(Codec.DOUBLE.optionalFieldOf("doubleValue")).forGetter(ValueMatcher::doubleValue))
				.apply(instance, ValueMatcher::new));

		boolean test(NbtElement element) {
			return switch (element) {
				case NbtString nbtString when stringValue.isPresent() -> stringValue.get().equals(nbtString.asString());

				//Int-like types
				case NbtByte nbtByte when intValue.isPresent() -> intValue.getAsInt() == nbtByte.byteValue();
				case NbtShort nbtShort when intValue.isPresent() -> intValue.getAsInt() == nbtShort.shortValue();
				case NbtInt nbtInt when intValue.isPresent() -> intValue.getAsInt() == nbtInt.intValue();

				//Longs
				case NbtLong nbtLong when longValue.isPresent() -> longValue.getAsLong() == nbtLong.longValue();

				//Floating Point types
				case NbtFloat nbtFloat when floatValue.isPresent() -> floatValue.get() == nbtFloat.floatValue();
				case NbtDouble nbtDouble when doubleValue.isPresent() -> doubleValue.getAsDouble() == nbtDouble.doubleValue();

				default -> false;
			};
		}
	}

	/**
	 * Validates that this is a path we can parse.
	 */
	private static DataResult<String> validatePath(String path) {
		if (PATH_VALIDATION_PATTERN.matcher(path).matches()) {
			return DataResult.success(path);
		} else {
			return DataResult.error(() -> "Path \"" + path + "\" failed validation! Expected it to match the pattern: " + PATH_VALIDATION_PATTERN.pattern());
		}
	}

	//TODO move these helpers into a Codec util class in the future and add a note there about Mojang's optional long existing or have a proxy to it
	private static MapCodec<OptionalInt> optionalInt(MapCodec<Optional<Integer>> codec) {
		return codec.xmap(opt -> opt.map(OptionalInt::of).orElseGet(OptionalInt::empty), optInt -> optInt.isPresent() ? Optional.of(optInt.getAsInt()) : Optional.empty());
	}

	private static MapCodec<OptionalDouble> optionalDouble(MapCodec<Optional<Double>> codec) {
		return codec.xmap(opt -> opt.map(OptionalDouble::of).orElseGet(OptionalDouble::empty), optDouble -> optDouble.isPresent() ? Optional.of(optDouble.getAsDouble()) : Optional.empty());
	}
}
