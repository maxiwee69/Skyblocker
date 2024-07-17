package de.hysky.skyblocker.stp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.utils.render.SkyblockerRenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.StringIdentifiable;

public record SkyblockerRPMetadata(boolean stpSupported, BorderType borderType) {
	private static final Codec<SkyblockerRPMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("stp_supported").forGetter(SkyblockerRPMetadata::stpSupported),
			BorderType.CODEC.optionalFieldOf("border_type", BorderType.CHROMA).forGetter(SkyblockerRPMetadata::borderType))
			.apply(instance, SkyblockerRPMetadata::new));
	public static final ResourceMetadataSerializer<SkyblockerRPMetadata> SERIALIZER = ResourceMetadataSerializer.fromCodec(SkyblockerMod.NAMESPACE, CODEC);

	public enum BorderType implements StringIdentifiable {
		CHROMA(SkyblockerRenderLayers.CHROMA_GUI),
		GOLD(SkyblockerRenderLayers.GOLD_GUI),
		OFF(null);

		private static final Codec<BorderType> CODEC = StringIdentifiable.createBasicCodec(BorderType::values);

		private final RenderLayer layer;

		BorderType(RenderLayer layer) {
			this.layer = layer;
		}

		public RenderLayer layer() {
			return this.layer;
		}

		@Override
		public String asString() {
			return name();
		}
	}
}
