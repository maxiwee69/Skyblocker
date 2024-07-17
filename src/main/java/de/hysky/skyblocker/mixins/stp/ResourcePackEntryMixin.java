package de.hysky.skyblocker.mixins.stp;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import de.hysky.skyblocker.stp.SkyblockerRPMetadata;
import de.hysky.skyblocker.stp.SkyblockerRPMetadata.BorderType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;

@Mixin(PackListWidget.ResourcePackEntry.class)
public class ResourcePackEntryMixin {
	@Unique
	private static final int STP_COLOR = 0xFFE5B80B;

	@Shadow
	@Final
	private PackListWidget widget;
	@Shadow
	@Final
	private ResourcePackOrganizer.Pack pack;

	@Inject(method = "render", at = @At("HEAD"))
	private void skyblocker$stpPackBorder(CallbackInfo ci, @Local(argsOnly = true) DrawContext context, @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int x, @Local(argsOnly = true, ordinal = 3) int entryWidth, @Local(argsOnly = true, ordinal = 4) int entryHeight) {
		if (this.pack instanceof AbstractPackAccessor abstractPack && abstractPack.getProfile().getSkyblockerMetadata() != null) {
			SkyblockerRPMetadata metadata = abstractPack.getProfile().getSkyblockerMetadata();

			if (metadata.stpSupported() && metadata.borderType() != BorderType.OFF) {
				boolean isScrollbarVisible = ((EntryListWidgetInvoker) this.widget).invokeIsScrollbarVisible();
				int rightX = x + entryWidth - 3 - (isScrollbarVisible ? 7 : 0);

				//Top Line
				context.fill(metadata.borderType().layer(), x - 1, y - 1, rightX, y, STP_COLOR);

				//Left Line
				context.fill(metadata.borderType().layer(), x - 1, y - 1, x, y + entryHeight + 1, STP_COLOR);

				//Bottom Line
				context.fill(metadata.borderType().layer(), x - 1, y + entryHeight, rightX, y + entryHeight + 1, STP_COLOR);

				//Right Line
				context.fill(metadata.borderType().layer(), rightX - 1, y, rightX, y + entryHeight + 1, STP_COLOR);
			}
		}
	}
}
