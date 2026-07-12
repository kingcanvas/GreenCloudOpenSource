package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.StringSetting;
import greencloud.impl.utils.render.CapeUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

public class Capes extends Module {

    public ModeSetting capeStyle = new ModeSetting("Style", this, "moonclient", "moonclient", "Sonic",
            "GreenCloud", "KingCanvas", "SparkyEclipse", "SrNooby", "DivineKill",
            "Checkered", "DOOM", "Optifine", "Smile", "Moon", "Purple", "Black",
            "Doggy", "Blue", "RedVirus", "Neurosis", "Sonic", "JapEnderman", "BlueDev", "DuckCape", "Felix", "Furina", "Furina2", "Furina3");

    public StringSetting capeSearch = new StringSetting("Search Cape...", this, "");

    private final ResourceLocation greenCloudCape = new ResourceLocation("greencloud", "cape/greencloud.png");
    private final ResourceLocation sparkyEclipseCape = new ResourceLocation("greencloud", "cape/sparkyeclipse.png");
    private final ResourceLocation kingCanvasCape = new ResourceLocation("greencloud", "cape/kingcanvas.png");
    private final ResourceLocation divineKillCape = new ResourceLocation("greencloud", "cape/divinekill.png");
    private final ResourceLocation srNoobyCape = new ResourceLocation("greencloud", "cape/srnooby.png");
    private final ResourceLocation checkeredCape = new ResourceLocation("greencloud", "cape/checkered.png");
    private final ResourceLocation DOOMCape = new ResourceLocation("greencloud", "cape/DOOM.png");
    private final ResourceLocation OptifineCape = new ResourceLocation("greencloud", "cape/Optifine.png");
    private final ResourceLocation SmileCape = new ResourceLocation("greencloud", "cape/Smile.png");
    private final ResourceLocation MoonCape = new ResourceLocation("greencloud", "cape/Moon.png");
    private final ResourceLocation PurpleCape = new ResourceLocation("greencloud", "cape/Purple.png");
    private final ResourceLocation BlackCape = new ResourceLocation("greencloud", "cape/BlackCape.png");
    private final ResourceLocation DoggyCape = new ResourceLocation("greencloud", "cape/Doggy.png");
    private final ResourceLocation BlueCape = new ResourceLocation("greencloud", "cape/Blue.png");
    private final ResourceLocation NeurosisCape = new ResourceLocation("greencloud", "cape/Neurosis.png");
    private final ResourceLocation SonicCape = new ResourceLocation("greencloud", "cape/Sonic.png");
    private final ResourceLocation BlueDevCape = new ResourceLocation("greencloud", "cape/BlueDev.png");
    private final ResourceLocation DuckCape = new ResourceLocation("greencloud", "cape/DuckCape.png");
    private final ResourceLocation FelixCape = new ResourceLocation("greencloud", "cape/Felix.png");
    private final ResourceLocation FurinaCape = new ResourceLocation("greencloud", "cape/Furina.png");
    private final ResourceLocation Furina2Cape = new ResourceLocation("greencloud", "cape/Furina2.png");
    private final ResourceLocation Furina3Cape = new ResourceLocation("greencloud", "cape/Furina3.png");
    private final ResourceLocation moonclientCape = new ResourceLocation("greencloud", "cape/moonclient.png");

    private Field locationCapeField;
    private int ticks = 0;

    public Capes() {
        super("Capes", "Renders a custom client cape.", Category.RENDER);
        addSetting(capeStyle);
        setupReflection();
    }

    private void setupReflection() {
        try {
            locationCapeField = NetworkPlayerInfo.class.getDeclaredField("locationCape");
        } catch (NoSuchFieldException e) {
            try {
                locationCapeField = NetworkPlayerInfo.class.getDeclaredField("field_178862_f");
            } catch (Exception ex) {
                GreenCloud.logger.error("Failed to setup Cape Reflection!");
            }
        }

        if (locationCapeField != null) {
            locationCapeField.setAccessible(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            setCape(mc.thePlayer, null);
        }
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || locationCapeField == null) return;
        if (event.phase != TickEvent.Phase.END) return;

        ticks++;
        ResourceLocation selected = null;
        String mode = capeStyle.currentMode;
        String query = capeSearch.getValue().trim();
        if (!query.isEmpty()) {
            for (String m : capeStyle.modes) {
                if (m.toLowerCase().startsWith(query.toLowerCase())) {
                    mode = m;
                    capeStyle.currentMode = m;
                    break;
                }
            }
        }
        if (mode.equals("KingCanvas")) selected = kingCanvasCape;
        else if (mode.equals("SparkyEclipse")) selected = sparkyEclipseCape;
        else if (mode.equals("DivineKill")) selected = divineKillCape;
        else if (mode.equals("SrNooby")) selected = srNoobyCape;
        else if (mode.equals("Checkered")) selected = checkeredCape;
        else if (mode.equals("DOOM")) selected = DOOMCape;
        else if (mode.equals("Optifine")) selected = OptifineCape;
        else if (mode.equals("Smile")) selected = SmileCape;
        else if (mode.equals("Moon")) selected = MoonCape;
        else if (mode.equals("moonclient")) selected = moonclientCape;
        else if (mode.equals("Purple")) selected = PurpleCape;
        else if (mode.equals("Black")) selected = BlackCape;
        else if (mode.equals("Doggy")) selected = DoggyCape;
        else if (mode.equals("Blue")) selected = BlueCape;
        else if (mode.equals("GreenCloud")) selected = greenCloudCape;
        else if (mode.equals("Neurosis")) selected = NeurosisCape;
        else if (mode.equals("BlueDev")) selected = BlueDevCape;
        else if (mode.equals("DuckCape")) selected = DuckCape;
        else if (mode.equals("Felix")) selected = FelixCape;
        else if (mode.equals("Furina")) selected = FurinaCape;
        else if (mode.equals("Furina2")) selected = Furina2Cape;
        else if (mode.equals("Furina3")) selected = Furina3Cape;

        else if (mode.equals("RedVirus")) {
            selected = CapeUtils.getAnimatedCape("RedVirus", ticks, 3);
        }
        else if (mode.equals("JapEnderman")) {
            selected = CapeUtils.getAnimatedCape("JapEnderman", ticks, 3);
        }

        if (selected != null) {
            setCape(mc.thePlayer, selected);
        }
    }

    private void setCape(AbstractClientPlayer player, ResourceLocation cape) {
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info == null) return;

        try {
            locationCapeField.set(info, cape);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}