package de.hysky.skyblocker.mixins.stp;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import de.hysky.skyblocker.injected.SkyblockerResourcePackProfile;
import de.hysky.skyblocker.stp.SkyblockerRPMetadata;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackProfile;

@Mixin(ResourcePackProfile.class)
public class ResourcePackProfileMixin implements SkyblockerResourcePackProfile {
	@Unique
	private SkyblockerRPMetadata skyblockerMetadata;

	@ModifyReturnValue(method = "create", at = @At("RETURN"))
	private static ResourcePackProfile skyblocker$injectSkyblockerMetadata(ResourcePackProfile original, @Local(argsOnly = true) ResourcePackInfo info, @Local(argsOnly = true) ResourcePackProfile.PackFactory packFactory) {
		if (original != null) {
			try (ResourcePack pack = packFactory.open(info)) {
				SkyblockerRPMetadata metadata = pack.parseMetadata(SkyblockerRPMetadata.SERIALIZER);

				original.setSkyblockerMetadata(metadata);
			} catch (Throwable ignored) {}
		}

		return original;
	}

	@Override
	public void setSkyblockerMetadata(SkyblockerRPMetadata metadata) {
		this.skyblockerMetadata = metadata;
	}

	@Override
	@Nullable
	public SkyblockerRPMetadata getSkyblockerMetadata() {
		return this.skyblockerMetadata;
	}
}
