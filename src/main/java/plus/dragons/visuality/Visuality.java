package plus.dragons.visuality;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import plus.dragons.visuality.proxy.CommonProxy;

/**
 * Visuality: Reforged - client-side particle mod, ported from MC 1.20.x to 1.12.2.
 *
 * <p>Client only: all behavior spawns client particles. {@code acceptableRemoteVersions = "*"}
 * lets it join any server (including vanilla / servers without the mod).
 */
@Mod(modid = Visuality.ID, name = Visuality.NAME, version = Visuality.VERSION,
        clientSideOnly = true, acceptableRemoteVersions = "*",
        guiFactory = "plus.dragons.visuality.client.config.VisualityGuiFactory")
public class Visuality {

    public static final String ID = "visuality";
    public static final String NAME = "Visuality: Reforged";
    public static final String VERSION = "2.0.2";

    public static final Logger LOGGER = LogManager.getLogger("Visuality");

    @SidedProxy(clientSide = "plus.dragons.visuality.proxy.ClientProxy",
                serverSide = "plus.dragons.visuality.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    public static ResourceLocation location(String path) {
        return new ResourceLocation(ID, path);
    }
}
