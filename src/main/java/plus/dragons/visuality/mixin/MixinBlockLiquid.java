package plus.dragons.visuality.mixin;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import plus.dragons.visuality.config.VisualityConfig;
import plus.dragons.visuality.particle.WaterCircleParticle;

import java.util.Random;

/**
 * Water circle particles on rain. Ports the modern WaterFluidMixin (which hooked
 * WaterFluid.animateTick - no such class in 1.12.2) onto BlockLiquid.randomDisplayTick.
 * Only fires for water source blocks under open rain. Client-side only (randomDisplayTick
 * is a client-only callback).
 */
@Mixin(BlockLiquid.class)
public class MixinBlockLiquid {

    @Inject(method = "randomDisplayTick", at = @At("HEAD"))
    private void visuality$waterCircles(IBlockState state, World world, BlockPos pos, Random rand, CallbackInfo ci) {
        if (!VisualityConfig.waterCircleEnabled) {
            return;
        }
        if (state.getMaterial() != Material.WATER) {
            return;
        }
        // source block only (fluid level 0)
        if (state.getValue(BlockLiquid.LEVEL) != 0) {
            return;
        }
        int density = VisualityConfig.waterCircleDensity;
        if (rand.nextInt(256) >= density) {
            return;
        }
        BlockPos above = pos.up();
        if (!world.isRainingAt(above)) {
            return;
        }
        int color = -1;
        if (VisualityConfig.waterCircleColored) {
            // Modern reads biome.getWaterColor() (a rich blue-ish per-biome colour).
            // 1.12.2 has no such field - biome.getWaterColorMultiplier() is ~0xFFFFFF
            // (white) for almost every biome, which is why circles rendered white. Instead
            // tint a base water blue by the biome-BLENDED water multiplier (BiomeColorHelper
            // does the neighbour blend the user remembers), giving blue circles normally and
            // a green shift in swamps - matching the 1.20 biome-dependent look.
            int multiplier = BiomeColorHelper.getWaterColorAtPos(world, pos);
            final int base = 0x3F76E4; // modern default water blue
            int r = ((base >> 16) & 0xFF) * ((multiplier >> 16) & 0xFF) / 255;
            int g = ((base >> 8) & 0xFF) * ((multiplier >> 8) & 0xFF) / 255;
            int b = (base & 0xFF) * (multiplier & 0xFF) / 255;
            color = (r << 16) | (g << 8) | b;
        }
        WaterCircleParticle.spawn(world,
                above.getX() + rand.nextDouble(),
                above.getY() - 0.1,
                above.getZ() + rand.nextDouble(),
                color);
    }
}
