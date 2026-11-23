package jogo.voxel;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

import jogo.util.ProcTextures;

public abstract class VoxelBlockType {
    private final String name;
    private final boolean multiTexture;

    protected VoxelBlockType(String name) {
        this(name, false);
    }

    protected VoxelBlockType(String name, boolean multiTexture) {
        this.name = name;
        this.multiTexture = multiTexture;
    }

    public String getName() {
        return name;
    }

    /** Whether this block is physically solid (collides/occludes). */
    public boolean isSolid() { return true; }

    public Material getMaterial(AssetManager assetManager, MeshBuilder.Face face) {
        if (!multiTexture) {
            return getMaterial(assetManager);
        }

        String base = "Textures/" + getName().toLowerCase();

        String chosenPath = switch (face) {
            case PY -> firstExisting(assetManager, base + "_top.png", base + ".png");
            case NY -> firstExisting(assetManager, base + "_bottom.png", base + "_top.png", base + ".png");
            case PZ -> firstExisting(assetManager, base + "_front.png", base + "_side.png", base + ".png");
            case PX, NX, NZ -> firstExisting(assetManager, base + "_side.png", base + ".png");
        };

        return loadBlockMaterial(assetManager, chosenPath);
    }

    public Material getMaterial(AssetManager assetManager) {
        return loadBlockMaterial(assetManager, "Textures/" + getName().toLowerCase() + ".png");
    }

    private String firstExisting(AssetManager assetManager, String... paths) {
        for (String path : paths) {
            if (assetManager.locateAsset(new AssetKey<>(path)) != null) {
                return path;
            }
        }
        return paths[paths.length - 1];
    }

    protected Material loadBlockMaterial(AssetManager assetManager, String texturePath) {
        Texture tex;
        try {
            tex = assetManager.loadTexture(texturePath);
            tex.setMagFilter(Texture.MagFilter.Nearest);
            tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        } catch (AssetNotFoundException e) {
            tex = getDefaultTexture();
        }

        if (tex == null) {
            return null;
        }

        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setFloat("Shininess", 0f);
        m.setColor("Specular", ColorRGBA.White.mult(0.02f));
        return m;
    }

    protected Texture getDefaultTexture() {
        // Fallback genérico xadrez acinzentado
        return ProcTextures.checker(128, 4, ColorRGBA.Gray, ColorRGBA.DarkGray);
    }

    public Material getMaterial(AssetManager assetManager, jogo.framework.math.Vec3 blockPos) {
        return getMaterial(assetManager);
    }
}