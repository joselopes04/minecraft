package jogo.voxel.blocks;

public class WaterBlockType extends LiquidBlockType {
    public WaterBlockType() {
        // Procura por "Textures/water.png" e aplica 75% de opacidade (semi-transparente)
        super("water", 0.75f);
    }
}