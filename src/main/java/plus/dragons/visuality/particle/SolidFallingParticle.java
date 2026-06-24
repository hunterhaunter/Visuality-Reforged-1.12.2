package plus.dragons.visuality.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SolidFallingParticle extends Particle {

    private boolean stuckInGround = false;

    public SolidFallingParticle(World world, double x, double y, double z,
                                 double mx, double my, double mz,
                                 TextureAtlasSprite sprite) {
        super(world, x, y, z, mx, my, mz);
        // Modern SolidFallingParticle extends RisingParticle, whose ctor damps the base random
        // spawn velocity to 1% (xd = xd*0.01 + spawnVel) and adds a small ±0.05 spawn jitter.
        // 1.12.2's 7-arg Particle ctor injects that same random spherical velocity, so WITHOUT
        // this damp bones shot off in random directions instead of the tight modern up-and-out
        // ±0.2 burst. motionY is overwritten below, so only X/Z need damping.
        this.motionX *= 0.01D;
        this.motionZ *= 0.01D;
        this.setPosition(
                this.posX + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05D,
                this.posY + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05D,
                this.posZ + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05D);
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        // Modern m_6569_(1.1..1.6) MULTIPLIES the random base quadSize (0.1*(1.0..2.0)).
        // 1.12.2 renders half-size = 0.1F * particleScale on the identical random 1.0..2.0 base,
        // and multipleParticleScaleBy multiplies it (and the bbox) exactly like m_6569_ - so this
        // matches modern 1:1. (Overwriting with *2.0 dropped the small half of the range and made
        // every bone oversized.)
        this.multipleParticleScaleBy(1.1F + this.rand.nextInt(6) / 10.0F);
        this.particleAngle = this.rand.nextFloat() * (float)(Math.PI * 2.0);
        this.prevParticleAngle = this.particleAngle;
        this.motionY = -0.25D;
        this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D)) + 12;
        // NO continuous gravity: modern SolidFallingParticle extends RisingParticle,
        // whose tick() applies move + friction only (no 0.04*gravity term). Falling is
        // driven entirely by the age<=10 motionY logic below. Leaving particleGravity at
        // its 0 default keeps bone/wither_bone/emerald from plummeting (the "too quick" bug).
        this.setParticleTexture(sprite);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.stuckInGround) {
            // frozen on landing - only age out, no physics (kills the ground-hop)
            if (this.particleAge++ >= this.particleMaxAge) {
                this.setExpired();
            } else if (this.particleAge > this.particleMaxAge / 2) {
                this.particleAlpha = 1.0F
                        - (float) (this.particleAge - this.particleMaxAge / 2)
                        / (float) this.particleMaxAge;
            }
            return;
        }

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        // alpha fade in second half of life
        if (this.particleAge > this.particleMaxAge / 2) {
            this.particleAlpha = 1.0F
                    - (float)(this.particleAge - this.particleMaxAge / 2)
                    / (float)this.particleMaxAge;
        }

        // base physics: gravity + friction
        this.motionY -= 0.04D * (double)this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        // RisingParticle friction is 0.96 (not the 0.98 vanilla default) - applied to all axes.
        this.motionX *= 0.96D;
        this.motionY *= 0.96D;
        this.motionZ *= 0.96D;
        if (this.onGround) {
            this.motionX *= 0.7D;
            this.motionZ *= 0.7D;
        }

        // rising-then-falling motion: burst upward at age 1, gradual slowdown ages 2-10
        if (this.particleAge == 1) {
            this.motionX += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.2F;
            this.motionY = 0.3D + (double)this.rand.nextInt(11) / 100.0D;
            this.motionZ += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.2F;
        } else if (this.particleAge <= 10) {
            this.motionY -= 0.05D + (double)this.particleAge / 200.0D;
        }

        // latch on first ground contact: settle once, then freeze (no per-tick climb)
        if (this.onGround) {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            this.setPosition(this.prevPosX, this.prevPosY + 0.1D, this.prevPosZ);
            this.stuckInGround = true;
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    public static void spawn(World world, double x, double y, double z,
                              double mx, double my, double mz,
                              TextureAtlasSprite sprite) {
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new SolidFallingParticle(world, x, y, z, mx, my, mz, sprite));
    }
}
