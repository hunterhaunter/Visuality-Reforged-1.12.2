package plus.dragons.visuality.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SlimeParticle extends Particle {

    protected SlimeParticle(World world, double x, double y, double z,
                            float r, float g, float b, float scale,
                            TextureAtlasSprite sprite) {
        super(world, x, y, z, 0, 0, 0);
        this.particleRed = r;
        this.particleGreen = g;
        this.particleBlue = b;
        this.particleAlpha = 0.8f;
        this.motionX *= 0.1;
        this.motionY *= 0.1;
        this.motionZ *= 0.1;
        this.particleGravity = 1.0f;
        this.multipleParticleScaleBy(scale + this.rand.nextInt(6) / 10.0f); // 1.20 m_6569_(scale + rand(6)/10)
        this.particleMaxAge = 10 + this.rand.nextInt(7);
        this.setParticleTexture(sprite);
    }

    @Override
    public void onUpdate() {
        if (this.particleAge > this.particleMaxAge / 2) {
            this.particleAlpha = 1.0f - (float)(this.particleAge - this.particleMaxAge / 2) / (float)this.particleMaxAge;
        }
        super.onUpdate();
        if (this.onGround) {
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
            this.particleGravity = 0;
            this.setPosition(this.prevPosX, this.prevPosY + 0.1, this.prevPosZ);
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    public static void spawn(World world, double x, double y, double z,
                             float r, float g, float b, float scale,
                             TextureAtlasSprite sprite) {
        net.minecraft.client.Minecraft.getMinecraft().effectRenderer
                .addEffect(new SlimeParticle(world, x, y, z, r, g, b, scale, sprite));
    }
}
