package jogo.voxel.blocks;

import jogo.interaction.Interactable;
import jogo.voxel.VoxelBlockType;

/**
 * Classe base abstrata para blocos voxel que suportam interação.
 * Combina a definição de tipo de bloco do mundo (VoxelBlockType) com o contrato
 * polimórfico de interação (Interactable).
 * Subclasses como Baú ou Fornalha devem estender esta classe e implementar
 * o método interact(InteractionContext ctx).
 */
public abstract class BlocoInteragivel extends VoxelBlockType implements Interactable {

    /**
     * O parametro "name" corresponde ao Nome identificador do tipo de bloco (usado para localizar texturas, como "bau" ou "fornalha").
     */
    protected BlocoInteragivel(String name) {
        super(name);
    }
}
