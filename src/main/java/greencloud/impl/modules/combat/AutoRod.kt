/*
*
* KingCanvas = Good Kotlin person...
*
*/
package greencloud.impl.modules.combat

import greencloud.GreenCloud
import greencloud.impl.managers.notification.NotificationManager
import greencloud.impl.modules.Category
import greencloud.impl.modules.Module
import greencloud.impl.settings.BooleanSetting
import greencloud.impl.settings.NumberSetting
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemFishingRod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Mouse
import java.util.function.Supplier

class AutoRod : Module("AutoRod", "Auto-Throws Rods.", Category.COMBAT) {

    private val onlyPlayers = BooleanSetting("Only Players", this, true)
    private val triggerLeftClick = BooleanSetting("On Left Click", this, false)
    private val checkFov = BooleanSetting("FOV Check", this, true)
    private val fovAmount = NumberSetting("FOV Range", this, 90.0, 10.0, 180.0, 5.0, checkFov::enabled)
    private val rodCooldown = NumberSetting("Cooldown", this, 500.0, 100.0, 2000.0, 50.0)
    private val holdDelay = NumberSetting("Hold Delay", this, 150.0, 0.0, 1000.0, 10.0)
    private val range = NumberSetting("Range", this, 4.5, 3.0, 6.0, 0.1)

    private var lastRodTime = 0L
    private var lastSlot = -1
    private var isProcessing = false
    private var hasThrown = false
    private var currentTarget: EntityPlayer? = null

    init {
        addSettings(onlyPlayers, triggerLeftClick, checkFov, fovAmount, rodCooldown, holdDelay, range)
    }

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return

        val currentTime = System.currentTimeMillis()

        if (isProcessing && hasThrown) {
            if (currentTime - lastRodTime > holdDelay.value.toLong()) {
                if (lastSlot != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot
                    lastSlot = -1
                }
                isProcessing = false
                hasThrown = false
                lastRodTime = System.currentTimeMillis()
            }
            return
        }

        if (currentTime - lastRodTime < rodCooldown.value.toLong() || isProcessing) return
        if (triggerLeftClick.enabled && !Mouse.isButtonDown(0)) return

        val target = findTarget() ?: return
        currentTarget = target

        if (mc.thePlayer.getDistanceToEntity(target).toDouble() > range.value) return
        if (checkFov.enabled && !isInView(target, fovAmount.value.toFloat())) return

        val slot = findRodSlot()
        if (slot != -1) {
            isProcessing = true
            lastSlot = mc.thePlayer.inventory.currentItem
            mc.thePlayer.inventory.currentItem = slot

            useRod()
            hasThrown = true
            lastRodTime = System.currentTimeMillis()
        }
    }

    private fun findTarget(): EntityPlayer? {
        var closest: EntityPlayer? = null
        var closestDist = java.lang.Double.MAX_VALUE

        for (player in mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || !player.isEntityAlive) continue
            if (onlyPlayers.enabled && player !is EntityPlayer) continue

            val dist = mc.thePlayer.getDistanceToEntity(player).toDouble()
            if (dist <= range.value && dist < closestDist) {
                closestDist = dist
                closest = player as EntityPlayer
            }
        }
        return closest
    }

    private fun isInView(entity: EntityPlayer, fov: Float): Boolean {
        var yaw = mc.thePlayer.rotationYaw % 360
        if (yaw < 0) yaw += 360f

        val dX = entity.posX - mc.thePlayer.posX
        val dZ = entity.posZ - mc.thePlayer.posZ
        var targetYaw = (Math.atan2(dZ, dX) * 180.0 / Math.PI).toFloat() - 90.0f
        if (targetYaw < 0) targetYaw += 360f

        val diff = Math.abs(yaw - targetYaw)
        return diff <= fov / 2 || diff >= 360 - fov / 2
    }

    private fun findRodSlot(): Int {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.item is ItemFishingRod) return i
        }
        return -1
    }

    private fun useRod() {
        if (mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem)) {
            mc.thePlayer.swingItem()
        }
    }

    override fun onDisable() {
        isProcessing = false
        hasThrown = false
        lastSlot = -1
        super.onDisable()
    }
}