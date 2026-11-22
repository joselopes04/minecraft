package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import jogo.voxel.blocks.LiquidBlockType;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a chunk of the voxel world (e.g., 16x16x16 blocks).
 */
public class Chunk {
    public static final int SIZE = 16;
    private final int chunkX, chunkY, chunkZ;
    private final byte[][][] vox;
    private final Node node;
    private final Node solidNode = new Node("Solids");
    private final Node liquidNode = new Node("Liquids");

    private boolean dirty = true;

    private RigidBodyControl rigidBody;

    private record MeshKey(byte blockId, MeshBuilder.Face face) {}

    public Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.vox = new byte[SIZE][SIZE][SIZE];
        this.node = new Node("Chunk_" + chunkX + "," + chunkY + "," + chunkZ);
        node.attachChild(solidNode);
        node.attachChild(liquidNode);
    }

    public Node getNode() { return node; }
    public byte get(int x, int y, int z) { return vox[x][y][z]; }
    public void set(int x, int y, int z, byte id) { vox[x][y][z] = id; }
    public int getChunkX() { return chunkX; }
    public int getChunkY() { return chunkY; }
    public int getChunkZ() { return chunkZ; }

    public void markDirty() { dirty = true; }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    // Build and attach mesh for this chunk
    public void buildMesh(AssetManager assetManager, VoxelPalette palette) {
        long start = System.nanoTime();
        solidNode.detachAllChildren();
        liquidNode.detachAllChildren();
        Map<MeshKey, MeshBuilder> builders = new HashMap<>();

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = vox[x][y][z];
                    if (id == VoxelPalette.AIR_ID) continue;

                    VoxelBlockType type = palette.get(id);
                    boolean isLiquid = type instanceof LiquidBlockType;
                    if (!type.isSolid() && !isLiquid) continue;

                    int wx = chunkX * SIZE + x;
                    int wy = chunkY * SIZE + y;
                    int wz = chunkZ * SIZE + z;

                    // Only add faces if neighbor is air, transparent/liquid, or out of bounds
                    if (!shouldHideFace(wx + 1, wy, wz, id, isLiquid, palette)) addFace(builders, id, MeshBuilder.Face.PX, wx, wy, wz);
                    if (!shouldHideFace(wx - 1, wy, wz, id, isLiquid, palette)) addFace(builders, id, MeshBuilder.Face.NX, wx, wy, wz);
                    if (!shouldHideFace(wx, wy + 1, wz, id, isLiquid, palette)) addFace(builders, id, MeshBuilder.Face.PY, wx, wy, wz);
                    if (!shouldHideFace(wx, wy - 1, wz, id, isLiquid, palette)) addFace(builders, id, MeshBuilder.Face.NY, wx, wy, wz);
                    if (!shouldHideFace(wx, wy, wz + 1, id, isLiquid, palette)) addFace(builders, id, MeshBuilder.Face.PZ, wx, wy, wz);
                    if (!shouldHideFace(wx, wy, wz - 1, id, isLiquid, palette)) addFace(builders, id, MeshBuilder.Face.NZ, wx, wy, wz);
                }
            }
        }
        int geomCount = 0;
        for (Map.Entry<MeshKey, MeshBuilder> entry : builders.entrySet()) {
            MeshBuilder meshBuilder = entry.getValue();
            Mesh mesh = meshBuilder.build();
            if (mesh.getTriangleCount() > 0) {
                MeshKey key = entry.getKey();
                Geometry g = new Geometry("chunk_" + chunkX + "_" + chunkY + "_" + chunkZ + "_" + key.blockId + "_" + key.face, mesh);
                VoxelBlockType blockType = palette.get(key.blockId);
                Material mat = blockType.getMaterial(assetManager, key.face);
                g.setMaterial(mat);

                if (blockType instanceof LiquidBlockType liquid && liquid.isTransparent()) {
                    g.setQueueBucket(RenderQueue.Bucket.Transparent);
                }

                if (palette.get(key.blockId) instanceof LiquidBlockType) {
                    liquidNode.attachChild(g);
                } else {
                    solidNode.attachChild(g);
                }
                geomCount++;
            }
        }
        long end = System.nanoTime();
        System.out.println("Chunk [" + chunkX + "," + chunkY + "," + chunkZ + "] mesh built in " + ((end - start) / 1_000_000.0) + " ms, geometries: " + geomCount);
    }

    private void addFace(Map<MeshKey, MeshBuilder> builders, byte id, MeshBuilder.Face face, int wx, int wy, int wz) {
        builders.computeIfAbsent(new MeshKey(id, face), k -> new MeshBuilder())
                .addVoxelFace(wx, wy, wz, face);
    }

    /**
     * Updates the physics control for this chunk. Call after mesh rebuild.
     */
    public void updatePhysics(PhysicsSpace space) {
        if (rigidBody != null) {
            System.out.println("Removing old RigidBodyControl for chunk [" + chunkX + "," + chunkY + "," + chunkZ + "]");
            space.remove(rigidBody);
            node.removeControl(rigidBody);
            rigidBody = null;
        }
        if (solidNode.getQuantity() > 0) {
            if (node.getParent() == null) {
                System.out.println("Warning: Chunk node [" + chunkX + "," + chunkY + "," + chunkZ + "] not attached to world node before physics update!");
            }
            Node tempNode = new Node("TempPhysics");
            tempNode.setLocalTransform(node.getLocalTransform());
            for (int i = 0; i < solidNode.getQuantity(); i++) {
                if (solidNode.getChild(i) instanceof Geometry g) {
                    Geometry gClone = g.clone(false);
                    gClone.setMesh(g.getMesh().deepClone());
                    tempNode.attachChild(gClone);
                }
            }
            if (tempNode.getQuantity() > 0) {
                CollisionShape shape = CollisionShapeFactory.createMeshShape(tempNode);
                rigidBody = new RigidBodyControl(shape, 0f);
                node.addControl(rigidBody);
                space.add(rigidBody);
                System.out.println("Added new RigidBodyControl for chunk [" + chunkX + "," + chunkY + "," + chunkZ + "]");
            }
        }
    }

    // Helper to determine whether a block face should be hidden by its neighbor
    private boolean shouldHideFace(int wx, int wy, int wz, byte currentId, boolean currentIsLiquid, VoxelPalette palette) {
        if (wx < 0 || wy < 0 || wz < 0) return false;
        int max = SIZE * 1000; // arbitrary large world limit
        if (wx >= max || wy >= max || wz >= max) return false;
        int cx = wx / SIZE, cy = wy / SIZE, cz = wz / SIZE;
        int lx = wx % SIZE, ly = wy % SIZE, lz = wz % SIZE;
        if (cx != chunkX || cy != chunkY || cz != chunkZ) return false; // only check within this chunk
        if (lx < 0 || ly < 0 || lz < 0 || lx >= SIZE || ly >= SIZE || lz >= SIZE) return false;

        byte neighborId = vox[lx][ly][lz];
        if (neighborId == VoxelPalette.AIR_ID) return false;
        if (palette.get(neighborId).isSolid()) return true;
        return currentIsLiquid && neighborId == currentId;
    }
}