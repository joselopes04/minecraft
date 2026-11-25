package jogo.gameobject.character;

import jogo.gameobject.item.Item;
import jogo.inventory.Container;

/**
 * Entidade lógica do jogador.
 * Mantém o estado de jogabilidade, incluindo a hotbar, o inventário pessoal
 * e o contentor atualmente aberto.
 */
public class Player extends Character {

    /** Número de compartimentos da barra de acesso rápido (hotbar), estilo Minecraft. */
    public static final int HOTBAR_SIZE = 9;

    /**
     * Referência para o contentor que o jogador está atualmente a aceder (ex: baú ou inventário pessoal).
     * Se null, o jogador não tem qualquer inventário aberto.
     */
    private Container openContainer;

    /**
     * Barra de acesso rápido (hotbar) do jogador com 9 slots.
     */
    private final Container hotbar = new Container("Hotbar", HOTBAR_SIZE);

    /**
     * Índice do slot atualmente selecionado na hotbar (0 a HOTBAR_SIZE - 1).
     */
    private int selectedSlot = 0;

    /**
     * Inventário pessoal do jogador (estilo Minecraft: 27 slots principais além da hotbar).
     */
    private final Container personalInventory = new Container("Inventário", 27);

    public Player() {
        super("Player");
    }

    /**
     * Retorna a hotbar (barra de acesso rápido de 9 slots) do jogador.
     */
    public Container getHotbar() {
        return hotbar;
    }

    /**
     * Retorna o índice do slot atualmente selecionado na hotbar (0 a 8).
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Define o slot selecionado na hotbar, garantindo que o índice permanece dentro dos limites válidos (0 a 8).
     *
     * @param slot Novo índice do slot.
     */
    public void setSelectedSlot(int slot) {
        int size = hotbar.getSize();
        if (size <= 0) return;
        this.selectedSlot = ((slot % size) + size) % size;
    }

    /**
     * Retorna o item atualmente selecionado na hotbar, ou null se o slot estiver vazio.
     */
    public Item getSelectedItem() {
        return hotbar.getItem(selectedSlot);
    }

    /**
     * Recolhe um item e guarda-o no inventário do jogador.
     * Tal como no Minecraft, tenta inserir primeiro na hotbar; se esta estiver cheia,
     * tenta armazenar no inventário pessoal.
     *
     * @param item Item a recolher.
     * @return true se o item foi guardado com sucesso; false se ambos os inventários estiverem cheios.
     */
    public boolean collectItem(Item item) {
        if (item == null) return false;
        if (hotbar.addItem(item)) {
            return true;
        }
        return personalInventory.addItem(item);
    }

    /**
     * Retorna o inventário pessoal (próprio) do jogador.
     */
    public Container getPersonalInventory() {
        return personalInventory;
    }

    /**
     * Abre diretamente o inventário do jogador.
     */
    public void openPersonalInventory() {
        openContainer(personalInventory);
    }

    /**
     * Define o contentor ativo para o jogador, sinalizando à interface gráfica para abrir a janela.
     * 
     * @param container Contentor aberto.
     */
    public void openContainer(Container container) {
        this.openContainer = container;
    }

    /**
     * Fecha o contentor atualmente aberto pelo jogador.
     */
    public void closeContainer() {
        this.openContainer = null;
    }

    /**
     * Retorna o contentor que o jogador tem aberto de momento.
     * 
     * @return O Container atual ou null se nenhum estiver aberto.
     */
    public Container getOpenContainer() {
        return openContainer;
    }

    /**
     * Verifica se o jogador tem algum contentor aberto.
     * 
     * @return true se tiver um contentor aberto, false em caso contrário.
     */
    public boolean hasOpenContainer() {
        return openContainer != null;
    }
}
