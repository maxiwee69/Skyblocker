package de.hysky.skyblocker.stp;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.stp.predicates.BoundingBoxPredicate;
import de.hysky.skyblocker.stp.predicates.SkyblockerTexturePredicate;
import de.hysky.skyblocker.utils.Location;
import de.hysky.skyblocker.utils.Utils;
import de.hysky.skyblocker.utils.scheduler.Scheduler;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceObjectPair;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class SkyblockerBlockTextures {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	private static final String MODELS_PREFIX = "models/";
	private static final String BLOCK_MODELS_FOLDER = "block";
	private static final String BLOCK_OVERRIDES_PATH = "overrides/block";
	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(?<name>[A-Za-z0-9_\\- ]+)(?=.json)");
	private static final Reference2ObjectMap<Location, CustomBlockOverride> CUSTOM_BLOCK_OVERRIDES = new Reference2ObjectOpenHashMap<>();

	private static Reference2ObjectMap<Block, Identifier> lastReplacements = null;

	public static void init() {
		PreparableModelLoadingPlugin.register(SkyblockerBlockTextures::prepareBlockModels, (data, pluginContext) -> {
			pluginContext.addModels(data); //Load & Bake the prepared block models
		});
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return Identifier.of(SkyblockerMod.NAMESPACE, "custom_block_textures");
			}

			@Override
			public void reload(ResourceManager manager) {
				loadCustomBlockTextureDefinitions(manager);
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
	}

	private static CompletableFuture<Set<Identifier>> prepareBlockModels(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Set<Identifier> modelFileIds = new ObjectOpenHashSet<>();
				Map<Identifier, Resource> modelFiles = manager.findResources(MODELS_PREFIX + BLOCK_MODELS_FOLDER, id -> id.getNamespace().equals(SkyblockerMod.NAMESPACE) && id.getPath().endsWith(".json"));

				for (Identifier id : modelFiles.keySet()) {
					String path = id.getPath();
					Identifier formatted = Identifier.of(SkyblockerMod.NAMESPACE, path.replaceFirst(MODELS_PREFIX, "").replace(".json", ""));

					modelFileIds.add(formatted);
				}

				return modelFileIds;
			} catch (Throwable t) {
				LOGGER.error("[Skyblocker Block Textures] Failed to prepare block models for baking!", t);
			}

			return Set.<Identifier>of();
		}, executor);
	}

	private static void loadCustomBlockTextureDefinitions(ResourceManager manager) {
		try {
			CUSTOM_BLOCK_OVERRIDES.clear();
			Map<Identifier, Resource> blockOverrides = manager.findResources(BLOCK_OVERRIDES_PATH, id -> id.getNamespace().equals(SkyblockerMod.NAMESPACE) && id.getPath().endsWith(".json"));
			Set<CompletableFuture<ReferenceObjectPair<Location, CustomBlockOverride>>> futures = new ObjectOpenHashSet<>();

			for (Map.Entry<Identifier, Resource> entry : blockOverrides.entrySet()) {
				CompletableFuture<ReferenceObjectPair<Location, CustomBlockOverride>> future = computeBlockOverride(entry);

				futures.add(future);
			}

			//Block until all finished
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

			for (CompletableFuture<ReferenceObjectPair<Location, CustomBlockOverride>> future : futures) {
				ReferenceObjectPair<Location, CustomBlockOverride> override = future.get();

				if (override != null) {
					CUSTOM_BLOCK_OVERRIDES.put(override.left(), override.right());
				}
			}
		} catch (Throwable t) {
			LOGGER.error("[Skyblocker Block Textures] Failed to load custom block texture rules!", t);
		}
	}

	private static CompletableFuture<ReferenceObjectPair<Location, CustomBlockOverride>> computeBlockOverride(Map.Entry<Identifier, Resource> entry) {
		return CompletableFuture.supplyAsync(() -> {
			try (BufferedReader reader = entry.getValue().getReader()) {
				Matcher matcher = FILE_NAME_PATTERN.matcher(entry.getKey().toString());

				if (matcher.find()) {
					JsonObject file = JsonParser.parseReader(reader).getAsJsonObject();
					Location location = Location.from(matcher.group("name"));
					Reference2ObjectMap<Block, Identifier> mainReplacements = CustomBlockOverride.REPLACEMENTS_CODEC.parse(JsonOps.INSTANCE, file.get("replacements")).getOrThrow();
					CustomBlockOverride.CustomBlockSubOverride[] subOverrides = new CustomBlockOverride.CustomBlockSubOverride[0];

					if (file.has("overrides")) {
						subOverrides = file.getAsJsonArray("overrides").asList().stream()
								.map(JsonElement::getAsJsonObject)
								.map(override -> {
									Reference2ObjectMap<Block, Identifier> replacements = CustomBlockOverride.REPLACEMENTS_CODEC.parse(JsonOps.INSTANCE, override.get("replacements")).getOrThrow();
									SkyblockerTexturePredicate[] predicate = Arrays.stream(SkyblockerTexturePredicates.compilePredicates(override))
											.filter(tp -> !tp.itemStackDependent())
											.toArray(SkyblockerTexturePredicate[]::new);

									return new CustomBlockOverride.CustomBlockSubOverride(replacements, predicate);
								}).toArray(CustomBlockOverride.CustomBlockSubOverride[]::new);
					}

					if (location == Location.UNKNOWN) LOGGER.warn("[Skyblocker Block Textures] Read unknown location: {}. Double-check that the file name is correct.", matcher.group("name"));

					return ReferenceObjectPair.of(location, new CustomBlockOverride(mainReplacements, subOverrides));
				} else {
					LOGGER.error("[Skyblocker Block Textures] Couldn't parse file name! Name: {}", entry.getKey());
				}

				return null;
			} catch (Exception e) {
				LOGGER.error("[Skyblocker Block Textures] Failed to load regions from file {} :(", entry.getKey(), e);
			}

			return null;
		}, Executors.newVirtualThreadPerTaskExecutor());
	}

	public static Identifier getBlockReplacement(Block block, @Nullable BlockPos pos) {
		if (CUSTOM_BLOCK_OVERRIDES.containsKey(Utils.getLocation())) {
			CustomBlockOverride override = CUSTOM_BLOCK_OVERRIDES.get(Utils.getLocation());

			subOverrideLoop: for (CustomBlockOverride.CustomBlockSubOverride subOverride : override.overrides()) {
				for (SkyblockerTexturePredicate predicate : subOverride.predicate()) {
					boolean result = (predicate instanceof BoundingBoxPredicate boundingBox && pos != null) ? boundingBox.test(pos) : predicate.test(null);

					if (!result) continue subOverrideLoop;
				}

				Reference2ObjectMap<Block, Identifier> replacements = subOverride.replacements();
				updateReplacements(replacements);

				return replacements.get(block);
			}

			Reference2ObjectMap<Block, Identifier> replacements = override.replacements();
			updateReplacements(replacements);

			return replacements.get(block);
		}

		return null;
	}

	private static void updateReplacements(Reference2ObjectMap<Block, Identifier> replacements) {
		if (replacements != lastReplacements) {
			Scheduler.INSTANCE.schedule(CLIENT.worldRenderer::reload, 1);
			lastReplacements = replacements;
		}
	}

	private static void reset() {
		lastReplacements = null;
	}

	private record CustomBlockOverride(Reference2ObjectMap<Block, Identifier> replacements, CustomBlockSubOverride[] overrides) {
		private static final Codec<Reference2ObjectMap<Block, Identifier>> REPLACEMENTS_CODEC = Codec.unboundedMap(Identifier.CODEC.xmap(Registries.BLOCK::get, Registries.BLOCK::getId), Identifier.CODEC)
				.xmap(Reference2ObjectOpenHashMap::new, Reference2ObjectOpenHashMap::new);

		private record CustomBlockSubOverride(Reference2ObjectMap<Block, Identifier> replacements, SkyblockerTexturePredicate[] predicate) {}
	}
}
