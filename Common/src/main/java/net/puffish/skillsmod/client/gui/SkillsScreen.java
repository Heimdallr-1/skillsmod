package net.puffish.skillsmod.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementObtainedStatus;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vector4f;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientFrameData;
import net.puffish.skillsmod.client.data.ClientIconData;
import net.puffish.skillsmod.client.data.ClientSkillCategoryData;
import net.puffish.skillsmod.client.data.ClientSkillData;
import net.puffish.skillsmod.client.data.ClientSkillDefinitionData;
import net.puffish.skillsmod.client.network.packets.out.SkillClickOutPacket;
import net.puffish.skillsmod.client.rendering.ConnectionBatchedRenderer;
import net.puffish.skillsmod.client.rendering.ItemBatchedRenderer;
import net.puffish.skillsmod.client.rendering.TextureBatchedRenderer;
import net.puffish.skillsmod.skill.SkillState;
import net.puffish.skillsmod.utils.Bounds2i;
import net.puffish.skillsmod.utils.Vec2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class SkillsScreen extends Screen {
	private static final Identifier TABS_TEXTURE = new Identifier("textures/gui/advancements/tabs.png");
	private static final Identifier WINDOW_TEXTURE = new Identifier("textures/gui/advancements/window.png");
	private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/advancements/widgets.png");

	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	private static final int FRAME_WIDTH = 252;
	private static final int FRAME_HEIGHT = 140;
	private static final int FRAME_PADDING = 8;
	private static final int FRAME_CUT = 16;
	private static final int FRAME_EXPAND = 24;
	private static final int CONTENT_GROW = 32;
	private static final int TABS_HEIGHT = 28;

	private static final int LINE_WIDTH = 170;

	private static final int HALF_FRAME_WIDTH = FRAME_WIDTH / 2;
	private static final int HALF_FRAME_HEIGHT = FRAME_HEIGHT / 2;

	private static final Vector4f COLOR_WHITE = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector4f COLOR_GRAY = new Vector4f(0.25f, 0.25f, 0.25f, 1f);

	private final Map<Identifier, ClientSkillCategoryData> categories;

	private Optional<ClientSkillCategoryData> optActiveCategory = Optional.empty();

	private int activeCategoryIndex = 0;

	private float minScale = 1f;
	private float maxScale = 1f;
	private float scale = 1;

	private int x = 0;
	private int y = 0;

	private double dragStartX = 0;
	private double dragStartY = 0;
	private boolean dragging = false;

	private Bounds2i bounds = Bounds2i.zero();
	private boolean small = false;

	private int contentPaddingTop = 0;
	private int contentPaddingLeft = 0;
	private int contentPaddingRight = 0;
	private int contentPaddingBottom = 0;

	private List<OrderedText> tooltip;

	public SkillsScreen(Map<Identifier, ClientSkillCategoryData> categories) {
		super(ScreenTexts.EMPTY);
		this.categories = categories;
	}

	@Override
	protected void init() {
		super.init();
	}

	private void resize() {
		this.small = optActiveCategory
				.map(activeCategory -> activeCategory.hasExperience() && this.width < 450)
				.orElse(false);

		if (this.small) {
			contentPaddingTop = 62;
			contentPaddingLeft = 17;
			contentPaddingRight = 17;
			contentPaddingBottom = 17;
		} else {
			contentPaddingTop = 54;
			contentPaddingLeft = 17;
			contentPaddingRight = 17;
			contentPaddingBottom = 17;
		}

		this.x = this.width / 2;
		this.y = this.height / 2;

		this.bounds = optActiveCategory
				.map(ClientSkillCategoryData::getBounds)
				.orElseGet(Bounds2i::zero);
		this.bounds.grow(CONTENT_GROW);
		this.bounds.extend(new Vec2i(contentPaddingLeft - this.x, contentPaddingTop - this.y));
		this.bounds.extend(new Vec2i(this.width - this.x - contentPaddingRight, this.height - this.y - contentPaddingBottom));

		var contentWidth = this.width - contentPaddingLeft - contentPaddingRight;
		var contentHeight = this.height - contentPaddingTop - contentPaddingBottom;

		var halfWidth = MathHelper.ceilDiv(this.bounds.height() * contentWidth, contentHeight * 2);
		var halfHeight = MathHelper.ceilDiv(this.bounds.width() * contentHeight, contentWidth * 2);

		this.bounds.extend(new Vec2i(-halfWidth, -halfHeight));
		this.bounds.extend(new Vec2i(halfWidth, halfHeight));

		this.minScale = Math.max(
				((float) contentWidth) / ((float) this.bounds.width()),
				((float) contentHeight) / ((float) this.bounds.height())
		);
		this.maxScale = 1f;
		this.scale = 1f;
	}

	private Vec2i getMousePos(double mouseX, double mouseY) {
		return new Vec2i(
				(int) mouseX,
				(int) mouseY
		);
	}

	private Vec2i getTransformedMousePos(double mouseX, double mouseY) {
		return new Vec2i(
				(int) Math.round((mouseX - x) / scale),
				(int) Math.round((mouseY - y) / scale)
		);
	}

	private boolean isInsideTab(Vec2i mouse, int i) {
		return mouse.x >= FRAME_PADDING + i * 32 && mouse.y >= FRAME_PADDING && mouse.x < FRAME_PADDING + i * 32 + 28 && mouse.y < FRAME_PADDING + 32;
	}

	private boolean isInsideSkill(Vec2i transformedMouse, ClientSkillData skill, ClientSkillDefinitionData definition) {
		var halfSize = Math.round(13f * definition.getSize());
		return transformedMouse.x >= skill.getX() - halfSize && transformedMouse.y >= skill.getY() - halfSize && transformedMouse.x < skill.getX() + halfSize && transformedMouse.y < skill.getY() + halfSize;
	}

	private boolean isInsideContent(Vec2i mouse) {
		return mouse.x >= contentPaddingLeft && mouse.y >= contentPaddingTop && mouse.x < width - contentPaddingRight && mouse.y < height - contentPaddingBottom;
	}

	private boolean isInsideExperience(Vec2i mouse, int x, int y) {
		return mouse.x >= x && mouse.y >= y && mouse.x < x + 182 && mouse.y < y + 5;
	}

	private boolean isInsideArea(Vec2i mouse, int x1, int y1, int x2, int y2) {
		return mouse.x >= x1 && mouse.y >= y1 && mouse.x < x2 && mouse.y < y2;
	}

	private void syncCategory() {
		var opt = categories.values().stream().skip(activeCategoryIndex).findFirst();
		if (!Objects.equals(opt, optActiveCategory)) {
			optActiveCategory = opt;
			resize();
		}
	}

	private void forEachCategory(BiConsumer<Integer, ClientSkillCategoryData> consumer) {
		var it = categories.values().iterator();
		var i = 0;
		while (it.hasNext()) {
			consumer.accept(i, it.next());
			i++;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		optActiveCategory.ifPresent(activeCategory ->
				mouseClickedWithCategory(mouseX, mouseY, button, activeCategory)
		);

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void mouseClickedWithCategory(double mouseX, double mouseY, int button, ClientSkillCategoryData activeCategory) {
		var mouse = getMousePos(mouseX, mouseY);
		var transformedMouse = getTransformedMousePos(mouseX, mouseY);

		if (isInsideContent(mouse)) {
			for (var skill : activeCategory.getSkills().values()) {
				var definition = activeCategory.getDefinitions().get(skill.getDefinitionId());
				if (definition == null) {
					continue;
				}

				if (isInsideSkill(transformedMouse, skill, definition)) {
					SkillsClientMod.getInstance()
							.getPacketSender()
							.send(SkillClickOutPacket.write(activeCategory.getId(), skill.getId()));
				}
			}

			if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
				dragStartX = mouseX - x;
				dragStartY = mouseY - y;
				dragging = true;
			}
		} else {
			dragging = false;
		}

		forEachCategory((i, category) -> {
			if (isInsideTab(mouse, i)) {
				activeCategoryIndex = i;
				syncCategory();
			}
		});
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (SkillsClientMod.OPEN_KEY_BINDING.matchesKey(keyCode, scanCode)) {
			this.close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.syncCategory();

		tooltip = null;

		this.renderBackground(matrices);
		this.drawContent(matrices, mouseX, mouseY);
		this.drawWindow(matrices, mouseX, mouseY);
		this.drawTabs(matrices, mouseX, mouseY);

		if (tooltip != null) {
			renderOrderedTooltip(matrices, tooltip, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
				x = (int) Math.round(mouseX - dragStartX);
				y = (int) Math.round(mouseY - dragStartY);

				limitPosition();

				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		float factor = (float) Math.pow(2, amount * 0.25);

		scale *= factor;

		if (scale < minScale) {
			scale = minScale;
			factor = minScale / scale;
		}
		if (scale > maxScale) {
			scale = maxScale;
			factor = maxScale / scale;
		}

		x -= (int) Math.round((factor - 1f) * (mouseX - x));
		y -= (int) Math.round((factor - 1f) * (mouseY - y));

		limitPosition();

		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	private void limitPosition() {
		y = Math.min(y, Math.round(contentPaddingTop - bounds.min().y * scale));
		x = Math.min(x, Math.round(contentPaddingLeft - bounds.min().x * scale));
		x = Math.max(x, Math.round(width - contentPaddingRight - bounds.max().x * scale));
		y = Math.max(y, Math.round(height - contentPaddingBottom - bounds.max().y * scale));
	}

	private void drawIcon(MatrixStack matrices, TextureBatchedRenderer textureRenderer, ItemBatchedRenderer itemRenderer, ClientIconData icon, float sizeScale, int x, int y) {
		if (client == null) {
			return;
		}

		matrices.push();

		if (icon instanceof ClientIconData.ItemIconData itemIcon) {
			matrices.translate(x * (1f - sizeScale), y * (1f - sizeScale), 1f);
			matrices.scale(sizeScale, sizeScale, 1);
			itemRenderer.emitItem(
					matrices,
					itemIcon.getItem(),
					x, y
			);
		} else if (icon instanceof ClientIconData.EffectIconData effectIcon) {
			matrices.translate(0f, 0f, 1f);
			var sprite = client.getStatusEffectSpriteManager().getSprite(effectIcon.getEffect());
			int halfSize = Math.round(9f * sizeScale);
			var size = halfSize * 2;
			textureRenderer.emitSpriteStretch(
					matrices, sprite,
					x - halfSize, y - halfSize, size, size,
					COLOR_WHITE
			);
		} else if (icon instanceof ClientIconData.TextureIconData textureIcon) {
			matrices.translate(0f, 0f, 1f);
			var halfSize = Math.round(8f * sizeScale);
			var size = halfSize * 2;
			textureRenderer.emitTexture(
					matrices, textureIcon.getTexture(),
					x - halfSize, y - halfSize, size, size,
					COLOR_WHITE
			);
		}

		matrices.pop();
	}

	private void drawFrame(MatrixStack matrices, TextureBatchedRenderer textureRenderer, ClientFrameData frame, float sizeScale, int x, int y, SkillState state) {
		if (client == null) {
			return;
		}

		var halfSize = Math.round(13f * sizeScale);
		var size = halfSize * 2;

		if (frame instanceof ClientFrameData.AdvancementFrameData advancementFrame) {
			var status = state == SkillState.UNLOCKED ? AdvancementObtainedStatus.OBTAINED : AdvancementObtainedStatus.UNOBTAINED;
			var color = switch (state) {
				case LOCKED, EXCLUDED -> COLOR_GRAY;
				case AVAILABLE, UNLOCKED -> COLOR_WHITE;
			};

			textureRenderer.emitTexture(
					matrices, WIDGETS_TEXTURE,
					x - halfSize, y - halfSize, size, size,
					(float) advancementFrame.getFrame().getTextureV() / TEXTURE_WIDTH,
					(float) (128 + status.getSpriteIndex() * 26) / TEXTURE_HEIGHT,
					(float) (advancementFrame.getFrame().getTextureV() + 26) / TEXTURE_WIDTH,
					(float) (128 + status.getSpriteIndex() * 26 + 26) / TEXTURE_HEIGHT,
					color
			);
		} else if (frame instanceof ClientFrameData.TextureFrameData textureFrame) {
			switch (state) {
				case AVAILABLE -> textureRenderer.emitTexture(
						matrices, textureFrame.getAvailableTexture(),
						x - halfSize, y - halfSize, size, size,
						COLOR_WHITE
				);
				case UNLOCKED -> textureRenderer.emitTexture(
						matrices, textureFrame.getUnlockedTexture(),
						x - halfSize, y - halfSize, size, size,
						COLOR_WHITE
				);
				case LOCKED -> {
					if (textureFrame.getLockedTexture() != null) {
						textureRenderer.emitTexture(
								matrices, textureFrame.getLockedTexture(),
								x - halfSize, y - halfSize, size, size,
								COLOR_WHITE
						);
					} else {
						textureRenderer.emitTexture(
								matrices, textureFrame.getAvailableTexture(),
								x - halfSize, y - halfSize, size, size,
								COLOR_GRAY
						);
					}
				}
				case EXCLUDED -> {
					if (textureFrame.getExcludedTexture() != null) {
						textureRenderer.emitTexture(
								matrices, textureFrame.getExcludedTexture(),
								x - halfSize, y - halfSize, size, size,
								COLOR_WHITE
						);
					} else {
						textureRenderer.emitTexture(
								matrices, textureFrame.getAvailableTexture(),
								x - halfSize, y - halfSize, size, size,
								COLOR_GRAY
						);
					}
				}
				default -> throw new UnsupportedOperationException();
			}
		}
	}

	private void drawContent(MatrixStack matrices, double mouseX, double mouseY) {
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();

		DrawableHelper.enableScissor(
				contentPaddingLeft - 4,
				contentPaddingTop - 4,
				this.width - contentPaddingRight + 4,
				this.height - contentPaddingBottom + 4
		);

		optActiveCategory.ifPresentOrElse(
				activeCategory -> drawContentWithCategory(matrices, mouseX, mouseY, activeCategory),
				() -> drawContentWithoutCategory(matrices)
		);

		DrawableHelper.disableScissor();
	}

	private void drawContentWithCategory(MatrixStack matrices, double mouseX, double mouseY, ClientSkillCategoryData activeCategory) {
		if (client == null) {
			return;
		}

		var mouse = getMousePos(mouseX, mouseY);
		var transformedMouse = getTransformedMousePos(mouseX, mouseY);

		matrices.push();

		matrices.translate(x, y, 0f);
		matrices.scale(scale, scale, 1f);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.setShaderTexture(0, activeCategory.getBackground());
		DrawableHelper.drawTexture(
				matrices,
				bounds.min().x,
				bounds.min().y,
				0,
				0,
				bounds.width(),
				bounds.height(),
				16,
				16
		);

		var connectionRenderer = new ConnectionBatchedRenderer();

		for (var connection : activeCategory.getNormalConnections()) {
			var skillA = activeCategory.getSkills().get(connection.getSkillAId());
			var skillB = activeCategory.getSkills().get(connection.getSkillBId());
			if (skillA != null && skillB != null) {
				connectionRenderer.emitNormalConnection(
						matrices,
						skillA.getX(),
						skillA.getY(),
						skillB.getX(),
						skillB.getY(),
						connection.isBidirectional()
				);
			}
		}

		if (isInsideContent(mouse)) {
			var optHoveredSkill = activeCategory
					.getSkills()
					.values()
					.stream()
					.filter(skill -> {
						var definition = activeCategory.getDefinitions().get(skill.getDefinitionId());
						if (definition == null) {
							return false;
						}

						return isInsideSkill(transformedMouse, skill, definition);
					})
					.findFirst();

			optHoveredSkill.ifPresent(hoveredSkill -> {
				var definition = activeCategory.getDefinitions().get(hoveredSkill.getDefinitionId());
				if (definition == null) {
					return;
				}

				var lines = new ArrayList<OrderedText>();
				lines.add(definition.getTitle().asOrderedText());
				lines.addAll(textRenderer.wrapLines(Texts.setStyleIfAbsent(definition.getDescription().copy(), Style.EMPTY.withFormatting(Formatting.GRAY)), LINE_WIDTH));
				if (Screen.hasShiftDown()) {
					lines.addAll(textRenderer.wrapLines(Texts.setStyleIfAbsent(definition.getExtraDescription().copy(), Style.EMPTY.withFormatting(Formatting.GRAY)), LINE_WIDTH));
				}
				if (client.options.advancedItemTooltips) {
					lines.add(Text.literal(hoveredSkill.getId()).formatted(Formatting.DARK_GRAY).asOrderedText());
				}
				tooltip = lines;

				var connections = activeCategory.getExclusiveConnections().get(hoveredSkill.getId());
				if (connections != null) {
					for (var connection : connections) {
						var skillA = activeCategory.getSkills().get(connection.getSkillAId());
						var skillB = activeCategory.getSkills().get(connection.getSkillBId());
						if (skillA != null && skillB != null) {
							connectionRenderer.emitExclusiveConnection(
									matrices,
									skillA.getX(),
									skillA.getY(),
									skillB.getX(),
									skillB.getY(),
									connection.isBidirectional()
							);
						}
					}
				}
			});
		}

		connectionRenderer.draw();

		var textureRenderer = new TextureBatchedRenderer();
		var itemRenderer = new ItemBatchedRenderer();

		for (var skill : activeCategory.getSkills().values()) {
			var definition = activeCategory.getDefinitions().get(skill.getDefinitionId());
			if (definition == null) {
				continue;
			}

			drawFrame(
					matrices,
					textureRenderer,
					definition.getFrame(),
					definition.getSize(),
					skill.getX(),
					skill.getY(),
					skill.getState()
			);

			drawIcon(
					matrices,
					textureRenderer,
					itemRenderer,
					definition.getIcon(),
					definition.getSize(),
					skill.getX(),
					skill.getY()
			);
		}

		textureRenderer.draw();
		itemRenderer.draw();

		matrices.pop();
	}

	private void drawContentWithoutCategory(MatrixStack matrices) {
		DrawableHelper.fill(matrices, 0, 0, width, height, 0xff000000);

		var tmpX = contentPaddingLeft + (width - contentPaddingLeft - contentPaddingRight) / 2;

		DrawableHelper.drawCenteredText(
				matrices,
				this.textRenderer,
				Text.translatable("advancements.sad_label"),
				tmpX,
				height - contentPaddingBottom - this.textRenderer.fontHeight,
				0xffffffff
		);
		DrawableHelper.drawCenteredText(
				matrices,
				this.textRenderer,
				Text.translatable("advancements.empty"),
				tmpX,
				contentPaddingTop + (height - contentPaddingTop - contentPaddingBottom - this.textRenderer.fontHeight) / 2,
				0xffffffff
		);
	}

	private void drawTabs(MatrixStack matrices, double mouseX, double mouseY) {
		if (client == null) {
			return;
		}

		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.disableDepthTest();
		RenderSystem.setShaderTexture(0, TABS_TEXTURE);

		forEachCategory((i, category) -> DrawableHelper.drawTexture(
				matrices,
				FRAME_PADDING + 32 * i,
				FRAME_PADDING,
				i > 0 ? 28 : 0,
				optActiveCategory.orElse(null) == category ? 32 : 0,
				28,
				32,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		));

		var mouse = getMousePos(mouseX, mouseY);

		var textureRenderer = new TextureBatchedRenderer();
		var itemBatch = new ItemBatchedRenderer();

		forEachCategory((i, category) -> {
			drawIcon(
					matrices,
					textureRenderer,
					itemBatch,
					category.getIcon(),
					1f,
					FRAME_PADDING + 32 * i + 6 + 8,
					FRAME_PADDING + 9 + 8
			);

			if (isInsideTab(mouse, i)) {
				var lines = new ArrayList<OrderedText>();
				lines.add(category.getTitle().asOrderedText());
				if (client.options.advancedItemTooltips) {
					lines.add(Text.literal(category.getId().toString()).formatted(Formatting.DARK_GRAY).asOrderedText());
				}
				tooltip = lines;
			}
		});

		textureRenderer.draw();
		itemBatch.draw();
	}

	private void drawWindow(MatrixStack matrices, double mouseX, double mouseY) {
		if (client == null) {
			return;
		}

		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.disableDepthTest();
		RenderSystem.setShaderTexture(0, WINDOW_TEXTURE);

		// bottom left
		DrawableHelper.drawTexture(
				matrices,
				FRAME_PADDING,
				this.height - FRAME_PADDING - HALF_FRAME_HEIGHT + 1,
				0,
				HALF_FRAME_HEIGHT,
				HALF_FRAME_WIDTH,
				HALF_FRAME_HEIGHT,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);

		// bottom right
		DrawableHelper.drawTexture(
				matrices,
				this.width - FRAME_PADDING - HALF_FRAME_WIDTH + 1,
				this.height - FRAME_PADDING - HALF_FRAME_HEIGHT + 1,
				HALF_FRAME_WIDTH,
				HALF_FRAME_HEIGHT,
				HALF_FRAME_WIDTH,
				HALF_FRAME_HEIGHT,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);

		// left
		DrawableHelper.drawTexture(
				matrices,
				FRAME_PADDING,
				FRAME_PADDING + HALF_FRAME_HEIGHT,
				HALF_FRAME_WIDTH,
				this.height - FRAME_PADDING * 2 - FRAME_HEIGHT + 1,
				0,
				HALF_FRAME_HEIGHT - 1,
				HALF_FRAME_WIDTH,
				2,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);

		// bottom
		DrawableHelper.drawTexture(
				matrices,
				FRAME_PADDING + HALF_FRAME_WIDTH,
				this.height - FRAME_PADDING - HALF_FRAME_HEIGHT + 1,
				this.width - FRAME_PADDING * 2 - FRAME_WIDTH + 1,
				HALF_FRAME_HEIGHT,
				HALF_FRAME_WIDTH - 1,
				HALF_FRAME_HEIGHT,
				2,
				HALF_FRAME_HEIGHT,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);

		// right
		DrawableHelper.drawTexture(
				matrices,
				this.width - FRAME_PADDING - HALF_FRAME_WIDTH + 1,
				FRAME_PADDING + HALF_FRAME_HEIGHT,
				HALF_FRAME_WIDTH,
				this.height - FRAME_PADDING * 2 - FRAME_HEIGHT + 1,
				HALF_FRAME_WIDTH,
				HALF_FRAME_HEIGHT - 1,
				HALF_FRAME_WIDTH,
				2,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);

		if (small) {
			// top left
			DrawableHelper.drawTexture(
					matrices,
					FRAME_PADDING,
					FRAME_PADDING + TABS_HEIGHT,
					0,
					0,
					HALF_FRAME_WIDTH,
					FRAME_CUT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);
			DrawableHelper.drawTexture(
					matrices,
					FRAME_PADDING,
					FRAME_PADDING + TABS_HEIGHT + FRAME_CUT,
					0,
					FRAME_CUT * 2 - FRAME_EXPAND,
					HALF_FRAME_WIDTH,
					HALF_FRAME_HEIGHT - TABS_HEIGHT - FRAME_CUT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);

			// top right
			DrawableHelper.drawTexture(
					matrices,
					this.width - FRAME_PADDING - HALF_FRAME_WIDTH + 1,
					FRAME_PADDING + TABS_HEIGHT,
					HALF_FRAME_WIDTH,
					0,
					HALF_FRAME_WIDTH,
					FRAME_CUT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);
			DrawableHelper.drawTexture(
					matrices,
					this.width - FRAME_PADDING - HALF_FRAME_WIDTH + 1,
					FRAME_PADDING + TABS_HEIGHT + FRAME_CUT,
					HALF_FRAME_WIDTH,
					FRAME_CUT * 2 - FRAME_EXPAND,
					HALF_FRAME_WIDTH,
					HALF_FRAME_HEIGHT - TABS_HEIGHT - FRAME_CUT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);

			// top
			DrawableHelper.drawTexture(
					matrices,
					FRAME_PADDING + HALF_FRAME_WIDTH,
					FRAME_PADDING + TABS_HEIGHT,
					this.width - FRAME_PADDING * 2 - FRAME_WIDTH + 1,
					FRAME_CUT,
					HALF_FRAME_WIDTH - 1,
					0,
					2,
					FRAME_CUT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);
			DrawableHelper.drawTexture(
					matrices,
					FRAME_PADDING + HALF_FRAME_WIDTH,
					FRAME_PADDING + TABS_HEIGHT + FRAME_CUT,
					this.width - FRAME_PADDING * 2 - FRAME_WIDTH + 1,
					HALF_FRAME_HEIGHT - FRAME_CUT,
					HALF_FRAME_WIDTH - 1,
					FRAME_CUT * 2 - FRAME_EXPAND,
					2,
					HALF_FRAME_HEIGHT - FRAME_CUT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);
		} else {
			// top left
			DrawableHelper.drawTexture(
					matrices,
					FRAME_PADDING,
					FRAME_PADDING + TABS_HEIGHT,
					0,
					0,
					HALF_FRAME_WIDTH,
					HALF_FRAME_HEIGHT - TABS_HEIGHT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);

			// top right
			DrawableHelper.drawTexture(
					matrices,
					this.width - FRAME_PADDING - HALF_FRAME_WIDTH + 1,
					FRAME_PADDING + TABS_HEIGHT,
					HALF_FRAME_WIDTH,
					0,
					HALF_FRAME_WIDTH,
					HALF_FRAME_HEIGHT - TABS_HEIGHT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);

			// top
			DrawableHelper.drawTexture(
					matrices,
					FRAME_PADDING + HALF_FRAME_WIDTH,
					FRAME_PADDING + TABS_HEIGHT,
					this.width - FRAME_PADDING * 2 - FRAME_WIDTH + 1,
					HALF_FRAME_HEIGHT,
					HALF_FRAME_WIDTH - 1,
					0,
					2,
					HALF_FRAME_HEIGHT,
					TEXTURE_WIDTH,
					TEXTURE_HEIGHT
			);
		}

		var tmpText = SkillsMod.createTranslatable("text", "skills");
		var tmpX = FRAME_PADDING + 8;
		var tmpY = FRAME_PADDING + TABS_HEIGHT + 6;

		this.textRenderer.draw(
				matrices,
				tmpText,
				tmpX,
				tmpY,
				0xff404040
		);

		optActiveCategory.ifPresent(activeCategory ->
				drawWindowWithCategory(matrices, mouseX, mouseY, tmpText, tmpX, tmpY, activeCategory)
		);
	}

	private void drawWindowWithCategory(MatrixStack matrices, double mouseX, double mouseY, Text tmpText, int tmpX, int tmpY, ClientSkillCategoryData activeCategory) {
		var mouse = getMousePos(mouseX, mouseY);

		var leftX = tmpX + this.textRenderer.getWidth(tmpText);

		tmpX = this.width - FRAME_PADDING - 7;

		var startX = tmpX;

		tmpText = Text.literal(activeCategory.getPointsLeft()
				+ (activeCategory.getSpentPointsLimit() == Integer.MAX_VALUE ? "" : "/" + activeCategory.getSpentPointsLeft())
		);

		tmpX -= this.textRenderer.getWidth(tmpText);
		tmpX -= 1;

		this.textRenderer.draw(matrices, tmpText, tmpX - 1, tmpY, 0xff000000);
		this.textRenderer.draw(matrices, tmpText, tmpX, tmpY - 1, 0xff000000);
		this.textRenderer.draw(matrices, tmpText, tmpX + 1, tmpY, 0xff000000);
		this.textRenderer.draw(matrices, tmpText, tmpX, tmpY + 1, 0xff000000);
		this.textRenderer.draw(matrices, tmpText, tmpX, tmpY, 0xff80ff20);
		tmpX -= 1;

		tmpText = SkillsMod.createTranslatable("text", "points_left");
		tmpX -= this.textRenderer.getWidth(tmpText);
		this.textRenderer.draw(
				matrices,
				tmpText,
				tmpX,
				tmpY,
				0xff404040
		);

		if (isInsideArea(mouse, tmpX, tmpY, startX, tmpY + this.textRenderer.fontHeight)) {
			var lines = new ArrayList<OrderedText>();

			lines.add(SkillsMod.createTranslatable(
					"tooltip",
					"earned_points",
					activeCategory.getEarnedPoints()
			).asOrderedText());
			lines.add(SkillsMod.createTranslatable(
					"tooltip",
					"spent_points",
					activeCategory.getSpentPoints()
							+ (activeCategory.getSpentPointsLimit() == Integer.MAX_VALUE ? "" : "/" + activeCategory.getSpentPointsLimit())
			).asOrderedText());

			tooltip = lines;
		}

		var rightX = tmpX;

		if (activeCategory.hasExperience()) {
			if (small) {
				tmpX = this.width - FRAME_PADDING - 8 - 182;
				tmpY = TABS_HEIGHT + 25;
			} else {
				tmpX = (leftX + rightX - 182) / 2;
				tmpY = TABS_HEIGHT + 15;
			}

			RenderSystem.setShaderTexture(0, InGameHud.GUI_ICONS_TEXTURE);
			drawTexture(matrices, tmpX, tmpY, 0, 64, 182, 5);
			var width = Math.min(182, (int) (activeCategory.getExperienceProgress() * 183f));
			if (width > 0) {
				drawTexture(matrices, tmpX, tmpY, 0, 69, width, 5);
			}

			if (isInsideExperience(mouse, tmpX, tmpY)) {
				var lines = new ArrayList<OrderedText>();

				lines.add(SkillsMod.createTranslatable(
						"tooltip",
						"current_level",
						activeCategory.getCurrentLevel()
				).asOrderedText());
				lines.add(SkillsMod.createTranslatable(
						"tooltip",
						"experience_progress",
						activeCategory.getCurrentExperience(),
						activeCategory.getRequiredExperience(),
						MathHelper.floor(activeCategory.getExperienceProgress() * 100f)
				).asOrderedText());
				lines.add(SkillsMod.createTranslatable(
						"tooltip",
						"to_next_level",
						activeCategory.getExperienceToNextLevel()
				).asOrderedText());

				tooltip = lines;
			}
		}
	}

}
