package greencloud.event;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Field;

@Cancelable
public class PacketEvent extends Event {

    private final Packet<?> packet;
    private final Direction direction;

    private static Field xField, yField, zField;
    private static Field yawField, pitchField;
    private static Field onGroundField, movingField, rotatingField;
    private static boolean fieldsInitialized = false;

    private static void initFields() {
        if (fieldsInitialized) return;
        try {
            xField = getField("field_149479_a", "x");
            yField = getField("field_149477_b", "y");
            zField = getField("field_149478_c", "z");
            yawField = getField("field_149476_e", "yaw");
            pitchField = getField("field_149473_f", "pitch");
            onGroundField = getField("field_149474_g", "onGround");
            movingField = getField("field_149480_h", "moving");
            rotatingField = getField("field_149481_i", "rotating");
            fieldsInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Field getField(String obf, String deobf) throws NoSuchFieldException {
        try {
            Field f = C03PacketPlayer.class.getDeclaredField(obf);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            Field f = C03PacketPlayer.class.getDeclaredField(deobf);
            f.setAccessible(true);
            return f;
        }
    }

    public PacketEvent(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
        initFields();
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setPosition(double x, double y, double z) {
        if (packet instanceof C03PacketPlayer) {
            try {
                xField.setDouble(packet, x);
                yField.setDouble(packet, y);
                zField.setDouble(packet, z);
                movingField.setBoolean(packet, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setRotation(float yaw, float pitch) {
        if (packet instanceof C03PacketPlayer) {
            try {
                yawField.setFloat(packet, yaw);
                pitchField.setFloat(packet, pitch);
                rotatingField.setBoolean(packet, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnGround(boolean onGround) {
        if (packet instanceof C03PacketPlayer) {
            try {
                onGroundField.setBoolean(packet, onGround);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public enum Direction {
        INCOMING,
        OUTGOING
    }
}