package greencloud.impl.modules.render;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.block.BlockBed;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class BedPlates extends Module {

    private final NumberSetting blurStrength   = new NumberSetting("Blur Strength",   this, 0,    0,    40,   1   );
    private final NumberSetting bgAlpha        = new NumberSetting("BG Alpha",        this, 180,  0,    255,  5   );
    private final NumberSetting cornerRadius   = new NumberSetting("Corner Radius",   this, 8,    0,    16,   1   );
    private final NumberSetting iconScale      = new NumberSetting("Icon Scale",      this, 0.75, 0.3,  1.5,  0.05);
    private final BooleanSetting shadow        = new BooleanSetting("Shadow",         this, true );
    private final NumberSetting shadowStrength = new NumberSetting("Shadow Strength", this, 80,   0,    200,  5   );

    private volatile List<Tag> tags = Collections.emptyList();
    private final    List<Tag> work = new ArrayList<>();

    private int[]            qX, qZ;
    private int              qHead, qTail;

    private int scanCd  = SCAN_CD;
    private int fastCd  = 0;
    private int defTick = 0;
    private int defPtr  = 0;

    private static final int CHUNKS_PER_TICK = 6;
    private static final int SCAN_CD         = 40;
    private static final int FAST_CD         = 3;
    private static final int FAST_RAD        = 1;
    private static final int DEF_CD          = 4;
    private static final int MAX_DIST        = 128;
    private static final int MAX_SEC         = 16;

    private static final FloatBuffer S_BUF = BufferUtils.createFloatBuffer(3);
    private static final FloatBuffer M_BUF = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer P_BUF = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer   V_BUF = BufferUtils.createIntBuffer(16);

    private final BlockPos.MutableBlockPos defPos = new BlockPos.MutableBlockPos();

    public BedPlates() {
        super("BedPlates", "Shows bed defense as floating tags.", Category.RENDER);
        addSettings(blurStrength, bgAlpha, cornerRadius, iconScale, shadow, shadowStrength);
    }

    @Override public void onEnable()  { super.onEnable();  reset(); }
    @Override public void onDisable() { super.onDisable(); reset(); }

    private void reset() {
        tags = Collections.emptyList();
        work.clear();
        qX = qZ = null;
        qHead = qTail = 0;
        scanCd = SCAN_CD;
        fastCd = defTick = defPtr = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        if (++fastCd >= FAST_CD) {
            fastCd = 0;
            patchNearby();
        }

        if (++scanCd >= SCAN_CD && qHead == qTail) {
            scanCd = 0;
            buildQueue();
        }

        int budget = CHUNKS_PER_TICK;
        while (qHead < qTail && budget-- > 0) {
            Chunk chunk = mc.theWorld.getChunkFromChunkCoords(qX[qHead], qZ[qHead]);
            qHead++;
            if (chunk.isLoaded()) scanChunk(chunk);
        }

        if (qHead == qTail && qX != null) {
            Map<BlockPos, Tag> prev = buildLookup(tags);
            for (Tag t : work) {
                Tag old = prev.get(t.pos);
                if (old != null) {
                    t.dist   = old.dist;
                    t.screen = old.screen;
                    t.def    = old.def;
                } else {
                    t.def = scanDef(t.pos);
                }
            }
            tags   = work.isEmpty() ? Collections.emptyList() : new ArrayList<>(work);
            work.clear();
            defPtr = 0;
            qX = qZ = null;
        }

        List<Tag> cur = tags;
        if (!cur.isEmpty() && ++defTick >= DEF_CD) {
            defTick = 0;
            if (defPtr >= cur.size()) defPtr = 0;
            cur.get(defPtr++).def = scanDef(cur.get(defPtr - 1).pos);
        }

        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        for (Tag t : cur) {
            double dx = t.pos.getX() + 0.5 - px;
            double dy = t.pos.getY()       - py;
            double dz = t.pos.getZ() + 0.5 - pz;
            t.dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    private void buildQueue() {
        int rd  = mc.gameSettings.renderDistanceChunks;
        int dia = 2 * rd + 1;
        int sz  = dia * dia;
        qX = new int[sz]; qZ = new int[sz];
        qHead = qTail = 0;
        int cx0 = (int) mc.thePlayer.posX >> 4;
        int cz0 = (int) mc.thePlayer.posZ >> 4;
        for (int x = -rd; x <= rd; x++)
            for (int z = -rd; z <= rd; z++) {
                qX[qTail] = cx0 + x;
                qZ[qTail] = cz0 + z;
                qTail++;
            }
        work.clear();
    }

    private void scanChunk(Chunk chunk) {
        collectBeds(chunk, work);
    }

    private static void collectBeds(Chunk chunk, List<Tag> out) {
        ExtendedBlockStorage[] secs = chunk.getBlockStorageArray();
        int bx = chunk.xPosition << 4;
        int bz = chunk.zPosition << 4;
        for (int si = 0; si < MAX_SEC && si < secs.length; si++) {
            ExtendedBlockStorage sec = secs[si];
            if (sec == null) continue;
            int by = si << 4;
            for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) for (int ly = 0; ly < 16; ly++) {
                BlockPos bp = new BlockPos(bx + lx, by + ly, bz + lz);
                net.minecraft.block.state.IBlockState state = chunk.getBlockState(bp);
                if (state.getBlock() != Blocks.bed) continue;
                if ((Blocks.bed.getMetaFromState(state) & 8) == 0) continue;
                out.add(new Tag(bp));
            }
        }
    }

    private void patchNearby() {
        int cx0 = (int) mc.thePlayer.posX >> 4;
        int cz0 = (int) mc.thePlayer.posZ >> 4;

        List<Tag> found = new ArrayList<>();
        for (int x = -FAST_RAD; x <= FAST_RAD; x++) for (int z = -FAST_RAD; z <= FAST_RAD; z++) {
            Chunk chunk = mc.theWorld.getChunkFromChunkCoords(cx0 + x, cz0 + z);
            if (chunk.isLoaded()) collectBeds(chunk, found);
        }

        int minBX = (cx0 - FAST_RAD) << 4, maxBX = ((cx0 + FAST_RAD) << 4) + 15;
        int minBZ = (cz0 - FAST_RAD) << 4, maxBZ = ((cz0 + FAST_RAD) << 4) + 15;

        Set<BlockPos> foundSet = new HashSet<>(found.size() * 2);
        for (Tag t : found) foundSet.add(t.pos);

        Map<BlockPos, Tag> existing = buildLookup(tags);
        List<Tag> patched = new ArrayList<>(tags);
        patched.removeIf(t -> {
            int tx = t.pos.getX(), tz = t.pos.getZ();
            return tx >= minBX && tx <= maxBX && tz >= minBZ && tz <= maxBZ && !foundSet.contains(t.pos);
        });
        for (Tag t : found) {
            if (!existing.containsKey(t.pos)) {
                t.def = scanDef(t.pos);
                patched.add(t);
            }
        }
        tags = patched;
    }

    private List<ItemStack> scanDef(BlockPos bed) {
        Set<Item>       seen = new HashSet<>();
        List<ItemStack> out  = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 0; dy <= 2; dy++) {
            defPos.set(bed.getX() + dx, bed.getY() + dy, bed.getZ() + dz);
            net.minecraft.block.Block b = mc.theWorld.getBlockState(defPos).getBlock();
            if (b == Blocks.air || b instanceof BlockBed) continue;
            Item item = Item.getItemFromBlock(b);
            if (item != null && seen.add(item)) out.add(new ItemStack(item, 1));
        }
        return out;
    }

    private static Map<BlockPos, Tag> buildLookup(List<Tag> list) {
        Map<BlockPos, Tag> m = new HashMap<>(list.size() * 2);
        for (Tag t : list) m.put(t.pos, t);
        return m;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        for (Tag t : tags) {
            if (t.dist > MAX_DIST) { t.screen = null; continue; }
            double rx = t.pos.getX() + 0.5 - mc.getRenderManager().viewerPosX;
            double ry = t.pos.getY() + 1.2  - mc.getRenderManager().viewerPosY;
            double rz = t.pos.getZ() + 0.5  - mc.getRenderManager().viewerPosZ;
            Vector3f s = proj((float) rx, (float) ry, (float) rz, sf);
            t.screen = (s != null && s.z >= 0f && s.z < 1f) ? s : null;
        }
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        List<Tag> cur = tags;
        if (cur.isEmpty()) return;

        List<Tag> vis = new ArrayList<>(cur.size());
        for (Tag t : cur) if (t.screen != null) vis.add(t);
        if (vis.isEmpty()) return;

        vis.sort((a, b) -> Float.compare(b.dist, a.dist));
        for (Tag t : vis) drawTag(t);
    }

    private void drawTag(Tag tag) {
        FontUtil.SafeFont bold = FontUtil.getSafeBold();
        float sc    = (float) iconScale.getValue();
        float iSz   = 16f * sc;
        float padX  = 8f, padY = 6f, gap = 4f;
        float fH    = bold.getHeight();

        String lbl  = (int) tag.dist + "m";
        float  lblW = bold.getStringWidth(lbl);

        List<ItemStack> def  = tag.def;
        float defW  = def.isEmpty() ? 0f : def.size() * iSz + (def.size() - 1) * 2f;
        float boxW  = padX * 2 + lblW + (def.isEmpty() ? 0f : gap + defW);
        float boxH  = padY * 2 + Math.max(fH, iSz);
        float r     = Math.min((float) cornerRadius.getValue(), boxH / 2f);
        float sx    = tag.screen.x - boxW / 2f;
        float sy    = tag.screen.y - boxH / 2f;

        if (shadow.enabled)
            GreenRender.glowRR(sx, sy, boxW, boxH, r, 10f, new Color(0, 0, 0, (int) shadowStrength.getValue()));

        if (blurStrength.getValue() > 0)
            GreenRender.blurRounded(sx, sy, boxW, boxH, (float) blurStrength.getValue(), (int) r);

        GreenRender.fillRR(sx, sy, boxW, boxH, r, new Color(0, 0, 0, (int) bgAlpha.getValue()).getRGB());

        float cx   = sx + padX;
        float midY = sy + boxH / 2f;
        bold.drawString(lbl, (int) cx, (int)(midY - fH / 2f), 0xFFFFFFFF);
        cx += lblW + gap;

        if (!def.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            for (ItemStack stack : def) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(cx, midY - iSz / 2f, 0);
                GlStateManager.scale(sc, sc, 1f);
                mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
                GlStateManager.popMatrix();
                cx += iSz + 2f;
            }
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static Vector3f proj(float x, float y, float z, int sf) {
        M_BUF.rewind(); P_BUF.rewind(); V_BUF.rewind(); S_BUF.rewind();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,  M_BUF);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, P_BUF);
        GL11.glGetInteger(GL11.GL_VIEWPORT,        V_BUF);
        M_BUF.rewind(); P_BUF.rewind(); V_BUF.rewind(); S_BUF.rewind();
        if (!GLU.gluProject(x, y, z, M_BUF, P_BUF, V_BUF, S_BUF)) return null;
        return new Vector3f(S_BUF.get(0) / sf, (Display.getHeight() - S_BUF.get(1)) / sf, S_BUF.get(2));
    }

    private static final class Tag {
        final BlockPos       pos;
        volatile List<ItemStack> def    = Collections.emptyList();
        volatile float           dist   = 0f;
        volatile Vector3f        screen = null;

        Tag(BlockPos pos) { this.pos = pos; }
    }
}
