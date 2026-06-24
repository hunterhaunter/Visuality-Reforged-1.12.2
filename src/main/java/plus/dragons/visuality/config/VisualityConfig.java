package plus.dragons.visuality.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import plus.dragons.visuality.Visuality;

import java.io.File;

/**
 * Visuality config - replaces the modern ForgeConfigSpec (TOML) + ReloadableJsonConfig
 * (codec/reload-listener) system with a single 1.12.2 Forge {@link Configuration}.
 *
 * <p>Live-editable in-game: the Mods list &rarr; Visuality &rarr; Config button opens
 * {@code VisualityGuiConfig}; on close Forge posts {@link ConfigChangedEvent.OnConfigChangedEvent},
 * {@link #load()} re-reads every field, and because each feature reads these static flags
 * fresh every tick/event the changes take effect immediately (no game restart).
 *
 * <p>Per-emitter entry data (which blocks/entities emit which particle) lives in
 * {@link ParticleEmitters} as hardcoded defaults; these toggles only gate the features.
 */
public final class VisualityConfig {

    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_WATER = "waterCircle";

    public static boolean slimeEnabled = true;
    public static boolean chargeEnabled = true;
    public static boolean soulEnabled = true;
    public static boolean hitParticlesEnabled = true;
    public static boolean shinyArmorEnabled = true;
    public static boolean shinyBlocksEnabled = true;

    /** Hit-particle burst count is clamp(ceil(attackDamage), min, max) - mirrors modern EntityHitParticleConfig. */
    public static int hitMinAmount = 1;
    public static int hitMaxAmount = 20;

    public static boolean waterCircleEnabled = true;
    public static boolean waterCircleColored = true;
    public static boolean waterCircleForce = false;
    public static int waterCircleDensity = 16;

    /**
     * Shared, mod-neutral JVM property used by multiple mods to cooperatively claim the
     * "rain ripple on water" effect so only one renders it. First mod to claim wins;
     * others defer (unless they force-enable). No shared library / hard dependency needed -
     * any mod can participate by reading/writing this exact key.
     */
    public static final String RAIN_RIPPLE_OWNER_KEY = "minecraft.rainRipple.owner";

    private static Configuration config;

    public static void init(File file) {
        config = new Configuration(file);
        load();
        MinecraftForge.EVENT_BUS.register(new VisualityConfig());
    }

    /** The backing Forge config - used by the in-game config GUI to build its element list. */
    public static Configuration getConfig() {
        return config;
    }

    private static void load() {
        config.getCategory(CATEGORY_GENERAL).setLanguageKey("visuality.config.general");
        slimeEnabled = bool(CATEGORY_GENERAL, "slimeEnabled", true,
                "Slime blob particles on landing", "visuality.config.slime");
        chargeEnabled = bool(CATEGORY_GENERAL, "chargeEnabled", true,
                "Charge particles on charged creepers", "visuality.config.charge");
        soulEnabled = bool(CATEGORY_GENERAL, "soulEnabled", true,
                "Soul particles when walking on soul sand/soil", "visuality.config.soul");
        hitParticlesEnabled = bool(CATEGORY_GENERAL, "hitParticlesEnabled", true,
                "Extra particles when entities are hit", "visuality.config.hitParticles");
        shinyArmorEnabled = bool(CATEGORY_GENERAL, "shinyArmorEnabled", true,
                "Sparkles around entities wearing configured armor", "visuality.config.shinyArmor");
        shinyBlocksEnabled = bool(CATEGORY_GENERAL, "shinyBlocksEnabled", true,
                "Ambient sparkles around configured blocks", "visuality.config.shinyBlocks");
        hitMinAmount = integer(CATEGORY_GENERAL, "hitMinAmount", 1, 0, 64,
                "Minimum hit particles spawned per attack", "visuality.config.hitParticles.min");
        hitMaxAmount = integer(CATEGORY_GENERAL, "hitMaxAmount", 20, 1, 64,
                "Maximum hit particles spawned per attack", "visuality.config.hitParticles.max");
        if (hitMaxAmount < hitMinAmount) {
            hitMaxAmount = hitMinAmount;
        }

        config.getCategory(CATEGORY_WATER).setLanguageKey("visuality.config.waterCircle");
        waterCircleEnabled = bool(CATEGORY_WATER, "enabled", true,
                "Water circle particles in rain", "visuality.config.waterCircle");
        waterCircleColored = bool(CATEGORY_WATER, "colored", true,
                "Tint water circles by biome water color", "visuality.config.waterCircle.colored");
        waterCircleForce = bool(CATEGORY_WATER, "force", false,
                "Render water circles even if another mod already claimed the rain-ripple effect",
                "visuality.config.waterCircle.force");
        waterCircleDensity = integer(CATEGORY_WATER, "density", 16, 0, 64,
                "Water circle spawn density", "visuality.config.waterCircle.density");

        if (waterCircleEnabled) {
            waterCircleEnabled = claimRainRipple();
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static boolean bool(String category, String key, boolean def, String comment, String langKey) {
        Property prop = config.get(category, key, def, comment);
        prop.setLanguageKey(langKey);
        return prop.getBoolean();
    }

    private static int integer(String category, String key, int def, int min, int max, String comment, String langKey) {
        Property prop = config.get(category, key, def, comment, min, max);
        prop.setLanguageKey(langKey);
        return prop.getInt();
    }

    /**
     * Cooperative claim of the shared rain-ripple effect. Returns whether Visuality
     * should render water circles: true if we claimed it (or forced), false if another
     * mod already owns it.
     */
    private static boolean claimRainRipple() {
        if (waterCircleForce) {
            return true;
        }
        synchronized (VisualityConfig.class) {
            String owner = System.getProperty(RAIN_RIPPLE_OWNER_KEY);
            if (owner == null || owner.isEmpty()) {
                System.setProperty(RAIN_RIPPLE_OWNER_KEY, Visuality.ID);
                return true;
            }
            if (Visuality.ID.equals(owner)) {
                return true;
            }
            Visuality.LOGGER.info("Rain-ripple effect already provided by '{}' - Visuality water circles deferred "
                    + "(set waterCircle.force=true to override)", owner);
            return false;
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Visuality.ID.equals(event.getModID())) {
            load();
        }
    }
}
