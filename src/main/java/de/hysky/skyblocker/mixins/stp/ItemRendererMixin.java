package de.hysky.skyblocker.mixins.stp;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.stp.SkyblockerItemTextures;
import de.hysky.skyblocker.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@WrapOperation(method = "getModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemModels;getModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/render/model/BakedModel;"))
	private BakedModel skyblocker$overrideModel(ItemModels models, ItemStack stack, Operation<BakedModel> operation) {
		if (Utils.isOnSkyblock() && SkyblockerConfigManager.get().uiAndVisuals.skyblockerTexturePredicates.itemTextures) {
			String skyblockId = stack.getSkyblockId();

			if (skyblockId != null && !skyblockId.isEmpty()) {
				Identifier id = SkyblockerItemTextures.getModelId(stack.getSkyblockId());

				if (id != null) {
					BakedModel model = client.getBakedModelManager().getModel(id);

					if (model != null) return model;
				}
			}
		}

		return operation.call(models, stack);
	}
}
