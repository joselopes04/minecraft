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

    private static final float JUMP_COOLDOWN_TIME = 0.20f;//Evitar o double jump.
    private float jumpCooldownTimer = 0f; //Temporizador para o cooldown do salto
    private static final float MAX_VERTICAL_SPEED_TO_JUMP = 2.0f; //Velocidade vertical para o jogador poder saltar
    private final Vector3f velocidadeTmp = new Vector3f(); //Vetor para ler a velocidade do jogador

    private Vector3f spawnPosition = new Vector3f(25.5f, 12f, 25.5f);
    private PointLight playerLight;
    private boolean wasInWater = false;
    private float debugTimer = 0f;

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
        if (world != null) {
            spawnPosition = world.getRecommendedSpawnPosition();
        }

        playerNode = new Node("Player");
        rootNode.attachChild(playerNode);

        player = new Player();

        // BetterCharacterControl(radius, height, mass)
        characterControl = new BetterCharacterControl(0.42f, 1.8f, 80f);
        characterControl.setJumpForce(new Vector3f(0, 540f, 0));
        playerNode.addControl(characterControl);
        physicsSpace.add(characterControl);
        characterControl.setGravity(new Vector3f(0, -24f, 0));

        playerLight = new PointLight();
        playerLight.setColor(new com.jme3.math.ColorRGBA(0.6f, 0.55f, 0.5f, 1f));
        playerLight.setRadius(12f);
        //rootNode.addLight(playerLight);

        respawn();

        cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 140f);
        applyViewToCamera();
    }

    @Override
    public void update(float tpf) {
        if (input.consumeRespawnRequested()) {
            if (world != null) spawnPosition = world.getRecommendedSpawnPosition();
            respawn();
        }

        if (!input.isMouseCaptured()) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));
            applyViewToCamera();
            return;
        }

        Vector2f md = input.consumeMouseDelta();
        if (md.lengthSquared() != 0f) {
            float degX = md.x * mouseSensitivity;
            float degY = md.y * mouseSensitivity;
            yaw -= degX * FastMath.DEG_TO_RAD;
            pitch += degY * FastMath.DEG_TO_RAD;
            pitch = FastMath.clamp(pitch, -FastMath.HALF_PI * 0.99f, FastMath.HALF_PI * 0.99f);
        }

        // 1. Verifica se o jogador está dentro ou à superfície da água (apenas pés e corpo)
        Vector3f pos = playerNode.getWorldTranslation();
        int bx = (int) Math.floor(pos.x);
        int byFeet = (int) Math.floor(pos.y + 0.2f);
        int byBody = (int) Math.floor(pos.y + 1.0f);
        int bz = (int) Math.floor(pos.z);

        boolean inWater = false;
        byte blockFeet = 0, blockBody = 0;
        if (world != null) {
            blockFeet = world.getBlockAt(bx, byFeet, bz);
            blockBody = world.getBlockAt(bx, byBody, bz);
            inWater = (blockFeet == jogo.voxel.VoxelPalette.WATER_ID
                    || blockBody == jogo.voxel.VoxelPalette.WATER_ID);
        }

        // 2. Gravidade mais natural na água (-12f em vez de -4f, para afundar logo ao entrar)
        if (inWater != wasInWater) {
            if (inWater) {
                characterControl.setGravity(new Vector3f(0, -12f, 0));
            } else {
                characterControl.setGravity(new Vector3f(0, -24f, 0));
            }
            wasInWater = inWater;
        }

        // 3. Movimento horizontal
        Vector3f wish = input.getMovementXZ();
        Vector3f dir = Vector3f.ZERO;
        if (wish.lengthSquared() > 0f) {
            dir = computeWorldMove(wish).normalizeLocal();
        }
        float speed = inWater ? (moveSpeed * 0.65f) : (moveSpeed * (input.isSprinting() ? sprintMultiplier : 1f));
        characterControl.setWalkDirection(dir.mult(speed));
        //Jump
        updateJump(tpf, inWater);

        applyViewToCamera();

        if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));
    }

    private void updateJump(float tpf, boolean inWater) {
        jumpCooldownTimer = Math.max(0f, jumpCooldownTimer - tpf);

        if (inWater) {
            characterControl.getVelocity(velocidadeTmp);
            // Permite nadar para cima com impulsos controlados sem acumulação exponencial de velocidade
            if (jumpCooldownTimer <= 0f
                    && (input.consumeJumpRequested() || input.isJumpHeld())
                    && velocidadeTmp.y <= MAX_VERTICAL_SPEED_TO_JUMP) {
                characterControl.setJumpForce(new Vector3f(0, 500f, 0));
                characterControl.jump();
                jumpCooldownTimer = 0.25f; // Cooldown entre impulsos na água
            }
        } else {
            characterControl.getVelocity(velocidadeTmp);
            boolean canJump = jumpCooldownTimer <= 0f
                    && characterControl.isOnGround()
                    && velocidadeTmp.y <= MAX_VERTICAL_SPEED_TO_JUMP;

            if (canJump && (input.consumeJumpRequested() || input.isJumpHeld())) {
                characterControl.setJumpForce(new Vector3f(0, 540f, 0));
                characterControl.jump();
                jumpCooldownTimer = JUMP_COOLDOWN_TIME;
            }
        }
    }

    private void respawn() {
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.warp(spawnPosition);
        this.yaw = 0f;
        this.pitch = -1.4f;
        applyViewToCamera();
    }

    private Vector3f computeWorldMove(Vector3f inputXZ) {
        Vector3f forward = new Vector3f(-FastMath.sin(yaw), 0, -FastMath.cos(yaw));
        Vector3f right = new Vector3f(FastMath.cos(yaw), 0, -FastMath.sin(yaw));
        return right.mult(inputXZ.x).addLocal(forward.mult(-inputXZ.z));
    }

    private void applyViewToCamera() {
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