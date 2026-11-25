package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.collision.CollisionResults;
import jogo.engine.RenderIndex;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.Player;
import jogo.interaction.BlockPos;
import jogo.interaction.Interactable;
import jogo.interaction.InteractionContext;
import jogo.interaction.InteractionResult;
import jogo.voxel.VoxelBlockType;
import jogo.voxel.VoxelWorld;

/**
 * AppState responsável pela deteção e coordenação de interações (tecla 'E').
 * Unifica sob o mesmo contrato polimórfico (Interactable) tanto entidades físicas
 * (GameObject) como blocos da grelha voxel (VoxelBlockType).
 */
public class InteractionAppState extends BaseAppState {

    private final Node rootNode;
    private final Camera cam;
    private final InputAppState input;
    private final RenderIndex renderIndex;
    private final WorldAppState world;
    private final Player player;
    private float reach = 5.5f; // Alcance máximo do raio de interação em metros

    public InteractionAppState(Node rootNode, Camera cam, InputAppState input, RenderIndex renderIndex, WorldAppState world, Player player) {
        this.rootNode = rootNode;
        this.cam = cam;
        this.input = input;
        this.renderIndex = renderIndex;
        this.world = world;
        this.player = player;
    }

    public InteractionAppState(Node rootNode, Camera cam, InputAppState input, RenderIndex renderIndex, WorldAppState world) {
        this(rootNode, cam, input, renderIndex, world, null);
    }

    @Override
    protected void initialize(Application app) { }

    @Override
    public void update(float tpf) {
        // MECÂNICA 1: Fecho rápido de inventário
        // Se o jogador já tem um contentor aberto, pressionar 'E' fecha-o imediatamente.
        if (player != null && player.hasOpenContainer()) {
            if (input.consumeInteractRequested()) {
                player.closeContainer();
            }
            return;
        }

        // Se o rato não estiver capturado (e não há contentor), não processa interações no mundo
        if (!input.isMouseCaptured()) return;
        // Verifica se o jogador solicitou interação nesta frame
        if (!input.consumeInteractRequested()) return;

        Vector3f origin = cam.getLocation();
        Vector3f dir = cam.getDirection().normalize();
        VoxelWorld vw = world != null ? world.getVoxelWorld() : null;

        // MECÂNICA 2: Raycast em Entidades (GameObject)
        // Dispara um raio em linha de visão a partir da câmara.
        Ray ray = new Ray(origin, dir);
        ray.setLimit(reach);
        CollisionResults results = new CollisionResults();
        rootNode.collideWith(ray, results);
        if (results.size() > 0) {
            Spatial hit = results.getClosestCollision().getGeometry();
            GameObject obj = findRegistered(hit);
            // Polimorfismo: qualquer GameObject que implemente Interactable é ativado
            if (obj instanceof Interactable interactable) {
                InteractionResult result = interactable.interact(new InteractionContext(vw, null, player));
                if (result == InteractionResult.CONSUME) {
                    System.out.println("Interacted with interactable: " + obj.getName());
                    return; // Interação consumida pela entidade; não verifica blocos atrás
                }
            }
        }

        // MECÂNICA 3: DDA Traversal em Blocos Voxel
        // Se nenhuma entidade consumiu a interação, verifica o bloco voxel sólido sob a mira.
        if (vw != null) {
            var hitSolid = vw.pickFirstSolid(cam, reach);
            if (hitSolid.isPresent()) {
                var cell = hitSolid.get();
                VoxelBlockType blockType = vw.getBlockType(cell.x, cell.y, cell.z);
                // Se o bloco implementar Interactable (ex: BlocoInteragivel), invoca a sua interação
                if (blockType instanceof Interactable interactable) {
                    BlockPos pos = new BlockPos(cell.x, cell.y, cell.z);
                    InteractionResult result = interactable.interact(new InteractionContext(vw, pos, player));
                    if (result == InteractionResult.CONSUME) {
                        System.out.println("Interacted with voxel block at " + pos);
                        return; // Interação consumida pelo bloco; não abre o inventário pessoal
                    }
                }
            }
        }

        // MECÂNICA 4: Abertura do Inventário Pessoal (estilo Minecraft)
        // Se o jogador pressionou 'E' sem estar a mirar para um bloco ou entidade interagível,
        // abre o seu próprio inventário pessoal (27 slots).
        if (player != null) {
            player.openPersonalInventory();
            System.out.println("Opened player personal inventory");
        }
    }

    private GameObject findRegistered(Spatial s) {
        Spatial cur = s;
        while (cur != null) {
            GameObject obj = renderIndex.lookup(cur);
            if (obj != null) return obj;
            cur = cur.getParent();
        }
        return null;
    }

    @Override
    protected void cleanup(Application app) { }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
