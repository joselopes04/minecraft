package jogo.voxel.terrainGenerator;

import jogo.voxel.VoxelPalette;

public class TerrainGenerator {

    private final PerlinNoise perlin;

    // Parâmetros do terreno
    private static final int MEDIAN_HEIGHT = 62; // Altura média do terreno
    private static final int AMPLITUDE_RELIEF = 48; // Variação máxima(+-) da altura do terreno em relação à altura base
    private static final double SCALE = 0.008;// Controlando a frequência do relevo, menor valor = colinas mais largas e suaves

    // Parâmetros do Perlin Noise
    private static final int OCTAVES = 4; // Número de camadas de detalhe somadas
    private static final double PERSISTENCE = 0.48; // Peso de cada oitava seguinte na altura
    private static final double LACUNARITY = 2.0; // Frequência de cada oitava seguinte em relação à anterior

    // Parâmetros adicionais
    public static final int SEA_LEVEL = 58;
    private static final int PEAK_HEIGHT = 88; // Altura a partir da qual o terreno é desenhado por pedras
    private static final int DIRT_THICKNESS = 4; // Espessura da camada de terra acima da pedra


    // Construtor que recebe uma seed
    public TerrainGenerator(long seed) {
        this.perlin = new PerlinNoise(seed);
    }

    // Construtor alternativo que aceita uma string como semente, convertendo-a em um valor long.
    public TerrainGenerator(String textSeed) {
        this(textSeed != null ? textSeed.hashCode() : 0L);
    }

    //Devolve a altura do bloco sólido mais alto no ponto (x,z)
    public int getSurfaceHeight(int x, int z) {
        double noiseValue = perlin.fractalBrownianMotion(x * SCALE, z * SCALE, OCTAVES, PERSISTENCE, LACUNARITY); //Calcula o valor do ruído Perlin para a posição (x,z), o valor está entre -1 e 1
        int height = MEDIAN_HEIGHT + (int) Math.round(noiseValue * AMPLITUDE_RELIEF); //Converte o ruido em uma altura de bloco
        return Math.max(1, height); // Garante que a altura mínima seja 1
    }

    // Devolve a altura do bloco mais alto (terreno ou água) nesta coluna.
    public int getHighestBlockY(int surfaceHeight) {
        return Math.max(surfaceHeight, SEA_LEVEL); // se a superfície estiver abaixo do nível do mar, devolve o nível do mar
    }

    //Recebe o bloco que se encontra na posição (x,y,z) do mundo, devolvendo o ID do bloco
    public byte getBlockIdAt(int x, int y, int z) {
        return getBlockIdAt(y, getSurfaceHeight(x, z));
    }

    // Coloca o bloco correto na posição (x,y,z) do mundo, devolvendo o ID do bloco
    public byte getBlockIdAt(int y, int surfaceHeight) {
       
        // Acima do terreno: água até ao nível do mar, ar daí para cima
        if (y > surfaceHeight) {
            return y <= SEA_LEVEL ? VoxelPalette.WATER_ID : VoxelPalette.AIR_ID;
        }

        boolean peak = surfaceHeight >= PEAK_HEIGHT;  // Determina se o ponto é considerado um pico de montanha
        int depth = surfaceHeight - y; // Calcula a profundidade do bloco em relação à superfície

        if (depth == 0) {
            if (peak) return VoxelPalette.STONE_ID; // Confirma se é pico
            if (surfaceHeight <= SEA_LEVEL) return VoxelPalette.DIRT_ID; // Se estiver abaixo do nível do mar, coloca terra
            return VoxelPalette.Grass_ID; 
        }
        if (depth <= DIRT_THICKNESS && !peak) return VoxelPalette.DIRT_ID; //Coloca terra nos blocos logo abaixo da superfície, a menos que seja um pico

        return VoxelPalette.STONE_ID;
    }
}