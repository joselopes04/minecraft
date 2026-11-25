package jogo.gameobject.item;

import java.util.Objects;

import jogo.gameobject.GameObject;
import jogo.interaction.Interactable;
import jogo.interaction.InteractionContext;
import jogo.interaction.InteractionResult;

/**
 * Classe base para todos os itens representáveis no jogo.
 * Estende GameObject e implementa Interactable para permitir que qualquer item
 * largado ou posicionado no cenário 3D responda a interações quando mirado pelo jogador.
 * Suporta empilhamento (stacking estilo Minecraft), textura de inventário e associação opcional a um bloco.
 */
public abstract class Item extends GameObject implements Interactable {

    /** Quantidade máxima padrão de itens numa mesma pilha (stack), estilo Minecraft. */
    public static final int DEFAULT_MAX_STACK = 64;

    private int quantity;
    private final int maxStackSize;

    protected Item(String name) {
        this(name, DEFAULT_MAX_STACK, 1);
    }

    protected Item(String name, int maxStackSize, int quantity) {
        super(name);
        this.maxStackSize = Math.max(1, maxStackSize);
        this.quantity = Math.max(0, Math.min(quantity, this.maxStackSize));
    }

    /** Retorna a quantidade atual nesta pilha de itens. */
    public int getQuantity() {
        return quantity;
    }

    /** Define a quantidade nesta pilha, limitada entre 0 e o máximo permitido. */
    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, Math.min(quantity, maxStackSize));
    }

    /** Adiciona (ou subtrai, se negativo) unidades à pilha atual. */
    public void addQuantity(int delta) {
        setQuantity(this.quantity + delta);
    }

    /** Retorna o limite máximo de empilhamento deste tipo de item. */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * Verifica se este item pode ser empilhado (stacked) com outro item.
     * Requer que sejam da mesma classe e tenham o mesmo nome identificador.
     */
    public boolean canStackWith(Item other) {
        if (other == null || this.maxStackSize <= 1) return false;
        return this.getClass().equals(other.getClass())
                && Objects.equals(this.getName(), other.getName());
    }

    /**
     * Caminho da textura 2D usada para representar o item no inventário e na hotbar.
     */
    public String getTexturePath() {
        return "Textures/" + getName().toLowerCase() + ".png";
    }

    /**
     * Retorna o ID do bloco correspondente na VoxelPalette caso este item possa ser colocado no mundo,
     * ou -1 se não for um item colocável.
     */
    public byte getBlockId() {
        return -1;
    }

    /**
     * Cria uma nova instância do mesmo tipo de item com a quantidade especificada
     * (útil para dividir pilhas no inventário).
     */
    public abstract Item copyWithQuantity(int quantity);

    /**
     * Ponto de extensão para subclasses de Item definirem comportamentos customizados
     * ao serem ativados (ex: apanhar o item, equipar ferramenta, etc.).
     */
    public void onInteract() {
        // Ponto de extensão para lógica de interação de itens específicos
    }

    /**
     * Implementação do contrato universal de interação.
     * Delega no método onInteract() e consome o evento.
     */
    @Override
    public InteractionResult interact(InteractionContext ctx) {
        onInteract();
        return InteractionResult.CONSUME;
    }
}
