package greencloud.impl.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CapeUtils {

    private static final Map<String, List<ResourceLocation>> animatedCapes = new HashMap<>();
    public static ResourceLocation getAnimatedCape(String name, int tick, int speed) {
        List<ResourceLocation> frames = animatedCapes.get(name);


        if (frames == null) {
            frames = loadGif(name);
            animatedCapes.put(name, frames);
        }

        if (frames.isEmpty()) return null;


        int index = (tick / speed) % frames.size();
        return frames.get(index);
    }

    private static List<ResourceLocation> loadGif(String name) {
        List<ResourceLocation> frames = new ArrayList<>();
        try {
            String path = "greencloud";
            ResourceLocation resLoc = new ResourceLocation(path, "textures/cape/" + name + ".gif");
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(resLoc);
            } catch (IOException e) {
                System.out.println("[GreenCloud] Cape not found: " + name);
                return frames;
            }

            InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(resLoc).getInputStream();
            ImageInputStream iis = ImageIO.createImageInputStream(stream);
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(iis, false);

            int numImages = 0;
            try {
                numImages = reader.getNumImages(true);
            } catch (Exception e) {
                System.err.println("[GreenCloud] Could not count frames for: " + name);
            }

            for (int i = 0; i < numImages; i++) {
                try {
                    BufferedImage frame = reader.read(i);

                    DynamicTexture texture = new DynamicTexture(frame);
                    String dynamicPath = "greencloud/capes/" + name + "_" + i;
                    ResourceLocation location = new ResourceLocation(dynamicPath);
                    Minecraft.getMinecraft().getTextureManager().loadTexture(location, texture);
                    frames.add(location);
                } catch (IndexOutOfBoundsException | IOException e) {
                    System.err.println("[GreenCloud] Skipped corrupt frame " + i + " in " + name);
                }
            }
        } catch (Exception e) {
            System.err.println("[GreenCloud] Failed to load animated cape: " + name);
            e.printStackTrace();
        }
        return frames;
    }
}