package jogo.appstate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import jogo.gameobject.character.Player;
import jogo.gameobject.item.Item;
import jogo.inventory.Container;

/**
 * AppState da interface visual (HUD).
 * Observa de forma reativa o estado do Player:
 * - Renderiza a hotbar estilo Minecraft no fundo do ecrã com texturas e quantidades (stacks).
 * - Quando o jogador abre um contentor, renderiza a janela gráfica com título e slots,
 *   liberta o cursor do rato e permite pegar/mover/empilhar itens entre o inventário e a hotbar.
 */
public class HudAppState extends BaseAppState {

    /** Representa a área clicável de um slot na interface 2D. */
    private record SlotHitbox(Container container, int slotIndex, float x, float y, float size) {
        boolean contains(float px, float py) {
            return px >= x && px <= x + size && py >= y && py <= y + size;
        }
    }

    private final Node guiNode;
    private final AssetManager assetManager;
    private final InputAppState input;
    private final Player player;

    private BitmapFont font;
    private BitmapText crosshair;
    private Container lastRenderedContainer = null;
    private Node containerNode;

    // Estado reativo da Hotbar (estilo Minecraft)
    private Node hotbarNode;
    private int lastSelectedSlot = -1;
    private final Item[] lastHotbarItems = new Item[Player.HOTBAR_SIZE];
    private final int[] lastHotbarQuantities = new int[Player.HOTBAR_SIZE];
    private int lastScreenW = -1;
    private int lastScreenH = -1;

    // Hitboxes dos slots ativos (separados por contentor aberto e hotbar)
    private final List<SlotHitbox> containerHitboxes = new ArrayList<>();
    private final List<SlotHitbox> hotbarHitboxes = new ArrayList<>();

    // Item atualmente "agarrado" pelo cursor do rato no inventário
    private Item heldItem = null;
    private Node heldItemNode = null;

    // Cache de materiais de textura para ícones de itens
    private final Map<String, Material> itemIconMaterials = new HashMap<>();

    public HudAppState(Node guiNode, AssetManager assetManager, InputAppState input, Player player) {
        this.guiNode = guiNode;
        this.assetManager = assetManager;
        this.input = input;
        this.player = player;
    }

    public HudAppState(Node guiNode, AssetManager assetManager) {
        this(guiNode, assetManager, null, null);
    }

    @Override
    protected void initialize(Application app) {
        font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        crosshair = new BitmapText(font);
        crosshair.setText("+");
        crosshair.setSize(font.getCharSet().getRenderedSize() * 2f);
        guiNode.attachChild(crosshair);
        centerCrosshair();
        System.out.println("HudAppState initialized: crosshair attached");
    }

    private void centerCrosshair() {
        if (getApplication() == null) return;
        SimpleApplication sapp = (SimpleApplication) getApplication();
        int w = sapp.getCamera().getWidth();
        int h = sapp.getCamera().getHeight();
        float x = (w - crosshair.getLineWidth()) / 2f;
        float y = (h + crosshair.getLineHeight()) / 2f;
        crosshair.setLocalTranslation(x, y, 0);
    }

    @Override
    public void update(float tpf) {
        centerCrosshair();

        if (player != null) {
            Container current = player.getOpenContainer();
            if (current != lastRenderedContainer) {
                lastRenderedContainer = current;

                if (current != null) {
                    // O contentor acabou de ser aberto: desenha a janela e liberta o rato
                    showContainerUI(current);
                    if (input != null) {
                        input.setMouseCaptured(false);
                    }
                    if (getApplication() != null) {
                        getApplication().getInputManager().setCursorVisible(true);
                    }
                } else {
                    // Se o jogador fechar o inventário enquanto segura um item, devolve-o ao inventário
                    if (heldItem != null) {
                        player.collectItem(heldItem);
                        heldItem = null;
                        hideHeldItemUI();
                    }
                    hideContainerUI();
                    if (input != null) {
                        input.setMouseCaptured(true);
                    }
                    if (getApplication() != null) {
                        getApplication().getInputManager().setCursorVisible(false);
                    }
                }
            }

            // Processa cliques do rato quando um inventário/contentor está aberto
            if (current != null && input != null && !input.isMouseCaptured()) {
                boolean leftClick = input.consumeUiLeftClickRequested();
                boolean rightClick = input.consumeUiRightClickRequested();
                if (leftClick || rightClick) {
                    handleInventoryClick(current, leftClick);
                }
            }

            // Atualiza a posição do ícone do item agarrado pelo cursor
            updateHeldItemPosition();

            // Atualiza a hotbar se houver alterações nos itens, quantidades, slot ou resolução
            updateHotbarIfNeeded();
        }
    }

    /**
     * Trata cliques com o botão esquerdo/direito sobre os slots do contentor aberto ou da hotbar,
     * permitindo pegar, largar, dividir e empilhar itens ao estilo Minecraft.
     */
    private void handleInventoryClick(Container openContainer, boolean isLeftClick) {
        if (getApplication() == null) return;
        Vector2f cursor = getApplication().getInputManager().getCursorPosition();

        SlotHitbox hit = findHitboxAt(cursor.x, cursor.y);
        if (hit == null) return;

        Container targetContainer = hit.container();
        int slotIdx = hit.slotIndex();
        Item slotItem = targetContainer.getItem(slotIdx);

        if (isLeftClick) {
            if (heldItem == null) {
                // Pegar na pilha inteira do slot
                if (slotItem != null) {
                    heldItem = slotItem;
                    targetContainer.setItem(slotIdx, null);
                }
            } else {
                if (slotItem == null) {
                    // Colocar a pilha agarrada no slot vazio
                    targetContainer.setItem(slotIdx, heldItem);
                    heldItem = null;
                } else if (slotItem.canStackWith(heldItem) && slotItem.getQuantity() < slotItem.getMaxStackSize()) {
                    // Empilhar no slot existente até ao limite máximo
                    int space = slotItem.getMaxStackSize() - slotItem.getQuantity();
                    int toMove = Math.min(space, heldItem.getQuantity());
                    slotItem.addQuantity(toMove);
                    heldItem.addQuantity(-toMove);
                    if (heldItem.getQuantity() <= 0) {
                        heldItem = null;
                    }
                } else {
                    // Trocar o item agarrado com o item do slot
                    targetContainer.setItem(slotIdx, heldItem);
                    heldItem = slotItem;
                }
            }
        } else {
            // Botão direito: dividir metade (se mão vazia) ou colocar 1 unidade (se a segurar algo)
            if (heldItem == null) {
                if (slotItem != null) {
                    int total = slotItem.getQuantity();
                    int take = (total + 1) / 2;
                    int remain = total - take;
                    heldItem = slotItem.copyWithQuantity(take);
                    if (remain > 0) {
                        slotItem.setQuantity(remain);
                    } else {
                        targetContainer.setItem(slotIdx, null);
                    }
                }
            } else {
                if (slotItem == null) {
                    targetContainer.setItem(slotIdx, heldItem.copyWithQuantity(1));
                    heldItem.addQuantity(-1);
                    if (heldItem.getQuantity() <= 0) {
                        heldItem = null;
                    }
                } else if (slotItem.canStackWith(heldItem) && slotItem.getQuantity() < slotItem.getMaxStackSize()) {
                    slotItem.addQuantity(1);
                    heldItem.addQuantity(-1);
                    if (heldItem.getQuantity() <= 0) {
                        heldItem = null;
                    }
                }
            }
        }

        // Reconstrói as interfaces afetadas para refletir imediatamente a alteração
        showContainerUI(openContainer);
        forceRebuildHotbar();
        rebuildHeldItemUI();
    }

    private SlotHitbox findHitboxAt(float px, float py) {
        for (SlotHitbox box : containerHitboxes) {
            if (box.contains(px, py)) return box;
        }
        for (SlotHitbox box : hotbarHitboxes) {
            if (box.contains(px, py)) return box;
        }
        return null;
    }

    /**
     * Constrói dinamicamente a geometria 2D da interface gráfica do contentor.
     * Gera painel de fundo, título, grelha proporcional de slots e instruções de fecho.
     */
    private void showContainerUI(Container container) {
        hideContainerUI();
        if (getApplication() == null) return;

        containerNode = new Node("ContainerUI");
        containerHitboxes.clear();

        SimpleApplication sapp = (SimpleApplication) getApplication();
        float screenW = sapp.getCamera().getWidth();
        float screenH = sapp.getCamera().getHeight();

        int size = container.getSize();
        // Disposição em grelha: no máximo 9 colunas por linha
        int cols = size <= 9 ? size : 9;
        int rows = (int) Math.ceil((double) size / cols);

        float slotSize = 44f;
        float slotPadding = 8f;
        float panelPadding = 24f;

        float gridW = cols * slotSize + (cols - 1) * slotPadding;
        float gridH = rows * slotSize + (rows - 1) * slotPadding;

        float panelW = Math.max(gridW + panelPadding * 2, 320f);
        float panelH = gridH + panelPadding * 2 + 76f; // Espaço extra para cabeçalho e rodapé

        float panelX = (screenW - panelW) / 2f;
        float panelY = (screenH - panelH) / 2f + 24f; // Ligeiramente acima da hotbar

        // 1. Painel de Fundo do Contentor
        Quad bgQuad = new Quad(panelW, panelH);
        Geometry bgGeom = new Geometry("ContainerBG", bgQuad);
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.12f, 0.12f, 0.16f, 0.94f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bgGeom.setMaterial(bgMat);
        bgGeom.setLocalTranslation(panelX, panelY, 0);
        containerNode.attachChild(bgGeom);

        // 2. Título do Contentor
        BitmapText titleText = new BitmapText(font);
        titleText.setSize(font.getCharSet().getRenderedSize() * 1.25f);
        titleText.setColor(ColorRGBA.White);
        titleText.setText(container.getTitle() + " (" + size + " slots)");
        titleText.setLocalTranslation(panelX + panelPadding, panelY + panelH - 12f, 1);
        containerNode.attachChild(titleText);

        // 3. Grelha de Slots
        Material slotMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        slotMat.setColor("Color", new ColorRGBA(0.24f, 0.24f, 0.28f, 0.90f));
        slotMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        float startGridX = panelX + (panelW - gridW) / 2f;
        float startGridY = panelY + panelH - 46f - slotSize;

        for (int i = 0; i < size; i++) {
            int col = i % cols;
            int row = i / cols;
            float sx = startGridX + col * (slotSize + slotPadding);
            float sy = startGridY - row * (slotSize + slotPadding);

            Quad slotQuad = new Quad(slotSize, slotSize);
            Geometry slotGeom = new Geometry("Slot_" + i, slotQuad);
            slotGeom.setMaterial(slotMat);
            slotGeom.setLocalTranslation(sx, sy, 1);
            containerNode.attachChild(slotGeom);

            containerHitboxes.add(new SlotHitbox(container, i, sx, sy, slotSize));

            Item item = container.getItem(i);
            if (item != null) {
                attachItemVisual(containerNode, item, sx, sy, slotSize, 2, true);
            }
        }

        // 4. Rodapé com instruções
        BitmapText footerText = new BitmapText(font);
        footerText.setSize(font.getCharSet().getRenderedSize() * 0.85f);
        footerText.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        footerText.setText("Clique esq: mover/juntar | Clique dir: dividir/1 | [E] Fechar");
        float footX = panelX + (panelW - footerText.getLineWidth()) / 2f;
        footerText.setLocalTranslation(footX, panelY + 20f, 1);
        containerNode.attachChild(footerText);

        guiNode.attachChild(containerNode);
    }

    /**
     * Remove a interface gráfica do contentor da cena 2D (guiNode) quando é fechado.
     */
    private void hideContainerUI() {
        containerHitboxes.clear();
        if (containerNode != null) {
            containerNode.removeFromParent();
            containerNode = null;
        }
    }

    /**
     * Verifica se o estado da hotbar (slot selecionado, itens, quantidades ou resolução) mudou
     * e reconstrói a barra visual apenas quando necessário.
     */
    private void updateHotbarIfNeeded() {
        if (getApplication() == null) return;
        SimpleApplication sapp = (SimpleApplication) getApplication();
        int screenW = sapp.getCamera().getWidth();
        int screenH = sapp.getCamera().getHeight();
        int selected = player.getSelectedSlot();
        Container hotbar = player.getHotbar();

        boolean changed = (hotbarNode == null)
                || (screenW != lastScreenW)
                || (screenH != lastScreenH)
                || (selected != lastSelectedSlot);

        if (!changed) {
            for (int i = 0; i < Player.HOTBAR_SIZE; i++) {
                Item item = hotbar.getItem(i);
                int qty = item != null ? item.getQuantity() : 0;
                if (item != lastHotbarItems[i] || qty != lastHotbarQuantities[i]) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            lastScreenW = screenW;
            lastScreenH = screenH;
            lastSelectedSlot = selected;
            for (int i = 0; i < Player.HOTBAR_SIZE; i++) {
                Item item = hotbar.getItem(i);
                lastHotbarItems[i] = item;
                lastHotbarQuantities[i] = item != null ? item.getQuantity() : 0;
            }
            rebuildHotbarUI(screenW, screenH, hotbar, selected);
        }
    }

    private void forceRebuildHotbar() {
        if (getApplication() == null || player == null) return;
        SimpleApplication sapp = (SimpleApplication) getApplication();
        int screenW = sapp.getCamera().getWidth();
        int screenH = sapp.getCamera().getHeight();
        Container hotbar = player.getHotbar();
        int selected = player.getSelectedSlot();

        lastScreenW = screenW;
        lastScreenH = screenH;
        lastSelectedSlot = selected;
        for (int i = 0; i < Player.HOTBAR_SIZE; i++) {
            Item item = hotbar.getItem(i);
            lastHotbarItems[i] = item;
            lastHotbarQuantities[i] = item != null ? item.getQuantity() : 0;
        }
        rebuildHotbarUI(screenW, screenH, hotbar, selected);
    }

    /**
     * Constrói a barra de acesso rápido (Hotbar) estilo Minecraft no fundo central do ecrã.
     */
    private void rebuildHotbarUI(float screenW, float screenH, Container hotbar, int selectedSlot) {
        if (hotbarNode != null) {
            hotbarNode.removeFromParent();
            hotbarNode = null;
        }
        hotbarHitboxes.clear();

        hotbarNode = new Node("HotbarUI");
        int count = hotbar.getSize();
        float slotSize = 44f;
        float slotPadding = 6f;
        float panelPadding = 8f;

        float barW = count * slotSize + (count - 1) * slotPadding + panelPadding * 2;
        float barH = slotSize + panelPadding * 2;
        float barX = (screenW - barW) / 2f;
        float barY = 14f;

        // Fundo da hotbar
        Geometry bgGeom = new Geometry("HotbarBG", new Quad(barW, barH));
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.10f, 0.85f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bgGeom.setMaterial(bgMat);
        bgGeom.setLocalTranslation(barX, barY, 0);
        hotbarNode.attachChild(bgGeom);

        Material slotMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        slotMat.setColor("Color", new ColorRGBA(0.24f, 0.24f, 0.28f, 0.90f));
        slotMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Material selectedBorderMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        selectedBorderMat.setColor("Color", new ColorRGBA(0.95f, 0.85f, 0.25f, 0.95f));
        selectedBorderMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        float startX = barX + panelPadding;
        float startY = barY + panelPadding;

        for (int i = 0; i < count; i++) {
            float sx = startX + i * (slotSize + slotPadding);

            // Destaque visual do slot selecionado (estilo Minecraft)
            if (i == selectedSlot) {
                Geometry borderGeom = new Geometry("HotbarSelected_" + i, new Quad(slotSize + 6f, slotSize + 6f));
                borderGeom.setMaterial(selectedBorderMat);
                borderGeom.setLocalTranslation(sx - 3f, startY - 3f, 1);
                hotbarNode.attachChild(borderGeom);
            }

            Geometry slotGeom = new Geometry("HotbarSlot_" + i, new Quad(slotSize, slotSize));
            slotGeom.setMaterial(slotMat);
            slotGeom.setLocalTranslation(sx, startY, 2);
            hotbarNode.attachChild(slotGeom);

            hotbarHitboxes.add(new SlotHitbox(hotbar, i, sx, startY, slotSize));

            // Número do slot (1 a 9) no canto superior esquerdo
            BitmapText numText = new BitmapText(font);
            numText.setSize(font.getCharSet().getRenderedSize() * 0.62f);
            numText.setColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.9f));
            numText.setText(String.valueOf(i + 1));
            numText.setLocalTranslation(sx + 3f, startY + slotSize - 2f, 4);
            hotbarNode.attachChild(numText);

            // Textura e quantidade do item, se o slot estiver ocupado
            Item item = hotbar.getItem(i);
            if (item != null) {
                attachItemVisual(hotbarNode, item, sx, startY, slotSize, 3, false);
            }
        }

        guiNode.attachChild(hotbarNode);
    }

    /**
     * Desenha o ícone texturizado do item (ex: Textures/stone.png) e a respetiva quantidade
     * no canto inferior direito do slot.
     */
    private void attachItemVisual(Node parent, Item item, float sx, float sy, float slotSize, int zBase, boolean centerIcon) {
        Material iconMat = getItemIconMaterial(item.getTexturePath());
        float iconSize = slotSize - 12f;
        float ix = sx + (slotSize - iconSize) / 2f;
        float iy = sy + (slotSize - iconSize) / 2f;

        if (iconMat != null) {
            Geometry iconGeom = new Geometry("ItemIcon_" + item.getName(), new Quad(iconSize, iconSize));
            iconGeom.setMaterial(iconMat);
            iconGeom.setLocalTranslation(ix, iy, zBase);
            parent.attachChild(iconGeom);
        } else {
            BitmapText itemText = new BitmapText(font);
            itemText.setSize(font.getCharSet().getRenderedSize() * 0.72f);
            itemText.setColor(ColorRGBA.Yellow);
            String itemName = item.getName();
            if (itemName.length() > 5) itemName = itemName.substring(0, 4) + "..";
            itemText.setText(itemName);
            itemText.setLocalTranslation(sx + 4f, sy + slotSize - 8f, zBase);
            parent.attachChild(itemText);
        }

        // Mostra o contador da pilha (stack) quando a quantidade é superior a 1
        if (item.getQuantity() > 1) {
            BitmapText qtyText = new BitmapText(font);
            qtyText.setSize(font.getCharSet().getRenderedSize() * 0.78f);
            qtyText.setColor(ColorRGBA.White);
            String qtyStr = String.valueOf(item.getQuantity());
            qtyText.setText(qtyStr);
            float qx = sx + slotSize - qtyText.getLineWidth() - 3f;
            float qy = sy + 14f;
            qtyText.setLocalTranslation(qx, qy, zBase + 1);
            parent.attachChild(qtyText);
        }
    }

    /**
     * Carrega e guarda em cache o Material 2D para a textura do item com filtro pixel-art.
     */
    private Material getItemIconMaterial(String texturePath) {
        if (texturePath == null) return null;
        if (itemIconMaterials.containsKey(texturePath)) {
            return itemIconMaterials.get(texturePath);
        }
        try {
            Texture tex = assetManager.loadTexture(texturePath);
            tex.setMagFilter(Texture.MagFilter.Nearest);
            tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", tex);
            itemIconMaterials.put(texturePath, mat);
            return mat;
        } catch (AssetNotFoundException e) {
            itemIconMaterials.put(texturePath, null);
            return null;
        }
    }

    private void rebuildHeldItemUI() {
        hideHeldItemUI();
        if (heldItem == null) return;
        heldItemNode = new Node("HeldItemUI");
        attachItemVisual(heldItemNode, heldItem, -20f, -20f, 40f, 10, true);
        guiNode.attachChild(heldItemNode);
        updateHeldItemPosition();
    }

    private void updateHeldItemPosition() {
        if (heldItemNode == null || getApplication() == null) return;
        Vector2f cursor = getApplication().getInputManager().getCursorPosition();
        heldItemNode.setLocalTranslation(cursor.x, cursor.y, 0);
    }

    private void hideHeldItemUI() {
        if (heldItemNode != null) {
            heldItemNode.removeFromParent();
            heldItemNode = null;
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (crosshair != null) {
            crosshair.removeFromParent();
            crosshair = null;
        }
        hideContainerUI();
        hideHeldItemUI();
        if (hotbarNode != null) {
            hotbarNode.removeFromParent();
            hotbarNode = null;
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
