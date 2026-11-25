package jogo;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.system.AppSettings;

import jogo.appstate.HudAppState;
import jogo.appstate.InputAppState;
import jogo.appstate.InteractionAppState;
import jogo.appstate.PlayerAppState;
import jogo.appstate.RenderAppState;
import jogo.appstate.WorldAppState;
import jogo.engine.GameRegistry;
import jogo.engine.RenderIndex;
import jogo.gameobject.character.Player;

/**
 * Main application entry.
 */
public class Jogo extends SimpleApplication {

    public static void main(String[] args) {
        // LWJGL's legacy memory backend uses deprecated sun.misc.Unsafe APIs.
        // FFM is available on Java 25+; preserve explicit user backend choices.
        if (Runtime.version().feature() >= 25
                && System.getProperty("org.lwjgl.system.memoryBackend") == null) {
            System.setProperty("org.lwjgl.system.memoryBackend", "ffm");
        }
        Jogo app = new Jogo();
        // AWT dialogs conflict with LWJGL first-thread startup on macOS.
        boolean isMac = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("mac");
        app.setShowSettings(!isMac);
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Test");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setGammaCorrection(true); // enable sRGB gamma-correct rendering
        app.setSettings(settings);
        app.start();
    }

    private BulletAppState bulletAppState;

    @Override
    public void simpleInitApp() {
        // disable flyCam, we manage camera ourselves
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(false);
        viewPort.setBackgroundColor(new ColorRGBA(0.6f, 0.75f, 1f, 1f)); // sky-like

        // Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(false); // toggle off later
        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();

        // AppStates (order matters a bit: input -> world -> render -> interaction -> player)
        InputAppState input = new InputAppState();
        stateManager.attach(input);

        WorldAppState world = new WorldAppState(rootNode, assetManager, physicsSpace, cam, input);
        stateManager.attach(world);

        // Engine registry and render layers
        GameRegistry registry = new GameRegistry();
        RenderIndex renderIndex = new RenderIndex();
        stateManager.attach(new RenderAppState(rootNode, assetManager, registry, renderIndex));

        // Shared Player instance for decoupling and consistency across systems
        // A mesma instância da entidade lógica Player é partilhada entre os AppStates:
        // - PlayerAppState: atualiza posição, câmara e física do jogador
        // - InteractionAppState: acede e altera o estado do contentor aberto do jogador
        // - HudAppState: observa o contentor aberto para desenhar a interface e libertar o rato, de modo interagir com os itens lá presentes
        Player player = new Player();

        PlayerAppState playerAppState = new PlayerAppState(rootNode, assetManager, cam, input, physicsSpace, world, player);
        stateManager.attach(playerAppState);

        InteractionAppState interactionAppState = new InteractionAppState(rootNode, cam, input, renderIndex, world, player);
        stateManager.attach(interactionAppState);

        // Post-processing: SSAO for subtle contact shadows
      /*  try {
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            Class<?> ssaoCls = Class.forName("com.jme3.post.ssao.SSAOFilter");
            Object ssao = ssaoCls.getConstructor(float.class, float.class, float.class, float.class)
                    .newInstance(2.1f, 0.6f, 0.5f, 0.02f); // radius, intensity, scale, bias
            // Add filter via reflection to avoid compile-time dependency
            java.lang.reflect.Method addFilter = FilterPostProcessor.class.getMethod("addFilter", Class.forName("com.jme3.post.Filter"));
            addFilter.invoke(fpp, ssao);
            viewPort.addProcessor(fpp);
        } catch (Exception e) {
            System.out.println("SSAO not available (effects module missing?): " + e.getMessage());
        }*/

        // HUD (crosshair and reactive container UI)
        stateManager.attach(new HudAppState(guiNode, assetManager, input, player));
    }
}
