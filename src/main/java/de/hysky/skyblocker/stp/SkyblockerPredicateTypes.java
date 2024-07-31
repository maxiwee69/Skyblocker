package de.hysky.skyblocker.stp;

import de.hysky.skyblocker.stp.predicates.AndPredicate;
import de.hysky.skyblocker.stp.predicates.ApiIdPredicate;
import de.hysky.skyblocker.stp.predicates.BoundingBoxPredicate;
import de.hysky.skyblocker.stp.predicates.CustomDataPredicate;
import de.hysky.skyblocker.stp.predicates.HeldByArmorStandPredicate;
import de.hysky.skyblocker.stp.predicates.InsideScreenPredicate;
import de.hysky.skyblocker.stp.predicates.LocationPredicate;
import de.hysky.skyblocker.stp.predicates.NotPredicate;
import de.hysky.skyblocker.stp.predicates.OrPredicate;
import de.hysky.skyblocker.stp.predicates.PetInfoPredicate;
import de.hysky.skyblocker.stp.predicates.ProfileComponentPredicate;
import de.hysky.skyblocker.stp.predicates.RegexPredicate;
import de.hysky.skyblocker.stp.predicates.SkyblockerTexturePredicate;
import de.hysky.skyblocker.stp.predicates.StringPredicate;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public interface SkyblockerPredicateTypes {
	SkyblockerPredicateType<AndPredicate> AND = register(AndPredicate.ID, new SkyblockerPredicateType<>(AndPredicate.CODEC, AndPredicate.MAP_CODEC));
	SkyblockerPredicateType<ApiIdPredicate> API_ID = register(ApiIdPredicate.ID, new SkyblockerPredicateType<>(ApiIdPredicate.CODEC, ApiIdPredicate.MAP_CODEC));
	SkyblockerPredicateType<BoundingBoxPredicate> BOUNDING_BOX = register(BoundingBoxPredicate.ID, new SkyblockerPredicateType<>(BoundingBoxPredicate.CODEC, BoundingBoxPredicate.MAP_CODEC));
	SkyblockerPredicateType<CustomDataPredicate> CUSTOM_DATA = register(CustomDataPredicate.ID, new SkyblockerPredicateType<>(CustomDataPredicate.CODEC, CustomDataPredicate.MAP_CODEC));
	SkyblockerPredicateType<HeldByArmorStandPredicate> HELD_BY_ARMOR_STAND = register(HeldByArmorStandPredicate.ID, new SkyblockerPredicateType<>(HeldByArmorStandPredicate.CODEC, HeldByArmorStandPredicate.MAP_CODEC));
	SkyblockerPredicateType<InsideScreenPredicate> INSIDE_SCREEN = register(InsideScreenPredicate.ID, new SkyblockerPredicateType<>(InsideScreenPredicate.CODEC, InsideScreenPredicate.MAP_CODEC));
	SkyblockerPredicateType<LocationPredicate> LOCATION = register(LocationPredicate.ID, new SkyblockerPredicateType<>(LocationPredicate.CODEC, LocationPredicate.MAP_CODEC));
	SkyblockerPredicateType<NotPredicate> NOT = register(NotPredicate.ID, new SkyblockerPredicateType<>(NotPredicate.CODEC, NotPredicate.MAP_CODEC));
	SkyblockerPredicateType<OrPredicate> OR = register(OrPredicate.ID, new SkyblockerPredicateType<>(OrPredicate.CODEC, OrPredicate.MAP_CODEC));
	SkyblockerPredicateType<PetInfoPredicate> PET_INFO = register(PetInfoPredicate.ID, new SkyblockerPredicateType<>(PetInfoPredicate.CODEC, PetInfoPredicate.MAP_CODEC));
	SkyblockerPredicateType<ProfileComponentPredicate> PROFILE_COMPONENT = register(ProfileComponentPredicate.ID, new SkyblockerPredicateType<>(ProfileComponentPredicate.CODEC, ProfileComponentPredicate.MAP_CODEC));
	SkyblockerPredicateType<RegexPredicate> REGEX = register(RegexPredicate.ID, new SkyblockerPredicateType<>(RegexPredicate.CODEC, RegexPredicate.MAP_CODEC));
	SkyblockerPredicateType<StringPredicate> STRING = register(StringPredicate.ID, new SkyblockerPredicateType<>(StringPredicate.CODEC, StringPredicate.MAP_CODEC));

	//Trigger class loading
	static void init() {
	}

	static <T extends SkyblockerTexturePredicate> SkyblockerPredicateType<T> register(Identifier id, SkyblockerPredicateType<T> predicateType) {
		return Registry.register(SkyblockerPredicateType.REGISTRY, id, predicateType);
	}
}
