package greencloudclient.com.modules.impl.combat

import greencloudclient.com.modules.Category
import greencloudclient.com.modules.Module
import greencloudclient.com.settings.NumberSetting
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Mouse
import java.security.SecureRandom

class BlockHit : Module("BlockHit", Category.COMBAT) {
    
    private val chance = NumberSetting("Chance %", this, 80.0, 100.0, 0.0, 100.0, 1.0, true)
    private val delay = NumberSetting("Delay", this, 0.0, 50.0, 0.0, 500.0, 1.0, true)
    private val hold = NumberSetting("Hold", this, 40.0, 150.0, 0.0, 500.0, 1.0, true)

    private var blockTriggerTime: Long = -1
    private var releaseTriggerTime: Long = -1
    private var isBlockingActive: Boolean = false

    private val random = SecureRandom()

    init {
        addSettings(chance, delay, hold)
    }

    override fun onDisable() {
        super.onDisable()
        terminateBlock()
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return

        handleTimings()

        if (mc.currentScreen != null || !Mouse.isButtonDown(0)) {
            if (blockTriggerTime != -1L) blockTriggerTime = -1
            return
        }

        if (shouldAttemptBlock()) scheduleBlock()
    }

    private fun shouldAttemptBlock(): Boolean {
        val player = mc.thePlayer ?: return false
        if (!isHoldingSword(player)) return false
        if (getTargetEntity() == null) return false
        if (player.swingProgressInt != 1) return false

        val chanceMin = chance.value.toDouble()
        val chanceMax = chance.maxValue.toDouble()
        val actualChance = if (chanceMax > chanceMin) {
            chanceMin + random.nextDouble() * (chanceMax - chanceMin)
        } else {
            chanceMin
        }

        return random.nextDouble() * 100 <= actualChance
    }

    private fun scheduleBlock() {
        if (blockTriggerTime != -1L) return

        val delayMin = delay.value.toDouble()
        val delayMax = delay.maxValue.toDouble()
        
        val waitMs = if (delayMax > delayMin) {
            delayMin + random.nextDouble() * (delayMax - delayMin)
        } else {
            delayMin
        }

        blockTriggerTime = System.currentTimeMillis() + waitMs.toLong()
    }

    private fun handleTimings() {
        val now = System.currentTimeMillis()

        if (blockTriggerTime != -1L && now >= blockTriggerTime) {
            activateBlock()
            blockTriggerTime = -1

            val holdMin = hold.value.toDouble()
            val holdMax = hold.maxValue.toDouble()
            val holdTime = if (holdMax > holdMin) {
                holdMin + random.nextDouble() * (holdMax - holdMin)
            } else {
                holdMin
            }

            releaseTriggerTime = now + holdTime.toLong()
        }

        if (releaseTriggerTime != -1L && now >= releaseTriggerTime) {
            terminateBlock()
        }
    }

    private fun activateBlock() {
        if (isBlockingActive) return
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
        isBlockingActive = true
    }

    private fun terminateBlock() {
        if (!isBlockingActive) return

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)

        isBlockingActive = false
        releaseTriggerTime = -1
        blockTriggerTime = -1
    }

    private fun isHoldingSword(player: net.minecraft.entity.player.EntityPlayer): Boolean {
        val item = player.heldItem ?: return false
        return item.item is ItemSword
    }

    private fun getTargetEntity(): EntityLivingBase? {
        val mop = mc.objectMouseOver ?: return null
        if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) return null

        val entity = mop.entityHit
        return if (entity is EntityLivingBase && !entity.isDead) entity else null
    }
}
