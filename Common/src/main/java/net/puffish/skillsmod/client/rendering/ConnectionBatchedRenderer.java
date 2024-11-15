package net.puffish.skillsmod.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ConnectionBatchedRenderer {
	private final Int2ObjectMap<List<TriangleEmit>> strokeBatch = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<List<TriangleEmit>> fillBatch = new Int2ObjectOpenHashMap<>();

	private record TriangleEmit(
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3
	) { }

	public void emitConnection(
			DrawContext context,
			float startX,
			float startY,
			float endX,
			float endY,
			boolean bidirectional,
			int fillColor,
			int strokeColor
	) {
		var matrix = context.getMatrices().peek().getPositionMatrix();

		emitLine(strokeBatch, matrix, strokeColor, startX, startY, endX, endY, 3);
		if (!bidirectional) {
			emitArrow(strokeBatch, matrix, strokeColor, startX, startY, endX, endY, 8);
		}
		emitLine(fillBatch, matrix, fillColor, startX, startY, endX, endY, 1);
		if (!bidirectional) {
			emitArrow(fillBatch, matrix, fillColor, startX, startY, endX, endY, 6);
		}
	}

	private void emitLine(
			Int2ObjectMap<List<TriangleEmit>> batch,
			Matrix4f matrix,
			int color,
			float startX,
			float startY,
			float endX,
			float endY,
			float thickness
	) {
		var side = new Vector2f(endX, endY)
				.sub(startX, startY)
				.normalize()
				.perpendicular()
				.mul(thickness / 2f);

		emitTriangle(
				batch, matrix, color,
				startX + side.x, startY + side.y,
				startX - side.x, startY - side.y,
				endX + side.x, endY + side.y
		);
		emitTriangle(
				batch, matrix, color,
				endX - side.x, endY - side.y,
				endX + side.x, endY + side.y,
				startX - side.x, startY - side.y
		);
	}

	private void emitArrow(
			Int2ObjectMap<List<TriangleEmit>> batch,
			Matrix4f matrix,
			int color,
			float startX,
			float startY,
			float endX,
			float endY,
			float thickness
	) {
		var center = new Vector2f(endX, endY)
				.add(startX, startY)
				.div(2f);
		var normal = new Vector2f(endX, endY)
				.sub(startX, startY)
				.normalize();
		var forward = new Vector2f(normal)
				.mul(thickness);
		var backward = new Vector2f(forward)
				.div(-2f);
		var back = new Vector2f(center)
				.add(backward);
		var side = new Vector2f(backward)
				.perpendicular()
				.mul(MathHelper.sqrt(3f));

		emitTriangle(
				batch, matrix, color,
				center.x + forward.x, center.y + forward.y,
				back.x - side.x, back.y - side.y,
				back.x + side.x, back.y + side.y
		);
	}

	private void emitTriangle(
			Int2ObjectMap<List<TriangleEmit>> batch,
			Matrix4f matrix,
			int color,
			float x1, float y1,
			float x2, float y2,
			float x3, float y3

	) {
		var v1 = matrix.transformPosition(new Vector3f(x1, y1, 0f));
		var v2 = matrix.transformPosition(new Vector3f(x2, y2, 0f));
		var v3 = matrix.transformPosition(new Vector3f(x3, y3, 0f));

		var emits = batch.computeIfAbsent(color, key -> new ArrayList<>());

		emits.add(new TriangleEmit(
				v1.x, v1.y, v1.z,
				v2.x, v2.y, v2.z,
				v3.x, v3.y, v3.z
		));
	}

	public void draw() {
		RenderSystem.setShader(ShaderProgramKeys.POSITION);

		drawBatch(strokeBatch);
		drawBatch(fillBatch);
	}

	private void drawBatch(Int2ObjectMap<List<TriangleEmit>> batch) {
		RenderSystem.setShader(ShaderProgramKeys.POSITION);

		for (var entry : batch.int2ObjectEntrySet()) {
			var color = entry.getIntKey();
			var a = (float) ((color >> 24) & 0xff) / 255f;
			var r = (float) ((color >> 16) & 0xff) / 255f;
			var g = (float) ((color >> 8) & 0xff) / 255f;
			var b = (float) (color & 0xff) / 255f;
			RenderSystem.setShaderColor(r, g, b, a);

			var bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION);
			for (var emit : entry.getValue()) {
				bufferBuilder.vertex(emit.x1, emit.y1, emit.z1);
				bufferBuilder.vertex(emit.x2, emit.y2, emit.z2);
				bufferBuilder.vertex(emit.x3, emit.y3, emit.z3);
			}
			BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
		}

		batch.clear();
	}
}
