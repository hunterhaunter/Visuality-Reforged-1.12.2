package plus.dragons.visuality.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import plus.dragons.visuality.client.VisualityParticles;
import plus.dragons.visuality.config.VisualityConfig;
import plus.dragons.visuality.particle.ChargeParticle;
import plus.dragons.visuality.particle.FeatherParticle;
import plus.dragons.visuality.particle.SolidFallingParticle;
import plus.dragons.visuality.particle.SparkleParticle;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Forge event hub (client-side). Ports the modern event/config-driven triggers:
 *
 * <ul>
 *   <li>Charge particles on charged creepers - modern LivingTickEvent + PowerableMob.</li>
 *   <li>Hit particles - modern LivingAttackEvent (which fires server-side in 1.12.2 and
 *       cannot reach the client renderer), reimplemented as a client-side hurt detector
 *       via the {@code hurtTime == maxHurtTime} transition. Trade-off: no server
 *       damage value on the client, so a fixed burst count is used instead of
 *       damage-scaled count.</li>
 *   <li>Shiny armor sparkles - modern render-layer model probing, approximated here by
 *       sampling the entity bounding box per equipment slot (no model-part access in 1.12.2).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class VisualityEvents {

    private static final int ARMOR_INTERVAL = 20;
    // Modern armor sparkle tints (resolved from 1.20.1 EntityArmorParticleConfig defaults):
    //   DIAMOND armor -> 11861486 = 0xB4FDEE (pale aqua)
    //   GOLD armor    -> 16711613 = 0xFEFFBD (pale yellow)
    // (the original's 2nd entry is GOLD, not netherite - both port cleanly to 1.12.2)
    private static final int DIAMOND_ARMOR_TINT = 0xB4FDEE;
    private static final int GOLD_ARMOR_TINT = 0xFEFFBD;

    /** Armor item -> sparkle tint (matches the 1.20.1 default: diamond + gold). */
    private static final Map<Item, Integer> ARMOR_SPARKLE = new IdentityHashMap<Item, Integer>();

    static {
        ARMOR_SPARKLE.put(Items.DIAMOND_HELMET, DIAMOND_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.DIAMOND_CHESTPLATE, DIAMOND_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.DIAMOND_LEGGINGS, DIAMOND_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.DIAMOND_BOOTS, DIAMOND_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.GOLDEN_HELMET, GOLD_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.GOLDEN_CHESTPLATE, GOLD_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.GOLDEN_LEGGINGS, GOLD_ARMOR_TINT);
        ARMOR_SPARKLE.put(Items.GOLDEN_BOOTS, GOLD_ARMOR_TINT);
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new VisualityEvents());
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if (!world.isRemote) {
            return;
        }
        spawnChargeParticles(entity, world);
        spawnArmorParticles(entity, world);
    }

    /**
     * Hit particles. Modern fires on {@code LivingAttackEvent} (server-side in 1.12.2,
     * never reaches the client renderer) and scales the count by the attacker's weapon
     * damage: {@code count = clamp(ceil(attackDamage), 1, 20)}. We reproduce this on the
     * client via Forge's {@link AttackEntityEvent} (fired client-side when the local
     * player swings), reading the player's mainhand ATTACK_DAMAGE attribute - bare fist
     * = 1.0 -> 1 particle, swords scale up (diamond ~7 -> 7), exactly like modern. This
     * only covers player melee (projectile/mob hits have no clean client signal in 1.12.2).
     */
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!VisualityConfig.hitParticlesEnabled) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;
        if (!world.isRemote) {
            return;
        }
        Entity target = event.getTarget();
        if (!(target instanceof EntityLivingBase)) {
            return;
        }
        double damage = computeAttackDamage(player);
        if (damage <= 0.0) {
            return;
        }
        int count = MathHelper.clamp(MathHelper.ceil(damage),
                VisualityConfig.hitMinAmount, VisualityConfig.hitMaxAmount);

        double x = target.posX;
        double y = target.posY + target.height * 0.5;
        double z = target.posZ;

        if (target instanceof EntityWitherSkeleton) {
            burstSolid(world, x, y, z, VisualityParticles.witherBone(), count);
        } else if (target instanceof EntitySkeleton || target instanceof EntityStray) {
            burstSolid(world, x, y, z, VisualityParticles.bone(), count);
        } else if (target instanceof EntityVillager) {
            burstSolid(world, x, y, z, VisualityParticles.emerald(), count);
        } else if (target instanceof EntityChicken) {
            for (int i = 0; i < count; i++) {
                FeatherParticle.spawn(world, x, y, z, 0, 0, 0);
            }
        }
    }

    /**
     * Computes the player's effective melee attack damage from the mainhand item's
     * ATTACK_DAMAGE modifiers, mirroring the modern {@code EntityHitParticleConfig.getAttackDamage}.
     *
     * <p>Necessary because 1.12.2 applies held-item attribute modifiers to the attribute map
     * <b>server-side only</b> - {@code EntityLivingBase}'s equipment loop casts {@code world}
     * to {@code WorldServer}, so it never runs on the client. Reading
     * {@code getAttributeValue()} on the client therefore always returns the base 1.0
     * regardless of weapon, which made every hit spawn exactly one particle.
     */
    private static double computeAttackDamage(EntityPlayer player) {
        double base = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue();
        ItemStack held = player.getHeldItemMainhand();
        double addition = 0.0;
        double multiplyBase = 1.0;
        double multiplyTotal = 1.0;
        for (AttributeModifier modifier : held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            switch (modifier.getOperation()) {
                case 0: // ADDITION
                    addition += modifier.getAmount();
                    break;
                case 1: // MULTIPLY_BASE
                    multiplyBase += modifier.getAmount();
                    break;
                case 2: // MULTIPLY_TOTAL
                    multiplyTotal *= 1.0 + modifier.getAmount();
                    break;
            }
        }
        return (base + addition) * multiplyBase * multiplyTotal;
    }

    private void spawnChargeParticles(EntityLivingBase entity, World world) {
        if (!VisualityConfig.chargeEnabled) {
            return;
        }
        if (!(entity instanceof EntityCreeper) || !((EntityCreeper) entity).getPowered()) {
            return;
        }
        if (!entity.isEntityAlive() || entity.getRNG().nextInt(20) != 0) {
            return;
        }
        AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(0.5);
        double x = lerp(entity.getRNG().nextDouble(), aabb.minX, aabb.maxX);
        double y = lerp(entity.getRNG().nextDouble(), aabb.minY, aabb.maxY);
        double z = lerp(entity.getRNG().nextDouble(), aabb.minZ, aabb.maxZ);
        ChargeParticle.spawn(world, x, y, z);
    }

    private void burstSolid(World world, double x, double y, double z,
                            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int count) {
        if (sprite == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            SolidFallingParticle.spawn(world, x, y, z, 0, 0, 0, sprite);
        }
    }

    private void spawnArmorParticles(EntityLivingBase entity, World world) {
        if (!VisualityConfig.shinyArmorEnabled || !entity.isEntityAlive()) {
            return;
        }
        if (entity.getRNG().nextInt(ARMOR_INTERVAL) != 0) {
            return;
        }
        // skip the local player in first person (no body to decorate)
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == entity && mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        if (VisualityParticles.sparkle() == null) {
            return;
        }

        double height = entity.getRNG().nextDouble();
        EntityEquipmentSlot slot = slotFromHeight(height);
        ItemStack stack = entity.getItemStackFromSlot(slot);
        Integer color = ARMOR_SPARKLE.get(stack.getItem());
        if (color == null) {
            return;
        }
        AxisAlignedBB aabb = entity.getEntityBoundingBox();
        double radian = Math.PI * 2 * entity.getRNG().nextDouble();
        double x = lerp(0.5 + 0.75 * Math.cos(radian), aabb.minX, aabb.maxX);
        double y = lerp(height, aabb.minY, aabb.maxY);
        double z = lerp(0.5 + 0.75 * Math.sin(radian), aabb.minZ, aabb.maxZ);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        SparkleParticle.spawn(world, x, y, z, r, g, b);
    }

    private static EntityEquipmentSlot slotFromHeight(double height) {
        if (height < 0.1875) {
            return EntityEquipmentSlot.FEET;
        }
        if (height < 0.5) {
            return EntityEquipmentSlot.LEGS;
        }
        if (height < 0.8125) {
            return EntityEquipmentSlot.CHEST;
        }
        return EntityEquipmentSlot.HEAD;
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}
