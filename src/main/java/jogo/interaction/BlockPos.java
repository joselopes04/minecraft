package jogo.interaction;

/**
 * Representa as coordenadas discretas inteiras (x, y, z) de um bloco no mundo voxel.
 * Como é um record, é imutável e gera automaticamente implementações seguras
 * de equals() e hashCode(), sendo ideal como chave para mapas de dados (ex: inventários).
 */
public record BlockPos(int x, int y, int z) {}
