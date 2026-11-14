package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

import jogo.util.ProcTextures;

public abstract class VoxelBlockType {
    private final String name;

    protected VoxelBlockType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Whether this block is physically solid (collides/occludes). */
    public boolean isSolid() { return true; }

    /**
     * Retorna o Material para este bloco.
     * Procura automaticamente por Textures/<name>.png.
     * Se não existir, recorre ao método de fallback getDefaultTexture().
     */
    public Material getMaterial(AssetManager assetManager) {
        Texture tex;
        String texturePath = "Textures/" + getName().toLowerCase() + ".png";

        try {
            tex = assetManager.loadTexture(texturePath);
            // Filtro pixel-art (evita borrão em texturas estilo Minecraft)
            tex.setMagFilter(Texture.MagFilter.Nearest);
            tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        } catch (AssetNotFoundException e) {
            // Se o PNG não existir, pede a textura de fallback do bloco
            tex = getDefaultTexture();
        }

        if (tex == null) {
            return null;
        }

        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White.mult(0.02f));
        m.setFloat("Shininess", 32f);
        return m;
    }

    /**
     * Textura de recurso caso o PNG não exista.
     * Pode ser sobrescrito por cada tipo de bloco.
     */
    protected Texture getDefaultTexture() {
        // Fallback genérico xadrez acinzentado
        return ProcTextures.checker(128, 4, ColorRGBA.Gray, ColorRGBA.DarkGray);
    }

    public Material getMaterial(AssetManager assetManager, jogo.framework.math.Vec3 blockPos) {
        return getMaterial(assetManager);
    }
}