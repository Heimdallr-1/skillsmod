package net.puffish.skillsmod.access;

import net.minecraft.util.math.Matrix4f;

import java.util.List;

public interface RenderLayerAccess {
	void setEmits(List<Matrix4f> emits);
}
