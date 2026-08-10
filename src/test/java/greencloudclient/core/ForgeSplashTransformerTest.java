package greencloudclient.core;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class ForgeSplashTransformerTest {

    @Test
    public void splashLifecycleUsesMainContextRenderer() throws IOException {
        ClassNode result = transformResource(
                "net/minecraftforge/fml/client/SplashProgress.class",
                "net.minecraftforge.fml.client.SplashProgress"
        );
        assertTrue(hasCall(result, "start", "greencloudclient/com/gui/loading/ForgeSplashLoadingScreen", "start"));
        assertTrue(hasCall(result, "finish", "greencloudclient/com/gui/loading/ForgeSplashLoadingScreen", "finish"));
        assertTrue(hasCall(result, "drawVanillaScreen", "greencloudclient/com/gui/loading/ForgeSplashLoadingScreen", "isActive"));
    }

    @Test
    public void progressStepsRenderLoadingFrames() throws IOException {
        ClassNode result = transformResource(
                "net/minecraftforge/fml/common/ProgressManager$ProgressBar.class",
                "net.minecraftforge.fml.common.ProgressManager$ProgressBar"
        );
        assertTrue(hasCall(result, "step", "greencloudclient/com/gui/loading/ForgeSplashLoadingScreen", "onProgress"));
    }

    @Test
    public void fullscreenPreferenceIsAppliedBeforeDisplayCreation() throws IOException {
        ClassNode result = transformResource(
                "net/minecraft/client/Minecraft.class",
                "net.minecraft.client.Minecraft"
        );
        assertTrue(hasCall(result, null, "greencloudclient/com/gui/loading/EarlyFullscreenBootstrap", "apply"));
    }

    private static ClassNode transformResource(String resource, String transformedName) throws IOException {
        InputStream input = ForgeSplashTransformerTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        } finally {
            input.close();
        }
        byte[] transformed = new ForgeSplashTransformer().transform(
                transformedName,
                transformedName,
                output.toByteArray()
        );
        ClassNode result = new ClassNode();
        new ClassReader(transformed).accept(result, 0);
        return result;
    }

    private static boolean hasCall(ClassNode classNode, String methodName, String owner, String name) {
        for (MethodNode method : classNode.methods) {
            if (methodName != null && !methodName.equals(method.name)) continue;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && name.equals(call.name)) return true;
            }
        }
        return false;
    }
}
