package plus.dragons.visuality.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import plus.dragons.visuality.client.VisualityParticles;

@SideOnly(Side.CLIENT)
public class ChargeParticle extends Particle {

    private final TextureAtlasSprite[] frames;

    protected ChargeParticle(World world, double x, double y, double z) {
        super(world, x, y, z, 0, 0, 0);
        this.frames = VisualityParticles.charge();
        this.particleMaxAge = 8 + this.rand.nextInt(4);
        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        this.multipleParticleScaleBy(1.25f); // mirrors 1.20 m_6569_(1.25)
        this.setParticleTexture(frames[0]);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        int idx = this.particleAge * this.frames.length / this.particleMaxAge;
        if (idx >= this.frames.length) {
            idx = this.frames.length - 1;
        }
        this.setParticleTexture(this.frames[idx]);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    // NOTE: modern ChargeParticle does NOT override the lightmap - it renders at real world light
    // (only SparkleParticle forces fullbright in modern). Leaving getBrightnessForRender at the
    // vanilla default matches 1.20.1. (The port previously forced 0xF000F0 fullbright, making
    // charged-creeper sparks glow in the dark unlike modern.)

    public static void spawn(World world, double x, double y, double z) {
        Minecraft.getMinecraft().effectRenderer.addEffect(new ChargeParticle(world, x, y, z));
    }
}
