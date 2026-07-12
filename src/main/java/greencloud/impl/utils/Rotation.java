package greencloud.impl.utils;

import net.minecraft.util.MathHelper;

public class Rotation {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
    }

    public Rotation copy() {
        return new Rotation(yaw, pitch);
    }

    public void applyToPlayer(net.minecraft.client.Minecraft mc) {
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
        }
    }

    @Override
    public String toString() {
        return String.format("Rotation(yaw=%.2f, pitch=%.2f)", yaw, pitch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Rotation)) return false;
        Rotation other = (Rotation) obj;
        return Math.abs(yaw - other.yaw) < 0.01f && Math.abs(pitch - other.pitch) < 0.01f;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(yaw) * 31 + Float.hashCode(pitch);
    }
}