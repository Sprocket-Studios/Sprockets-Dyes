package io.github.sprocketstudios.sprocketdyes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Slab and wall shapes of glazed terracotta. Like the full block they face back toward whoever placed them,
 * so their pattern lines up with the glazed terracotta next to them (stairs need no extra property: their
 * pattern follows their own {@code facing}).
 */
public final class GlazedShapes {
    private GlazedShapes() {}

    /** The pattern's facing for a new placement: the way the full block would face. */
    static Direction placedFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    static Direction rotated(Direction facing, Rotation rotation) {
        return rotation.rotate(facing);
    }

    /** Glazed patterns can't be mirrored, so vanilla turns the block instead; so do we. */
    static Direction mirrored(Direction facing, Mirror mirror) {
        return mirror.getRotation(facing).rotate(facing);
    }

    public static class Slab extends SlabBlock {
        public Slab(Properties properties) {
            super(properties);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            // Completing a double slab keeps the half that's already there.
            if (state == null || context.getLevel().getBlockState(context.getClickedPos()).is(this)) return state;
            return state.setValue(HorizontalDirectionalBlock.FACING, placedFacing(context));
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(HorizontalDirectionalBlock.FACING, rotated(state.getValue(HorizontalDirectionalBlock.FACING), rotation));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.setValue(HorizontalDirectionalBlock.FACING, mirrored(state.getValue(HorizontalDirectionalBlock.FACING), mirror));
        }
    }

    public static class Wall extends WallBlock {
        /** Before 1.21.5 WallBlock caches its shapes by state, built from the default state's facing only. */
        private final Direction shapeFacing;

        public Wall(Properties properties) {
            super(properties);
            this.shapeFacing = this.defaultBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        private BlockState shapeState(BlockState state) {
            return state.setValue(HorizontalDirectionalBlock.FACING, shapeFacing);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return super.getShape(shapeState(state), level, pos, context);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return super.getCollisionShape(shapeState(state), level, pos, context);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : state.setValue(HorizontalDirectionalBlock.FACING, placedFacing(context));
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            return super.rotate(state, rotation).setValue(HorizontalDirectionalBlock.FACING, rotated(facing, rotation));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            return super.mirror(state, mirror).setValue(HorizontalDirectionalBlock.FACING, mirrored(facing, mirror));
        }
    }
}
