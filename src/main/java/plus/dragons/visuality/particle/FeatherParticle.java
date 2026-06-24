package plus.dragons.visuality.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import plus.dragons.visuality.client.VisualityParticles;

/**
 * Feather particle - rises, then falls. Same motion shape as
 * {@code SolidFallingParticle} but with a smaller scale ({@code 0.7f} base)
 * and a simpler deceleration curve (linear {@code -0.05} per tick for the
 * first 10 ticks, no acceleration term).
 */
@SideOnly(Side.CLIENT)
public class FeatherParticle extends Particle {

    private boolean stuckInGround = false;

    public FeatherParticle(World world, double x, double y, double z,
                           double mx, double my, double mz) {
        super(world, x, y, z, mx, my, mz);
        // Modern FeatherParticle extends RisingParticle: damp the 7-arg ctor's random spawn
        // velocity to 1% and add a ±0.05 spawn jitter, so feathers burst in the tight modern
        // pattern rather than flying off in the random directions the raw 1.12.2 ctor injects.
        // motionY is overwritten below, so only X/Z need damping.
        this.motionX *= 0.01;
        this.motionZ *= 0.01;
        this.setPosition(
                this.posX + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05,
                this.posY + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05,
                this.posZ + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05);
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY = -0.25;
        this.particleMaxAge = (int) (8.0 / (Math.random() * 0.8 + 0.2)) + 12;
        this.particleAngle = this.rand.nextFloat() * (float) Math.PI * 2.0F;
        this.prevParticleAngle = this.particleAngle;
        // Modern m_6569_(0.7..1.2) MULTIPLIES the random base quadSize; multipleParticleScaleBy
        // does the same on 1.12.2's random 1.0..2.0 base so render half-size matches 1:1.
        // (Overwriting with *2.0 made feathers oversized, see SolidFallingParticle.)
        this.multipleParticleScaleBy(0.7F + this.rand.nextInt(6) / 10.0F);
        this.setParticleTexture(VisualityParticles.feather());
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.stuckInGround) {
            if (this.particleAge++ >= this.particleMaxAge) {
                this.setExpired();
            } else if (this.particleAge > this.particleMaxAge / 2) {
                float halfLife = this.particleMaxAge / 2.0F;
                this.particleAlpha = 1.0F - (this.particleAge - halfLife) / halfLife;
            }
            return;
        }

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        // alpha fade in second half of lifetime
        if (this.particleAge > this.particleMaxAge / 2) {
            float halfLife = this.particleMaxAge / 2.0F;
            this.particleAlpha = 1.0F - (this.particleAge - halfLife) / halfLife;
        }

        // base physics: gravity (none by default), 0.98 air friction
        this.motionY -= 0.04 * (double) this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        // RisingParticle friction is 0.96 (not the 0.98 vanilla default) - applied to all axes.
        this.motionX *= 0.96;
        this.motionY *= 0.96;
        this.motionZ *= 0.96;
        if (this.onGround) {
            this.motionX *= 0.7;
            this.motionZ *= 0.7;
        }

        // age-based vertical motion
        if (this.particleAge == 1) {
            this.motionX += (this.rand.nextFloat() * 2.0 - 1.0) * 0.2;
            this.motionY = 0.3 + this.rand.nextInt(11) / 100.0;
            this.motionZ += (this.rand.nextFloat() * 2.0 - 1.0) * 0.2;
        } else if (this.particleAge <= 10) {
            this.motionY -= 0.05;
        }

        // latch on first ground contact: settle once, then freeze
        if (this.onGround) {
            this.motionX = 0.0;
            this.motionY = 0.0;
            this.motionZ = 0.0;
            this.setPosition(this.prevPosX, this.prevPosY + 0.1, this.prevPosZ);
            this.stuckInGround = true;
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    public static void spawn(World world, double x, double y, double z,
                             double mx, double my, double mz) {
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new FeatherParticle(world, x, y, z, mx, my, mz));
    }
}
