package net.puffish.skillsmod.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

public class SimpleToast implements Toast {
	private final SystemToast toast;

	private SimpleToast(SystemToast toast) {
		this.toast = toast;
	}

	public static SimpleToast create(MinecraftClient client, Text title, Text description) {
		return new SimpleToast(SystemToast.create(client, SystemToast.Type.PACK_LOAD_FAILURE, title, description));
	}

	@Override
	public Visibility getVisibility() {
		return toast.getVisibility();
	}

	@Override
	public void update(ToastManager manager, long time) {
		toast.update(manager, time);
	}

	@Override
	public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
		toast.draw(context, textRenderer, startTime);
	}

	@Override
	public int getWidth() {
		return toast.getWidth();
	}

	@Override
	public int getHeight() {
		return toast.getHeight();
	}

	@Override
	public int getRequiredSpaceCount() {
		return toast.getRequiredSpaceCount();
	}
}
