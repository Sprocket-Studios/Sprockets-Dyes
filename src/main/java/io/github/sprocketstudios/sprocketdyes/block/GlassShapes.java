package io.github.sprocketstudios.sprocketdyes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Slab, stairs and wall shapes of (stained) glass. Like full glass they let light through, don't darken
 * their own faces, and hide faces pressed flush against the same glass, so a glass floor of slabs and
 * blocks reads as one pane. Stained variants tint beacon beams like stained glass.
 */
public final class GlassShapes {
    private GlassShapes() {}

    /** Hide our face when the neighbor is the same glass and both touching faces fully cover each other. */
    static boolean hidesFace(BlockState self, BlockState adjacent, Direction dir, Block base) {
        Block other = adjacent.getBlock();
        if (other != self.getBlock() && other != base && !(other instanceof Glassy g && g.glassBase() == base)) {
            return false;
        }
        VoxelShape mine = self.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(dir);
        VoxelShape theirs = adjacent.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(dir.getOpposite());
        // Hidden only when nothing of our face sticks out past theirs.
        return !mine.isEmpty() && !Shapes.joinIsNotEmpty(mine, theirs, BooleanOp.ONLY_FIRST);
    }

    interface Glassy {
        Block glassBase();
    }

    public static class Slab extends SlabBlock implements Glassy, BeaconBeamBlock {
        private final Block base;
        private final DyeColor color;

        public Slab(Block base, DyeColor color, Properties properties) {
            super(properties);
            this.base = base;
            this.color = color;
        }

        @Override public Block glassBase() { return base; }
        @Override public DyeColor getColor() { return color == null ? DyeColor.WHITE : color; }

        @Override
        protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
            return hidesFace(state, adjacent, dir, base) || super.skipRendering(state, adjacent, dir);
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0F;
        }

        //? if >=1.21.2 {
        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }
        //?} else {
        /*@Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
        *///?}
    }

    public static class Stairs extends StairBlock implements Glassy, BeaconBeamBlock {
        private final Block base;
        private final DyeColor color;

        public Stairs(Block base, DyeColor color, Properties properties) {
            super(base.defaultBlockState(), properties);
            this.base = base;
            this.color = color;
        }

        @Override public Block glassBase() { return base; }
        @Override public DyeColor getColor() { return color == null ? DyeColor.WHITE : color; }

        @Override
        protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
            return hidesFace(state, adjacent, dir, base) || super.skipRendering(state, adjacent, dir);
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0F;
        }

        //? if >=1.21.2 {
        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }
        //?} else {
        /*@Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
        *///?}
    }

    public static class Wall extends WallBlock implements Glassy, BeaconBeamBlock {
        private final Block base;
        private final DyeColor color;

        public Wall(Block base, DyeColor color, Properties properties) {
            super(properties);
            this.base = base;
            this.color = color;
        }

        @Override public Block glassBase() { return base; }
        @Override public DyeColor getColor() { return color == null ? DyeColor.WHITE : color; }

        @Override
        protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
            return hidesFace(state, adjacent, dir, base) || super.skipRendering(state, adjacent, dir);
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0F;
        }
    }
}
