package jogo.voxel.terrainGenerator;

import jogo.voxel.VoxelPalette;

public class TerrainGenerator {

    private final long seed;
    private final PerlinNoise perlin;

    // ---- Parâmetros do relevo (documentar no relatório) ----
    private static final int    ALTURA_BASE      = 32;    // Nível médio do chão
    private static final int    AMPLITUDE_RELEVO = 95;    // Variação máxima (± blocos) em torno da base
    private static final double ESCALA           = 0.01;  // Menor = colinas mais largas e suaves
    private static final int    OITAVAS          = 4;     // Mais oitavas = mais detalhe
    private static final double PERSISTENCIA     = 0.5;   // Quanto cada oitava contribui face à anterior
    private static final double LACUNARIDADE     = 2.0;   // Aumento de frequência por oitava

    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.perlin = new PerlinNoise(seed);
    }

    // Permite usar uma seed em formato de texto, convertendo-a para um long
    public TerrainGenerator(String textSeed) {
        this(textSeed != null ? textSeed.hashCode() : 0L);
    }

    /** Altura da superfície numa coluna (x, z) usando Perlin noise fractal. */
    public int getSurfaceHeight(int x, int z) {
        double n = perlin.fbm(x * ESCALA, z * ESCALA, OITAVAS, PERSISTENCIA, LACUNARIDADE); // ~[-1, 1]
        int altura = ALTURA_BASE + (int) Math.round(n * AMPLITUDE_RELEVO);
        return Math.max(1, altura);
    }

    public byte getBlockIdAt(int x, int y, int z) {
        int alturaSuperficie = getSurfaceHeight(x, z);

        if (y > alturaSuperficie) return VoxelPalette.AIR_ID;

     if (y == alturaSuperficie) {
            if (y < 55) {
                return VoxelPalette.DIRT_ID;
            } else if (y < 60) {
                double probPedra = (y - 54) / 6.0;
                return getPseudoRandom(x, y, z) < probPedra ? VoxelPalette.STONE_ID : VoxelPalette.DIRT_ID;
            } else {
                return VoxelPalette.STONE_ID;
            }
        }
        return VoxelPalette.STONE_ID;
    }

    private double getPseudoRandom(int x, int y, int z) {
        long h = seed;
        h ^= x * 374761393L;
        h ^= y * 668265263L;
        h ^= z * 2147483647L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        
        // Converte os bits positivos para um double entre 0.0 e 1.0
        return (h & 0x7FFFFFFFL) / (double) Integer.MAX_VALUE;
    }

    public long getSeed() { return seed; }
}