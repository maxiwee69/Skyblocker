package de.hysky.skyblocker.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;

import de.hysky.skyblocker.SkyblockerMod;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhase;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Cull;
import net.minecraft.client.render.RenderPhase.DepthTest;
import net.minecraft.client.render.RenderPhase.Transparency;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class SkyblockerRenderLayers {
	private static final Transparency DEFAULT_TRANSPARENCY = new Transparency("default_transparency", () -> {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}, RenderSystem::disableBlend);

	private static ShaderProgram goldProgram = null;
	private static final RenderPhase.ShaderProgram GOLD_PROGRAM = new RenderPhase.ShaderProgram(() -> setupProgram(goldProgram));

	private static ShaderProgram chromaProgram = null;
	private static final RenderPhase.ShaderProgram CHROMA_PROGRAM = new RenderPhase.ShaderProgram(() -> setupProgram(chromaProgram));

	public static void init() {
		CoreShaderRegistrationCallback.EVENT.register(context -> {
			context.register(Identifier.of(SkyblockerMod.NAMESPACE, "gold"), VertexFormats.POSITION_COLOR, program -> goldProgram = program);
			context.register(Identifier.of(SkyblockerMod.NAMESPACE, "chroma"), VertexFormats.POSITION_COLOR, program -> chromaProgram = program);
		});
	}

	public static final MultiPhase FILLED = RenderLayer.of("filled", VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP, RenderLayer.CUTOUT_BUFFER_SIZE, false, true, MultiPhaseParameters.builder()
			.program(RenderPhase.COLOR_PROGRAM)
			.cull(Cull.DISABLE_CULLING)
			.layering(RenderPhase.POLYGON_OFFSET_LAYERING)
			.transparency(DEFAULT_TRANSPARENCY)
			.depthTest(DepthTest.LEQUAL_DEPTH_TEST)
			.build(false));

	public static final MultiPhase FILLED_THROUGH_WALLS = RenderLayer.of("filled_through_walls", VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP, RenderLayer.CUTOUT_BUFFER_SIZE, false, true, MultiPhaseParameters.builder()
			.program(RenderPhase.COLOR_PROGRAM)
			.cull(Cull.DISABLE_CULLING)
			.layering(RenderPhase.POLYGON_OFFSET_LAYERING)
			.transparency(DEFAULT_TRANSPARENCY)
			.depthTest(DepthTest.ALWAYS_DEPTH_TEST)
			.build(false));

	public static final MultiPhase GOLD_GUI = RenderLayer.of("gold_gui", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, RenderLayer.CUTOUT_BUFFER_SIZE, MultiPhaseParameters.builder()
			.program(GOLD_PROGRAM)
			.transparency(Transparency.TRANSLUCENT_TRANSPARENCY)
			.depthTest(DepthTest.LEQUAL_DEPTH_TEST)
			.build(false));

	public static final MultiPhase CHROMA_GUI = RenderLayer.of("chroma_gui", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, RenderLayer.CUTOUT_BUFFER_SIZE, MultiPhaseParameters.builder()
			.program(CHROMA_PROGRAM)
			.transparency(Transparency.TRANSLUCENT_TRANSPARENCY)
			.depthTest(DepthTest.LEQUAL_DEPTH_TEST)
			.build(false));

	private static ShaderProgram setupProgram(ShaderProgram program) {
		program.getUniformOrDefault("SkyblockerTime").set((System.currentTimeMillis() % 30000L) / 30000f);

		return program;
	}
}
