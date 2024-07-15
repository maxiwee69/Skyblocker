package de.hysky.skyblocker.stp;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.stp.predicates.SkyblockerTexturePredicate;
import de.hysky.skyblocker.utils.Utils;
import de.hysky.skyblocker.utils.scheduler.Scheduler;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/**
 * Custom Armor Textures!
 */
public class SkyblockerArmorTextures {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String ARMOR_OVERRIDES_PATH = "overrides/armor";
	private static final Object2ObjectOpenHashMap<String, ArmorModelOverride> ARMOR_OVERRIDES = new Object2ObjectOpenHashMap<>();
	private static final Int2ReferenceOpenHashMap<List<ArmorMaterial.Layer>> CACHE = new Int2ReferenceOpenHashMap<>();
	public static final List<ArmorMaterial.Layer> NO_CUSTOM_TEXTURES = List.of();

	public static void init() {
		Scheduler.INSTANCE.scheduleCyclic(CACHE::clear, 4800);
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return Identifier.of(SkyblockerMod.NAMESPACE, "custom_armor_textures");
			}

			@Override
			public void reload(ResourceManager manager) {
				loadArmorPredicates(manager);
			}
		});
	}

	private static void loadArmorPredicates(ResourceManager manager) {
		try {
			ARMOR_OVERRIDES.clear();
			//Load armour textures from our namespace in the overrides folder
			Map<Identifier, Resource> overrides = manager.findResources(ARMOR_OVERRIDES_PATH, id -> id.getNamespace().equals(SkyblockerMod.NAMESPACE) && id.getPath().endsWith(".json"));
			List<CompletableFuture<Pair<List<String>, ArmorModelOverride>>> futures = new ArrayList<>();

			for (Map.Entry<Identifier, Resource> entry : overrides.entrySet()) {
				CompletableFuture<Pair<List<String>, ArmorModelOverride>> future = computeOverride(entry.getKey(), entry.getValue());

				futures.add(future);
			}

			//Block thread until all armour texture overrides have been loaded
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

			for (CompletableFuture<Pair<List<String>, ArmorModelOverride>> future : futures) {
				Pair<List<String>, ArmorModelOverride> override = future.join(); //Should never throw

				if (override != null) {
					for (String id : override.left()) {
						ARMOR_OVERRIDES.put(id, override.right());
					}
				}
			}

			CACHE.clear();
		} catch (Throwable t) {
			LOGGER.error("[Skyblocker Armor Textures] Failed to load armor textures!", t);
		}
	}

	private static CompletableFuture<Pair<List<String>, ArmorModelOverride>> computeOverride(Identifier id, Resource resource) {
		return CompletableFuture.supplyAsync(() -> {
			try (BufferedReader reader = resource.getReader()) {
				JsonObject customArmorModelOverride = JsonParser.parseReader(reader).getAsJsonObject();
				List<String> itemIds = Codec.STRING.listOf().parse(JsonOps.INSTANCE, customArmorModelOverride.get("itemIds")).getOrThrow();
				List<ArmorMaterial.Layer> layers = CustomArmorLayer.LIST_CODEC.parse(JsonOps.INSTANCE, customArmorModelOverride.get("layers")).getOrThrow().stream()
						.map(CustomArmorLayer::toLayer)
						.toList();
				ArmorModelOverride.ArmorModelOverrideOverride[] overrides = new ArmorModelOverride.ArmorModelOverrideOverride[0];

				if (customArmorModelOverride.has("overrides")) {
					overrides =  customArmorModelOverride.getAsJsonArray("overrides").asList().stream()
							.map(JsonElement::getAsJsonObject)
							.map(obj -> {
								SkyblockerTexturePredicate[] predicates = SkyblockerTexturePredicates.compilePredicates(obj);
								List<ArmorMaterial.Layer> layerOverride = CustomArmorLayer.LIST_CODEC.parse(JsonOps.INSTANCE, obj.get("layers")).getOrThrow().stream()
														.map(CustomArmorLayer::toLayer)
														.toList();

								return new ArmorModelOverride.ArmorModelOverrideOverride(predicates, layerOverride);
							}).toArray(ArmorModelOverride.ArmorModelOverrideOverride[]::new);
				}

				return Pair.of(itemIds, new ArmorModelOverride(layers, overrides));
			} catch (Exception e) {
				LOGGER.error("[Skyblocker Armor Textures] Failed to load armor override {}!", id, e);
			}

			return null;
		}, Executors.newVirtualThreadPerTaskExecutor());
	}

	/**
	 * The result of this method is cached until textures reload and cleared every 5 minutes because armor model overrides with overrides could
	 * get expensive if they are tested constantly.
	 */
	public static List<ArmorMaterial.Layer> getCustomArmorTextureLayers(ItemStack stack) {
		if (Utils.isOnSkyblock()) {
			int hashCode = getHashCode(stack);

			if (CACHE.containsKey(hashCode)) return CACHE.get(hashCode);

			String id = stack.getSkyblockId();

			if (id != null && ARMOR_OVERRIDES.containsKey(id)) {
				ArmorModelOverride modelOverride = ARMOR_OVERRIDES.get(id);

				overrideLoop: for (ArmorModelOverride.ArmorModelOverrideOverride override : modelOverride.overrides()) {
					for (SkyblockerTexturePredicate predicate : override.predicates()) {
						if (!predicate.test(stack)) continue overrideLoop;
					}

					CACHE.put(hashCode, override.layerOverride());

					return override.layerOverride();
				}

				CACHE.put(hashCode, modelOverride.layers());

				return modelOverride.layers();
			}
		}

		return NO_CUSTOM_TEXTURES;
	}

	/**
	 * Caching is done based on the identity hash code as that won't change unless the item stack instance does.
	 * This method is the most efficient while maintaining accuracy and has a net-zero performance impact as you would expect from the mod.
	 */
	private static int getHashCode(ItemStack stack) {
		return System.identityHashCode(stack);
	}

	private record ArmorModelOverride(List<ArmorMaterial.Layer> layers, ArmorModelOverrideOverride[] overrides) {
		private record ArmorModelOverrideOverride(SkyblockerTexturePredicate[] predicates, List<ArmorMaterial.Layer> layerOverride) {
		}
	}

	private record CustomArmorLayer(Identifier id, String suffix, boolean dyeable) {
		private static final Codec<CustomArmorLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("id").forGetter(CustomArmorLayer::id),
				Codec.STRING.optionalFieldOf("suffix", "").forGetter(CustomArmorLayer::suffix),
				Codec.BOOL.optionalFieldOf("dyeable", false).forGetter(CustomArmorLayer::dyeable))
				.apply(instance, CustomArmorLayer::new));
		private static final Codec<List<CustomArmorLayer>> LIST_CODEC = CODEC.listOf();

		ArmorMaterial.Layer toLayer() {
			return new ArmorMaterial.Layer(this.id, this.suffix, this.dyeable);
		}
	}
}
