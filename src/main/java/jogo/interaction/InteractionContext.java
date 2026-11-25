package jogo.interaction;

import jogo.gameobject.character.Player;
import jogo.voxel.VoxelWorld;

/**
 * Contexto imutável fornecido a um objeto interagível no momento da ação.
 * Agrega o mundo voxel (world), a posição do bloco (logo) após a interação (pos)
 * e o jogador que a efetuou (player).
 */
public record InteractionContext(VoxelWorld world, BlockPos pos, Player player) {}
