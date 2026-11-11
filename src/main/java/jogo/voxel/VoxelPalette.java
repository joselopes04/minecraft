package jogo.voxel;

import jogo.voxel.blocks.AirBlockType;
import jogo.voxel.blocks.StoneBlockType;

import java.util.ArrayList;
import java.util.List;

public class VoxelPalette {
    private final List<VoxelBlockType> types = new ArrayList<>();

    public byte register(VoxelBlockType type) {
        types.add(type);
        int id = types.size() - 1;
        if (id > 255) throw new IllegalStateException("Too many voxel block types (>255)");
        return (byte) id;
    }

    public VoxelBlockType get(byte id) {
        int idx = Byte.toUnsignedInt(id);
        if (idx < 0 || idx >= types.size()) return new AirBlockType();
        return types.get(idx);
    }

    public int size() { return types.size(); }

    public static VoxelPalette defaultPalette() {
        VoxelPalette p = new VoxelPalette();
        p.register(new AirBlockType());   // id 0
        p.register(new StoneBlockType()); // id 1
        return p;
    }

    public static final byte AIR_ID = 0;
    public static final byte STONE_ID = 1;
}
