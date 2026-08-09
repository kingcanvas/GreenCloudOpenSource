package greencloudclient.com.modules.render;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.StringSetting;
import greencloudclient.com.utils.render.CapeUtils;
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

    private final ResourceLocation greenCloudCape = new ResourceLocation("greencloudclient", "cape/greencloud.png");
    private final ResourceLocation sparkyEclipseCape = new ResourceLocation("greencloudclient", "cape/sparkyeclipse.png");
    private final ResourceLocation kingCanvasCape = new ResourceLocation("greencloudclient", "cape/kingcanvas.png");
    private final ResourceLocation divineKillCape = new ResourceLocation("greencloudclient", "cape/divinekill.png");
    private final ResourceLocation srNoobyCape = new ResourceLocation("greencloudclient", "cape/srnooby.png");
    private final ResourceLocation checkeredCape = new ResourceLocation("greencloudclient", "cape/checkered.png");
    private final ResourceLocation DOOMCape = new ResourceLocation("greencloudclient", "cape/DOOM.png");
    private final ResourceLocation OptifineCape = new ResourceLocation("greencloudclient", "cape/Optifine.png");
    private final ResourceLocation SmileCape = new ResourceLocation("greencloudclient", "cape/Smile.png");
    private final ResourceLocation MoonCape = new ResourceLocation("greencloudclient", "cape/Moon.png");
    private final ResourceLocation PurpleCape = new ResourceLocation("greencloudclient", "cape/Purple.png");
    private final ResourceLocation BlackCape = new ResourceLocation("greencloudclient", "cape/BlackCape.png");
    private final ResourceLocation DoggyCape = new ResourceLocation("greencloudclient", "cape/Doggy.png");
    private final ResourceLocation BlueCape = new ResourceLocation("greencloudclient", "cape/Blue.png");
    private final ResourceLocation NeurosisCape = new ResourceLocation("greencloudclient", "cape/Neurosis.png");
    private final ResourceLocation SonicCape = new ResourceLocation("greencloudclient", "cape/Sonic.png");
    private final ResourceLocation BlueDevCape = new ResourceLocation("greencloudclient", "cape/BlueDev.png");
    private final ResourceLocation DuckCape = new ResourceLocation("greencloudclient", "cape/DuckCape.png");
    private final ResourceLocation FelixCape = new ResourceLocation("greencloudclient", "cape/Felix.png");
    private final ResourceLocation FurinaCape = new ResourceLocation("greencloudclient", "cape/Furina.png");
    private final ResourceLocation Furina2Cape = new ResourceLocation("greencloudclient", "cape/Furina2.png");
    private final ResourceLocation Furina3Cape = new ResourceLocation("greencloudclient", "cape/Furina3.png");
    private final ResourceLocation moonclientCape = new ResourceLocation("greencloudclient", "cape/moonclient.png");

    private Field locationCapeField;
    private int ticks = 0;

    public Capes() {
        super("Capes", Category.RENDER);
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