package jogo.interaction;

/**
 * Resultado retornado após uma tentativa de interação com um bloco interagível.
 * - CONSUME: A interação foi processada com sucesso e o evento deve ser "consumido" (efetuado)
 *            (impede que a interação se propague para outros objetos ou blocos atrás).
 * - PASS: A interação foi ignorada pelo alvo, permitindo que o motor tente interagir
 *         com o próximo elemento no caminho do raycast.
 */
public enum InteractionResult {
    CONSUME,
    PASS
}
