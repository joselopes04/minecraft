package jogo.voxel.terrainGenerator;

import java.util.Random;

public class PerlinNoise {

    /** Tabela de permutação: baralhada primeiro e duplicada depois (evita overflow em X+1). */
    private final int[] permutation = new int[512];

    public PerlinNoise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        // Fisher-Yates com Random(seed) em vez de Math.random()
        Random rnd = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        // Baralhar e só depois duplicar
        for (int i = 0; i < 512; i++) permutation[i] = p[i & 255];
    }

    //Curva de suavização para interpolação
    private static double fade(double t) {
        return ((6 * t - 15) * t + 10) * t * t * t;
    }

    // Interpolação linear entre a1 e a2, com t em [0,1]
    private static double linearInterpolation (double t, double a1, double a2) {
        return a1 + t * (a2 - a1);
    }

    // Calcula o produto escalar entre o vetor de gradiente e o vetor de deslocamento (dx, dy)
    private static double dotConstantVector(int value, double dx, double dy) {
        switch (value & 3) {
            case 0:  return  dx + dy;
            case 1:  return -dx + dy;
            case 2:  return -dx - dy;
            default: return  dx - dy;
        }
    }

    // Calcula o valor do ruído Perlin para a posição (x,y), o valor está entre -1 e 1
    public double noise(double x, double y) {
        double floorX = Math.floor(x);
        double floorY = Math.floor(y);

        int X = (int) floorX & 255;   // célula da grelha (canto inferior esquerdo)
        int Y = (int) floorY & 255;
        double xf = x - floorX;       // posição dentro da célula [0, 1)
        double yf = y - floorY;

        // Valor da tabela para cada um dos 4 cantos (mesmo canto => mesmo valor)
        int valueTopRight = permutation[permutation[X + 1] + Y + 1];
        int valueTopLeft = permutation[permutation[X] + Y + 1];
        int valueBottomRight = permutation[permutation[X + 1] + Y];
        int valueBottomLeft = permutation[permutation[X] + Y];

        double dotTopRight = dotConstantVector(valueTopRight,xf - 1.0, yf - 1.0);
        double dotTopLeft = dotConstantVector(valueTopLeft,xf, yf - 1.0);
        double dotBottomRight = dotConstantVector(valueBottomRight, xf - 1.0, yf);
        double dotBottomLeft = dotConstantVector(valueBottomLeft, xf, yf);

        double u = fade(xf);
        double v = fade(yf);

        return linearInterpolation (u,
                linearInterpolation (v, dotBottomLeft,  dotTopLeft),
                linearInterpolation (v, dotBottomRight, dotTopRight));
    }

   // Combina as várias oitavas de ruído Perlin para criar um padrão mais complexo e natural. O resultado está em [-1, 1].
    public double fractalBrownianMotion(double x, double y, int octaves, double persistence, double lacunarity) {
        double result = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxAmplitude = 0.0;

        for (int i = 0; i < octaves; i++) {
            result += amplitude * noise(x * frequency, y * frequency);
            maxAmplitude += amplitude;
            amplitude *= persistence;   // artigo: 0.5
            frequency *= lacunarity;    // artigo: 2.0
        }
        return result / maxAmplitude;
    }
}