package greencloud.impl.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class EntityUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();


    public static boolean isTeam(EntityLivingBase entity) {
        if (entity == null || mc.thePlayer == null) return false;


        if (mc.thePlayer.isOnSameTeam(entity)) {
            return true;
        }


        String targetName = entity.getDisplayName().getFormattedText();
        String localName = mc.thePlayer.getDisplayName().getFormattedText();

        if (targetName.length() >= 2 && localName.length() >= 2) {
            if (targetName.startsWith("\u00a7") && localName.startsWith("\u00a7")) {
                return targetName.charAt(1) == localName.charAt(1);
            }
        }

        return false;
    }


    public static boolean isValidTarget(EntityLivingBase entity, double range, boolean attackPlayers, boolean attackMobs, boolean attackAnimals, boolean attackInvisible, boolean checkTeams) {
        if (entity == null || entity == mc.thePlayer) return false;


        if (entity.getHealth() <= 0 || entity.isDead) return false;
        if (mc.thePlayer.getDistanceToEntity(entity) > range) return false;
        if (!attackInvisible && entity.isInvisible()) return false;


        if (checkTeams && isTeam(entity)) return false;


        if (entity instanceof EntityPlayer) {
            return attackPlayers;

        }

        if (isMob(entity)) {
            return attackMobs;
        }

        if (isAnimal(entity)) {
            return attackAnimals;
        }

        return false;
    }

    public static boolean isAnimal(Entity entity) {
        return entity instanceof EntityAnimal || entity instanceof EntityVillager;
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof EntityMob || entity instanceof EntitySlime;
    }
}