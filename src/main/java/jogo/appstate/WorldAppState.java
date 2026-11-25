package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import jogo.gameobject.character.Player;
import jogo.gameobject.item.Item;
import jogo.gameobject.item.StoneItem;
import jogo.voxel.TerrainGenerator;
import jogo.voxel.VoxelBlockType;
import jogo.voxel.VoxelWorld;
import jogo.voxel.blocks.StoneBlockType;

public class WorldAppState extends BaseAppState {

    private final Node rootNode;
    private final AssetManager assetManager;
    private final PhysicsSpace physicsSpace;
    private final Camera cam;
    private final InputAppState input;
    private PlayerAppState playerAppState;

    // world root for easy cleanup
    private Node worldNode;
    private VoxelWorld voxelWorld;
    private com.jme3.math.Vector3f spawnPosition;

    public WorldAppState(Node rootNode, AssetManager assetManager, PhysicsSpace physicsSpace, Camera cam, InputAppState input) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.physicsSpace = physicsSpace;
        this.cam = cam;
        this.input = input;
    }

    public void registerPlayerAppState(PlayerAppState playerAppState) {
        this.playerAppState = playerAppState;
    }

    @Override
    protected void initialize(Application app) {
        worldNode = new Node("World");
        rootNode.attachChild(worldNode);

        // Lighting
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.20f)); // slightly increased ambient
        worldNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -1.3f, -0.25f).normalizeLocal()); // more top-down to reduce harsh contrast
        sun.setColor(ColorRGBA.White.mult(0.85f)); // slightly dimmer sun
        worldNode.addLight(sun);

        // Voxel world criado com base no terrain generator
        TerrainGenerator generator = new TerrainGenerator("IscteCraft2026");
        voxelWorld = new VoxelWorld(assetManager, 320, 32, 320, generator);
        voxelWorld.generateLayers();
        voxelWorld.buildMeshes();
        voxelWorld.clearAllDirtyFlags();
        worldNode.attachChild(voxelWorld.getNode());
        voxelWorld.buildPhysics(physicsSpace);

        // compute recommended spawn
        spawnPosition = voxelWorld.getRecommendedSpawn();
    }

    public com.jme3.math.Vector3f getRecommendedSpawnPosition() {
        return spawnPosition != null ? spawnPosition.clone() : new com.jme3.math.Vector3f(25.5f, 12f, 25.5f);
    }

    public VoxelWorld getVoxelWorld() {
        return voxelWorld;
    }

    @Override
    public void update(float tpf) {
        if (input != null && input.isMouseCaptured() && input.consumeBreakRequested()) {
            var hit = voxelWorld.pickFirstSolid(cam, 6f); // reach distance ~6
            hit.ifPresent(cell -> {
                // Obtém o tipo do bloco ANTES de o partir, pois breakAt substitui-o por ar
                VoxelBlockType brokenType = voxelWorld.getBlockType(cell.x, cell.y, cell.z);
                if (voxelWorld.breakAt(cell.x, cell.y, cell.z)) {
                    voxelWorld.rebuildDirtyChunks(physicsSpace);
                    if (playerAppState != null) {
                        playerAppState.refreshPhysics();
                        // Se o bloco partido for pedra, converte-o no item "stone" e envia para o inventário
                        if (brokenType instanceof StoneBlockType && playerAppState.getPlayer() != null) {
                            playerAppState.getPlayer().collectItem(new StoneItem());
                        }
                    }
                }
            });
        }

        // Colocação de blocos com o botão direito do rato (estilo Minecraft)
        if (input != null && input.isMouseCaptured() && input.consumePlaceRequested()) {
            if (playerAppState != null && playerAppState.getPlayer() != null) {
                Player player = playerAppState.getPlayer();
                Item selected = player.getSelectedItem();
                if (selected != null && selected.getBlockId() >= 0 && selected.getQuantity() > 0) {
                    var target = voxelWorld.pickPlaceTarget(cam, 6f);
                    target.ifPresent(cell -> {
                        if (!overlapsPlayer(cell.x, cell.y, cell.z)
                                && voxelWorld.placeAt(cell.x, cell.y, cell.z, selected.getBlockId())) {
                            selected.addQuantity(-1);
                            // Remove a referência do slot quando a pilha chega a 0 para libertar o compartimento
                            if (selected.getQuantity() <= 0) {
                                player.getHotbar().setItem(player.getSelectedSlot(), null);
                            }
                            voxelWorld.rebuildDirtyChunks(physicsSpace);
                            playerAppState.refreshPhysics();
                        }
                    });
                }
            }
        }

        if (input != null && input.consumeToggleShadingRequested()) {
            voxelWorld.toggleRenderDebug();
        }
    }

    /**
     * Impede que o jogador coloque um bloco exatamente dentro do próprio corpo/cabeça,
     * comparando a caixa AABB do bloco [bx..bx+1] com o volume aproximado do jogador.
     */
    private boolean overlapsPlayer(int bx, int by, int bz) {
        Vector3f eye = cam.getLocation();
        float pMinX = eye.x - 0.35f, pMaxX = eye.x + 0.35f;
        float pMinY = eye.y - 1.65f, pMaxY = eye.y + 0.15f;
        float pMinZ = eye.z - 0.35f, pMaxZ = eye.z + 0.35f;

        return pMaxX > bx && pMinX < (bx + 1f)
                && pMaxY > by && pMinY < (by + 1f)
                && pMaxZ > bz && pMinZ < (bz + 1f);
    }

    @Override
    protected void cleanup(Application app) {
        if (worldNode != null) {
            // Remove all physics controls under worldNode
            worldNode.depthFirstTraversal(spatial -> {
                RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
                if (rbc != null) {
                    physicsSpace.remove(rbc);
                    spatial.removeControl(rbc);
                }
            });
            worldNode.removeFromParent();
            worldNode = null;
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    public byte getBlockAt(int x, int y, int z) {
        if (voxelWorld == null) return jogo.voxel.VoxelPalette.AIR_ID;
        return voxelWorld.getBlock(x, y, z);
    }
}