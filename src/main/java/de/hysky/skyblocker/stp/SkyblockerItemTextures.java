package de.hysky.skyblocker.stp;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.mixins.stp.JsonUnbakedModelAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.OnLoad;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class SkyblockerItemTextures {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String MODELS_PREFIX = "models/";
	private static final String ITEM_MODELS_FOLDER = "item";
	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("(?<name>[A-Za-z0-9_\\-]+)(?=.json)");
	private static final Pattern FILE_PATH_PATTERN = Pattern.compile("(?<path>[\\/A-Za-z0-9-_]*\\/)(?=[A-Za-z0-9\\-_])");

	private static Map<String, Identifier> ID_2_MODEL_ID = new Object2ObjectOpenHashMap<>();

	public static void init() {
		PreparableModelLoadingPlugin.register(SkyblockerItemTextures::prepareItemModels, (data, pluginContext) -> {
			pluginContext.addModels(data);
			pluginContext.modifyModelOnLoad().register(SkyblockerItemTextures::onModelLoad);
		});
	}

	private static CompletableFuture<Collection<Identifier>> prepareItemModels(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Map<String, Identifier> ids2ModelIds = new Object2ObjectOpenHashMap<>();
				Map<Identifier, Resource> modelFiles = manager.findResources(MODELS_PREFIX + ITEM_MODELS_FOLDER, id -> id.getNamespace().equals(SkyblockerMod.NAMESPACE) && id.getPath().endsWith(".json"));

				for (Identifier id : modelFiles.keySet()) {
					String path = id.getPath();
					Identifier formatted = Identifier.of(SkyblockerMod.NAMESPACE, path.replaceFirst(MODELS_PREFIX, "").replace(".json", ""));
					Matcher matcher = FILE_NAME_PATTERN.matcher(path);

					if (matcher.find()) {
						ids2ModelIds.put(matcher.group("name").toUpperCase(Locale.ENGLISH), formatted);
					} else {
						LOGGER.error("[Skyblocker Item Textures] Could not find name for id {}.", id);
					}
				}

				ID_2_MODEL_ID = ids2ModelIds;

				return ids2ModelIds.values();
			} catch (Throwable t) {
				LOGGER.error("[Skyblocker Item Textures] Failed to prepare item models!", t);
			}

			return Set.of();
		}, executor);
	}

	/**
	 * Allows for special syntax to automatically resolve of texture layer paths to ease porting pains and reduce boilerplate.
	 */
	private static UnbakedModel onModelLoad(UnbakedModel model, OnLoad.Context context) {
		Identifier modelId = context.resourceId();

		if (modelId != null && modelId.getNamespace().equals(SkyblockerMod.NAMESPACE) && modelId.getPath().startsWith("item/") && model instanceof JsonUnbakedModelAccessor jsonModel) {
			for (Map.Entry<String, Either<SpriteIdentifier, String>> entry : jsonModel.getTextureMap().entrySet()) {
				Optional<SpriteIdentifier> spriteId = entry.getValue().left();

				if (spriteId.isPresent()) {
					Identifier tex = spriteId.get().getTextureId();
					String modelIdPath = modelId.getPath();

					if (tex.getNamespace().equals(SkyblockerMod.NAMESPACE)) {
						if (tex.getPath().startsWith("relative/")) {
							Matcher matcher = FILE_PATH_PATTERN.matcher(modelIdPath);

							if (matcher.find()) {
								Identifier newTexturePath = Identifier.of(SkyblockerMod.NAMESPACE, tex.getPath().replace("relative/", matcher.group("path")));
								SpriteIdentifier newSpriteId = new SpriteIdentifier(spriteId.get().getAtlasId(), newTexturePath);

								entry.setValue(Either.left(newSpriteId));
							} else {
								LOGGER.error("[Skyblocker Item Textures] Failed to match path for model id {}", modelId);
							}
						} else if (tex.getPath().equals("exact")) {
							SpriteIdentifier newSpriteId = new SpriteIdentifier(spriteId.get().getAtlasId(), modelId);

							entry.setValue(Either.left(newSpriteId));
						}
					}
				}
			}
		}

		return model;
	}

	public static Identifier getModelId(String skyblockId) {
		return ID_2_MODEL_ID.get(skyblockId);
	}
}
