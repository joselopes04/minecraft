package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import jogo.voxel.VoxelBlockType;

public abstract class LiquidBlockType extends VoxelBlockType {
    private final float opacity;

    protected LiquidBlockType(String name, float opacity) {
        super(name, false);
        this.opacity = opacity;
    }

    @Override
    public boolean isSolid() {
        return false; // O jogador atravessa o líquido (sem colisão física)
    }

    public boolean isLiquid() {
        return true;
    }

    @Override
    protected Material loadBlockMaterial(AssetManager assetManager, String texturePath) {
        Material m = super.loadBlockMaterial(assetManager, texturePath);
        if (m != null && opacity < 1.0f) {
            // Ativa transparência (Alpha Blending) para a água
            m.setColor("Diffuse", new ColorRGBA(1f, 1f, 1f, opacity));
            m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            m.getAdditionalRenderState().setDepthWrite(false);
        }
        return m;
    }

    public boolean isTransparent() {
        return opacity < 1.0f;
    }
}