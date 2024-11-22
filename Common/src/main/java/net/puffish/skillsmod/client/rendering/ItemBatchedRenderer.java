package net.puffish.skillsmod.client.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.puffish.skillsmod.access.MinecraftClientAccess;
import net.puffish.skillsmod.access.RenderLayerAccess;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ItemBatchedRenderer {

	private final Map<ComparableItemStack, List<Matrix4f>> batch = new HashMap<>();

	public void emitItem(DrawContext context, ItemStack item, int x, int y) {
		var emits = batch.computeIfAbsent(
				new ComparableItemStack(item),
				key -> new ArrayList<>()
		);

		emits.add(new Matrix4f(
				context.getMatrices().peek().getPositionMatrix()
		).translate(x, y, 0));
	}

	public void draw() {
		var matrices = new MatrixStack();
		matrices.translate(0, 0, 150);
		matrices.multiplyPositionMatrix(new Matrix4f().scaling(1f, -1f, 1f));
		matrices.scale(16f, 16f, 16f);

		for (var entry : batch.entrySet()) {
			var itemStack = entry.getKey().itemStack;

			var client = MinecraftClient.getInstance();

			var bakedModel = client.getItemRenderer().getModel(
					itemStack,
					client.world,
					client.player,
					0
			);

			if (bakedModel.isSideLit()) {
				DiffuseLighting.enableGuiDepthLighting();
			} else {
				DiffuseLighting.disableGuiDepthLighting();
			}

			var clientAccess = (MinecraftClientAccess) client;
			var immediate = clientAccess.getBufferBuilders().getEntityVertexConsumers();

			var layers = new HashSet<RenderLayerAccess>();

			client.getItemRenderer().renderItem(
					itemStack,
					ModelTransformationMode.GUI,
					false,
					matrices,
					layer -> {
						var layerAccess = (RenderLayerAccess) layer;
						layerAccess.setEmits(entry.getValue());
						layers.add(layerAccess);
						return immediate.getBuffer(layer);
					},
					0xF000F0,
					OverlayTexture.DEFAULT_UV,
					bakedModel
			);

			immediate.draw();

			for (var layer : layers) {
				layer.setEmits(null);
			}
		}
		batch.clear();
	}

	private record ComparableItemStack(ItemStack itemStack) {
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			return ItemStack.areEqual(this.itemStack, ((ComparableItemStack) o).itemStack);
		}

		@Override
		public int hashCode() {
			return itemStack.getItem().hashCode();
		}
	}
}
