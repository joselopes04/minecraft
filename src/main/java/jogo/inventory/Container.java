package jogo.inventory;

import jogo.gameobject.item.Item;
import java.util.Arrays;

/**
 * Modelo de dados genérico para inventários no jogo.
 * Encapsula o título do contentor e um conjunto fixo de slots que armazenam instâncias de Item.
 * Segue o Princípio da Responsabilidade Única (SRP): apenas armazena e manipula itens,
 * sem qualquer dependência ou lógica gráfica da interface.
 */
public class Container {
    private final String title;
    private final Item[] slots;

    /**
     * Cria um novo contentor com um título e número de slots definido.
     * 
     * @param title Nome visível do contentor (ex: "Baú", "Fornalha").
     * @param size  Quantidade total de compartimentos (mínimo 1).
     */
    public Container(String title, int size) {
        this.title = title != null ? title : "Inventário";
        this.slots = new Item[Math.max(1, size)];
    }

    /** Retorna o título do contentor. */
    public String getTitle() {
        return title;
    }

    /** Retorna o número total de slots disponíveis. */
    public int getSize() {
        return slots.length;
    }

    /**
     * Obtém o item armazenado no slot indicado.
     * 
     * @param slot Índice do compartimento (0 a size - 1).
     * @return O Item no slot ou null se estiver vazio ou fora dos limites.
     */
    public Item getItem(int slot) {
        if (slot < 0 || slot >= slots.length) return null;
        return slots[slot];
    }

    /**
     * Define ou substitui o item no slot especificado.
     * 
     * @param slot Índice do compartimento.
     * @param item Item a guardar (ou null para esvaziar).
     */
    public void setItem(int slot, Item item) {
        if (slot >= 0 && slot < slots.length) {
            slots[slot] = item;
        }
    }

    /**
     * Insere o item fornecido no contentor com suporte a empilhamento (stacking):
     * 1. Tenta primeiro juntar a quantidade a pilhas existentes do mesmo tipo que ainda tenham espaço.
     * 2. Caso sobre quantidade, procura o primeiro slot vazio.
     *
     * @param item Item (ou pilha) a adicionar.
     * @return true se toda a quantidade foi guardada; false se não coube tudo (a quantidade restante fica em {@code item}).
     */
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) return false;

        // 1ª passagem: empilhar em slots existentes do mesmo tipo
        for (int i = 0; i < slots.length; i++) {
            Item existing = slots[i];
            if (existing != null && existing.canStackWith(item)) {
                int space = existing.getMaxStackSize() - existing.getQuantity();
                if (space > 0) {
                    int toMove = Math.min(space, item.getQuantity());
                    existing.addQuantity(toMove);
                    item.addQuantity(-toMove);
                    if (item.getQuantity() <= 0) {
                        return true;
                    }
                }
            }
        }

        // 2ª passagem: colocar o restante em slots vazios
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                if (item.getQuantity() <= item.getMaxStackSize()) {
                    slots[i] = item;
                    return true;
                } else {
                    Item split = item.copyWithQuantity(item.getMaxStackSize());
                    slots[i] = split;
                    item.addQuantity(-item.getMaxStackSize());
                }
            }
        }

        return item.getQuantity() <= 0;
    }

    /**
     * Esvazia todos os slots do contentor.
     */
    public void clear() {
        Arrays.fill(slots, null);
    }
}
