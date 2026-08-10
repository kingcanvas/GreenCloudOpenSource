package greencloudclient.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class ForgeSplashTransformer implements IClassTransformer {

    private static final String SPLASH_TARGET = "net.minecraftforge.fml.client.SplashProgress";
    private static final String PROGRESS_TARGET = "net.minecraftforge.fml.common.ProgressManager$ProgressBar";
    private static final String MINECRAFT_TARGET = "net.minecraft.client.Minecraft";
    private static final String RENDERER = "greencloudclient/com/gui/loading/ForgeSplashLoadingScreen";
    private static final String FULLSCREEN_BOOTSTRAP = "greencloudclient/com/gui/loading/EarlyFullscreenBootstrap";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (matches(MINECRAFT_TARGET, name, transformedName)) return installMinecraftHook(basicClass);
        if (matches(SPLASH_TARGET, name, transformedName)) return installSplashLifecycleHooks(basicClass);
        if (matches(PROGRESS_TARGET, name, transformedName)) return installProgressHook(basicClass);
        return basicClass;
    }

    private byte[] installMinecraftHook(byte[] basicClass) {
        try {
            ClassNode classNode = read(basicClass);
            FieldInsnNode settingsAssignment = findGameSettingsAssignment(classNode);
            if (settingsAssignment == null) throw new IllegalStateException("Minecraft GameSettings initialization marker was not found");
            MethodNode owner = ownerOf(classNode, settingsAssignment);
            if (owner == null) throw new IllegalStateException("Minecraft GameSettings owner method was not found");
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FULLSCREEN_BOOTSTRAP, "apply", "(Ljava/lang/Object;)V", false));
            owner.instructions.insert(settingsAssignment, hook);
            System.out.println("[GreenCloud] Installed early fullscreen initialization hook");
            return write(classNode);
        } catch (Throwable throwable) {
            System.err.println("[GreenCloud] Keeping Minecraft fullscreen timing: " + throwable);
            return basicClass;
        }
    }

    private byte[] installSplashLifecycleHooks(byte[] basicClass) {
        try {
            ClassNode classNode = read(basicClass);
            MethodNode start = findMethod(classNode, "start", "()V");
            MethodNode finish = findMethod(classNode, "finish", "()V");
            MethodNode vanillaScreen = findMethodByName(classNode, "drawVanillaScreen");
            if (start == null || finish == null || vanillaScreen == null) {
                throw new IllegalStateException("Forge splash lifecycle methods were not found");
            }

            LabelNode forgeFallback = new LabelNode();
            InsnList startHook = new InsnList();
            startHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RENDERER, "start", "()Z", false));
            startHook.add(new JumpInsnNode(Opcodes.IFEQ, forgeFallback));
            startHook.add(new InsnNode(Opcodes.RETURN));
            startHook.add(forgeFallback);
            start.instructions.insert(startHook);

            InsnList finishHook = new InsnList();
            finishHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RENDERER, "finish", "()V", false));
            finish.instructions.insert(finishHook);

            LabelNode drawVanilla = new LabelNode();
            InsnList vanillaHook = new InsnList();
            vanillaHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RENDERER, "isActive", "()Z", false));
            vanillaHook.add(new JumpInsnNode(Opcodes.IFEQ, drawVanilla));
            vanillaHook.add(new InsnNode(Opcodes.RETURN));
            vanillaHook.add(drawVanilla);
            vanillaScreen.instructions.insert(vanillaHook);

            System.out.println("[GreenCloud] Installed main-context splash lifecycle hooks");
            return write(classNode);
        } catch (Throwable throwable) {
            System.err.println("[GreenCloud] Keeping Forge splash lifecycle: " + throwable);
            return basicClass;
        }
    }

    private byte[] installProgressHook(byte[] basicClass) {
        try {
            ClassNode classNode = read(basicClass);
            MethodNode step = findMethod(classNode, "step", "(Ljava/lang/String;)V");
            if (step == null) throw new IllegalStateException("Forge progress step method was not found");

            int hooks = 0;
            for (AbstractInsnNode instruction = step.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction.getOpcode() != Opcodes.RETURN) continue;
                InsnList hook = new InsnList();
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RENDERER, "onProgress", "()V", false));
                step.instructions.insertBefore(instruction, hook);
                hooks++;
            }
            if (hooks == 0) throw new IllegalStateException("Forge progress return marker was not found");

            System.out.println("[GreenCloud] Installed main-context splash progress hook");
            return write(classNode);
        } catch (Throwable throwable) {
            System.err.println("[GreenCloud] Keeping Forge progress rendering: " + throwable);
            return basicClass;
        }
    }

    private static ClassNode read(byte[] bytecode) {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytecode).accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    private static byte[] write(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode findMethod(ClassNode classNode, String name, String descriptor) {
        for (MethodNode method : classNode.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }

    private static MethodNode findMethodByName(ClassNode classNode, String name) {
        for (MethodNode method : classNode.methods) {
            if (name.equals(method.name)) return method;
        }
        return null;
    }

    private static FieldInsnNode findGameSettingsAssignment(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != Opcodes.PUTFIELD) continue;
                FieldInsnNode field = (FieldInsnNode) instruction;
                if ("gameSettings".equals(field.name) || "field_71474_y".equals(field.name)
                        || "t".equals(field.name) || "Lnet/minecraft/client/settings/GameSettings;".equals(field.desc)) return field;
            }
        }
        return null;
    }

    private static MethodNode ownerOf(ClassNode classNode, AbstractInsnNode target) {
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction == target) return method;
            }
        }
        return null;
    }

    private static boolean matches(String target, String name, String transformedName) {
        return target.equals(name) || target.equals(transformedName);
    }
}
