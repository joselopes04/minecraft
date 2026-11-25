package jogo.interaction;

/**s
 * Qualquer entidade do mundo (ex: Item caído, NPC) ou bloco voxel (ex: BlocoInteragivel)
 * que implemente esta interface pode ser ativado quando o jogador olha para esse bloco e
 * pressiona a tecla de interação ('E').
 */
public interface Interactable {

    /**
     * Executa a lógica de interação com base no contexto fornecido.
     * 
     * ctx corresponde à posição do bloco (se houver) e o jogador (contexto que inclui o mundo, inventário, etc.).
     * Depois de processar a interação, retorna CONSUME se a mesma foi tratada com sucesso; PASS se deve permitir outras interações.
     */
    InteractionResult interact(InteractionContext ctx);
}
