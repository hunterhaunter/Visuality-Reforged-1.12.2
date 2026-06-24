package plus.dragons.visuality.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import plus.dragons.visuality.client.VisualityParticles;

/**
 * Flat horizontal water circle on a fluid surface. Port of 1.20
 * {@code WaterCircleParticle}.
 *
 * <p>Renders as a single quad lying in the XZ plane (normal up), NOT
 * billboarded. 5-frame animation over a 5–7 tick life. Alpha fades in the
 * second half.
 */
@SideOnly(Side.CLIENT)
public class WaterCircleParticle extends Particle {

    private final TextureAtlasSprite[] frames;

    public WaterCircleParticle(World world, double x, double y, double z,
                                float r, float g, float b,
                                TextureAtlasSprite[] frames) {
        super(world, x, y, z, 0, 0, 0);
        this.frames = frames;
        if (r > 0 && g > 0 && b > 0) {
            this.particleRed = r;
            this.particleGreen = g;
            this.particleBlue = b;
        }
        // Longer than modern's 5+rand(3): the port's flat-quad render shows a single crisp
        // ring (vs modern's TRANSLUCENT pass that visually lingers), so at modern lifetimes
        // each crown flashed by "superspeed / barely noticeable". ~2x life slows the
        // expand so the ripple reads like 1.20.1.
        this.particleMaxAge = 10 + this.rand.nextInt(4);
        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        this.particleScale = 0.4f; // world-space half-size of the flat ring (render uses it directly)
        this.setParticleTexture(frames[0]);
    }

    /** Set RGB tint from a packed hex integer (e.g. {@code 0x88CCFF}). */
    public void setColor(int rgbHex) {
        this.particleRed = ((rgbHex >> 16) & 0xFF) / 255.0f;
        this.particleGreen = ((rgbHex >> 8) & 0xFF) / 255.0f;
        this.particleBlue = (rgbHex & 0xFF) / 255.0f;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        // Gentle dim only - floor ~0.75 so the crown stays clearly visible dark-cyan to
        // the end and pops, rather than reading as a fade-out to transparent. (Modern's
        // 0.5 floor over a short life looked like it dissolved away; 1.20.1 keeps it
        // dark-cyan, NO transparent fade.)
        if (this.particleAge > this.particleMaxAge / 2) {
            float half = this.particleMaxAge / 2.0f;
            float t = (this.particleAge - half) / (this.particleMaxAge - half); // 0..1
            this.particleAlpha = 1.0f - 0.25f * t; // 1.0 -> 0.75
        }

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

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partial,
                               float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        float cx = (float) (this.prevPosX + (this.posX - this.prevPosX) * partial - interpPosX);
        float cy = (float) (this.prevPosY + (this.posY - this.prevPosY) * partial - interpPosY);
        float cz = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partial - interpPosZ);

        float s = this.particleScale;
        TextureAtlasSprite sprite = this.particleTexture;
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        int light = this.getBrightnessForRender(partial);
        int sky = light >> 16 & 0xFFFF;
        int block = light & 0xFFFF;
        float r = this.particleRed, g = this.particleGreen, b = this.particleBlue,
                a = this.particleAlpha;

        // Flat quad in the XZ plane (corners: -1,-1 / -1,+1 / +1,+1 / +1,-1 scaled)
        buffer.pos(cx - s, cy, cz - s).tex(u1, v1).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx - s, cy, cz + s).tex(u1, v0).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx + s, cy, cz + s).tex(u0, v0).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx + s, cy, cz - s).tex(u0, v1).color(r, g, b, a).lightmap(sky, block).endVertex();
    }

    /** Spawn a water circle at the given position.
     * @param color packed RGB hex, or {@code -1} for untinted white
     */
    public static void spawn(World world, double x, double y, double z, int color) {
        float r, g, b;
        if (color == -1) {
            r = 0;
            g = 0;
            b = 0;
        } else {
            r = ((color >> 16) & 0xFF) / 255.0f;
            g = ((color >> 8) & 0xFF) / 255.0f;
            b = (color & 0xFF) / 255.0f;
        }
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new WaterCircleParticle(world, x, y, z, r, g, b,
                        VisualityParticles.waterCircle()));
    }
}
