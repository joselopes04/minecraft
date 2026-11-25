package jogo.gameobject.item;

import jogo.voxel.VoxelPalette;

/**
 * Representa um bloco de pedra convertido em item ("stone") que pode ser
 * recolhido, empilhado (até 64), armazenado no inventário e colocado de volta no mundo.
 */
public class StoneItem extends Item {

    /**
     * Cria uma nova instância do item de pedra com quantidade 1 (relativa ao bloco a ser ocupado no espaço do inventário).
     */
    public StoneItem() {
        this(1);
    }

    /**
     * Cria uma pilha do item de pedra com a quantidade especificada(relativa à quantidade de blocos no espaço do inventário).
     */
    public StoneItem(int quantity) {
        super("stone", DEFAULT_MAX_STACK, quantity);
    }

    @Override
    public byte getBlockId() {
        return VoxelPalette.STONE_ID;
    }

    @Override
    public Item copyWithQuantity(int quantity) {
        return new StoneItem(quantity);
    }
}
