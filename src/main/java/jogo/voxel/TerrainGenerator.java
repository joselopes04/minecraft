package jogo.voxel;

import java.util.Random;

public class TerrainGenerator {
    
    private final long seed;

    //Constantes para controlar o relevo
    private static final int AMPLITUDE_RELEVO = 8; // Quão altas sao as colinas e quão profundos são os vales
    private static final double FREQUENCIA = 0.12; // Como o relevo vai mudando quão maior mais acidentado fica.
    private static final int ALTURA_BASE = 16; // Nível médio do chão

    private final double offsetX;
    private final double offsetZ;

    //Gera um deslocamento aletório ao terreno com base na seed
    public TerrainGenerator(long seed) {
        Random seedRandom = new Random(seed);
        this.offsetX = seedRandom.nextDouble() * 10000.0;  //
        this.offsetZ = seedRandom.nextDouble() * 10000.0;
        this.seed = seed;
    }

    // Permite usar uma seed em formato de texto, convertendo-a para um long
    public TerrainGenerator(String textSeed) {
        this(textSeed != null ? textSeed.hashCode() : 0L); // Se a seed for nula, usa 0 como seed 
    }

    //Calcula a altura do terreno 
    public int getSurfaceHeight(int x, int z) {
        double ondaX = Math.sin((x + offsetX) * FREQUENCIA); //Gera uma curvatura ao terreno no eixo do x
        double ondaZ = Math.cos((z + offsetZ) * FREQUENCIA); // Gera uma curvatura ao terreno no eixo do z
        double ondaDiagonal = Math.sin(((x + z) * 0.5 + offsetX) * (FREQUENCIA * 0.5)); // Gera uma curvatura ao terreno na diagonal, para dar mais variedade ao relevo

        double ruidoCombinado = (ondaX + ondaZ + ondaDiagonal) / 2.0; // Combina as ondas para criar um relevo mais natural
        int altura = ALTURA_BASE + (int) Math.round(ruidoCombinado * AMPLITUDE_RELEVO); // Calcula a altura final do terreno com base na base e no relevo gerado
        return Math.max(1, altura); // Garante que a altura mínima seja 1, evitando buracos para o void
    }


   //Função que retorna o ID do bloco com base na altura e na posição (x, y, z)
   //Retorna um byte pois é mais 4*eficiente que usar um int(), prq 1 int = 4bytes
    public byte getBlockIdAt(int x, int y, int z) {
        int alturaSuperficie = getSurfaceHeight(x, z);

        if (y > alturaSuperficie) {
            return VoxelPalette.AIR_ID;
        }

        //Bedrock layer
        if (y == 0) {
            return 1; 
        }

       //Grass layer
        if (y == alturaSuperficie ) {
            return 1; 
        }

        // Terra Layer
        if (y >= alturaSuperficie - 2) {
            return 1;
        }

        return 1;
    }

    //Função que retorna a seed usada para gerar o terreno (Não está a ser usada para nada)
    public long getSeed() {
        return seed;
    }

}
