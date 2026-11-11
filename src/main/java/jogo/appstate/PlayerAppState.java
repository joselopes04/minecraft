package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.light.PointLight;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import jogo.gameobject.character.Player;

public class PlayerAppState extends BaseAppState {

    private final Node rootNode;
    private final AssetManager assetManager;
    private final Camera cam;
    private final InputAppState input;
    private final PhysicsSpace physicsSpace;
    private final WorldAppState world;

    private Node playerNode;
    private BetterCharacterControl characterControl;
    private Player player;

    // view angles
    private float yaw = 0f;
    private float pitch = 0f;

    // tuning
    private float moveSpeed = 8.0f; // m/s
    private float sprintMultiplier = 1.7f;
    private float mouseSensitivity = 30f; // degrees per mouse analog unit
    private float eyeHeight = 1.7f;

    private Vector3f spawnPosition = new Vector3f(25.5f, 12f, 25.5f);
    private PointLight playerLight;

    public PlayerAppState(Node rootNode, AssetManager assetManager, Camera cam, InputAppState input, PhysicsSpace physicsSpace, WorldAppState world) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.cam = cam;
        this.input = input;
        this.physicsSpace = physicsSpace;
        this.world = world;
        world.registerPlayerAppState(this);
    }

    @Override
    protected void initialize(Application app) {
        // query world for recommended spawn now that it should be initialized
        if (world != null) {
            spawnPosition = world.getRecommendedSpawnPosition();
        }

        playerNode = new Node("Player");
        rootNode.attachChild(playerNode);

        // Engine-neutral player entity (no engine visuals here)
        player = new Player();

        // BetterCharacterControl(radius, height, mass)
        characterControl = new BetterCharacterControl(0.42f, 1.8f, 80f);
        characterControl.setJumpForce(new Vector3f(0, 400f, 0));
        playerNode.addControl(characterControl);
        physicsSpace.add(characterControl);
        characterControl.setGravity(new Vector3f(0, -24f, 0));

        // Local light source that follows the player's head
        playerLight = new PointLight();
        playerLight.setColor(new com.jme3.math.ColorRGBA(0.6f, 0.55f, 0.5f, 1f));
        playerLight.setRadius(12f);
        rootNode.addLight(playerLight);

        // Spawn at recommended location
        respawn();

        // initialize camera
        cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 500f);
        applyViewToCamera();
    }

    @Override
    public void update(float tpf) {
        // respawn on request
        if (input.consumeRespawnRequested()) {
            // refresh spawn from world in case terrain changed
            if (world != null) spawnPosition = world.getRecommendedSpawnPosition();
            respawn();
        }

        // pause controls if mouse not captured
        if (!input.isMouseCaptured()) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            // keep light with player even when paused
            if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));
            applyViewToCamera();
            return;
        }

        // handle mouse look
        Vector2f md = input.consumeMouseDelta();
        if (md.lengthSquared() != 0f) {
            float degX = md.x * mouseSensitivity;
            float degY = md.y * mouseSensitivity;
            yaw -= degX * FastMath.DEG_TO_RAD;
            pitch += degY * FastMath.DEG_TO_RAD;
            pitch = FastMath.clamp(pitch, -FastMath.HALF_PI * 0.99f, FastMath.HALF_PI * 0.99f);
        }

        // movement input in XZ plane based on camera yaw
        Vector3f wish = input.getMovementXZ();
        Vector3f dir = Vector3f.ZERO;
        if (wish.lengthSquared() > 0f) {
            dir = computeWorldMove(wish).normalizeLocal();
        }
        float speed = moveSpeed * (input.isSprinting() ? sprintMultiplier : 1f);
        characterControl.setWalkDirection(dir.mult(speed));

        // jump
        if (input.consumeJumpRequested() && characterControl.isOnGround()) {
            characterControl.jump();
        }

        // place camera at eye height above physics location
        applyViewToCamera();

        // update light to follow head
        if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));
    }

    private void respawn() {
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.warp(spawnPosition);
        // Look down at the single block directly beneath the spawn point.
        this.yaw = 0f;
        this.pitch = -1.4f;
        applyViewToCamera();
    }

    private Vector3f computeWorldMove(Vector3f inputXZ) {
        // Match movement to the horizontal camera axes; input -Z means forward.
        Vector3f forward = new Vector3f(-FastMath.sin(yaw), 0, -FastMath.cos(yaw));
        Vector3f right = new Vector3f(FastMath.cos(yaw), 0, -FastMath.sin(yaw));
        return right.mult(inputXZ.x).addLocal(forward.mult(-inputXZ.z));
    }

    private void applyViewToCamera() {
        // Character world location (spatial is synced by control)
        Vector3f loc = playerNode.getWorldTranslation().add(0, eyeHeight, 0);
        cam.setLocation(loc);
        float cosPitch = FastMath.cos(pitch);
        Vector3f direction = new Vector3f(
                -FastMath.sin(yaw) * cosPitch,
                FastMath.sin(pitch),
                -FastMath.cos(yaw) * cosPitch);
        cam.lookAtDirection(direction, Vector3f.UNIT_Y);
    }

    @Override
    protected void cleanup(Application app) {
        if (playerNode != null) {
            if (characterControl != null) {
                physicsSpace.remove(characterControl);
                playerNode.removeControl(characterControl);
                characterControl = null;
            }
            playerNode.removeFromParent();
            playerNode = null;
        }
        if (playerLight != null) {
            rootNode.removeLight(playerLight);
            playerLight = null;
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    public void refreshPhysics() {
        if (characterControl != null) {
            physicsSpace.remove(characterControl);
            physicsSpace.add(characterControl);
        }
    }
}
