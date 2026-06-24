package plus.dragons.visuality.client.config;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import plus.dragons.visuality.Visuality;
import plus.dragons.visuality.config.VisualityConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game config screen (Mods list &rarr; Visuality &rarr; Config). Built from the two
 * Forge config categories; on Done, Forge posts {@code OnConfigChangedEvent} (parent here
 * is the mod list, not a GuiConfig) which {@link VisualityConfig} listens for to re-read
 * every field live - no restart needed.
 */
@SideOnly(Side.CLIENT)
public class VisualityGuiConfig extends GuiConfig {

    public VisualityGuiConfig(GuiScreen parent) {
        super(parent, elements(), Visuality.ID, false, false,
                I18n.format("visuality.config.title"));
    }

    private static List<IConfigElement> elements() {
        List<IConfigElement> list = new ArrayList<IConfigElement>();
        list.add(new ConfigElement(VisualityConfig.getConfig().getCategory(VisualityConfig.CATEGORY_GENERAL)));
        list.add(new ConfigElement(VisualityConfig.getConfig().getCategory(VisualityConfig.CATEGORY_WATER)));
        return list;
    }
}
