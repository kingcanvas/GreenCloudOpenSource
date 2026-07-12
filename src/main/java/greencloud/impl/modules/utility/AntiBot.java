package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldSettings;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiBot extends Module {

    public static AntiBot instance;

    private final BooleanSetting invalidGround = new BooleanSetting("Invalid Ground",  this, true);
    private final BooleanSetting noTablist     = new BooleanSetting("No Tablist",      this, true);
    private final BooleanSetting invalidPing   = new BooleanSetting("Invalid Ping",    this, true);
    private final BooleanSetting duplicateName = new BooleanSetting("Duplicate Name",  this, true);
    private final BooleanSetting gamemodCheck  = new BooleanSetting("Gamemode Check",  this, true);
    private final BooleanSetting selfCheck     = new BooleanSetting("Self Check",      this, true);

    private final Set<UUID> botCache = ConcurrentHashMap.newKeySet();

    public AntiBot() {
        super("AntiBot", "Filters bots from combat and render modules.", Category.MISC);
        instance = this;
        addSettings(invalidGround, noTablist, invalidPing, duplicateName, gamemodCheck, selfCheck);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        botCache.clear();
    }

    @Override
    public void onDisable() {
        botCache.clear();
        super.onDisable();
    }

    public boolean isBot(EntityPlayer p) {
        if (p == null) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        if (botCache.contains(p.getUniqueID())) return true;

        if (selfCheck.enabled && p == mc.thePlayer) return false;

        if (noTablist.enabled) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(p.getUniqueID());
            if (info == null) {
                cache(p);
                return true;
            }
        }

        if (invalidPing.enabled) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(p.getUniqueID());
            if (info != null && info.getResponseTime() <= 0) {
                cache(p);
                return true;
            }
        }

        if (gamemodCheck.enabled) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(p.getUniqueID());
            if (info != null) {
                WorldSettings.GameType gm = info.getGameType();
                if (gm == WorldSettings.GameType.CREATIVE || gm == WorldSettings.GameType.SPECTATOR) {
                    cache(p);
                    return true;
                }
            }
        }

        if (duplicateName.enabled) {
            for (net.minecraft.entity.Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityPlayer)) continue;
                EntityPlayer other = (EntityPlayer) entity;
                if (other == p) continue;
                if (other.getName().equalsIgnoreCase(p.getName())) {
                    cache(p);
                    return true;
                }
            }
        }

        if (invalidGround.enabled) {
            if (p.onGround && mc.theWorld.getCollidingBoundingBoxes(p, p.getEntityBoundingBox().offset(0, -0.01, 0)).isEmpty()) {
                if (p.posY % 1.0 != 0 && p.motionX == 0 && p.motionZ == 0 && p.motionY == 0) {
                    cache(p);
                    return true;
                }
            }
        }

        return false;
    }

    private void cache(EntityPlayer p) {
        botCache.add(p.getUniqueID());
    }

    public void clearCache() {
        botCache.clear();
    }
}