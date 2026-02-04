package meldexun.lagspikeprofiler.asm;

import java.lang.reflect.Field;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.BiMap;

import meldexun.asmutil2.ASMUtil;
import meldexun.asmutil2.HashMapClassNodeClassTransformer;
import meldexun.asmutil2.IClassTransformerRegistry;
import meldexun.asmutil2.NonLoadingClassWriter;
import meldexun.asmutil2.reader.ClassUtil;
import meldexun.lagspikeprofiler.asm.util.DeobfuscationUtil;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.eventhandler.Event;

public class LagSpikeProfilerTransformer extends HashMapClassNodeClassTransformer implements IClassTransformer {

	private static final ClassUtil REMAPPING_CLASS_UTIL;
	static {
		try {
			Class<?> FMLDeobfuscatingRemapper = Class.forName("net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper", true, Launch.classLoader);
			Field _INSTANCE = FMLDeobfuscatingRemapper.getField("INSTANCE");
			Field _classNameBiMap = FMLDeobfuscatingRemapper.getDeclaredField("classNameBiMap");
			_classNameBiMap.setAccessible(true);
			@SuppressWarnings("unchecked")
			BiMap<String, String> deobfuscationMap = (BiMap<String, String>) _classNameBiMap.get(_INSTANCE.get(null));
			REMAPPING_CLASS_UTIL = ClassUtil.getInstance(new ClassUtil.Configuration(Launch.classLoader, deobfuscationMap.inverse(), deobfuscationMap));
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@Override
	protected void registerTransformers(IClassTransformerRegistry registry) {
		registry.add("net.minecraftforge.fml.common.eventhandler.IEventListener", ClassWriter.COMPUTE_FRAMES, classNode -> {
			MethodNode profilingName = new MethodNode(Opcodes.ACC_PUBLIC, "profilingName", "()Ljava/lang/String;", null, null);
			profilingName.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			profilingName.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false));
			profilingName.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false));
			profilingName.instructions.add(new InsnNode(Opcodes.ARETURN));
			classNode.methods.add(profilingName);
		});
		registry.add("net.minecraftforge.fml.common.eventhandler.ASMEventHandler", ClassWriter.COMPUTE_FRAMES, classNode -> {
			FieldNode f_profilingName = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "profilingName", "Ljava/lang/String;", null, null);
			classNode.fields.add(f_profilingName);

			MethodNode init = ASMUtil.find(classNode, "<init>", "(Ljava/lang/Object;Ljava/lang/reflect/Method;Lnet/minecraftforge/fml/common/ModContainer;Z)V");
			init.instructions.insertBefore(ASMUtil.first(init).opcode(Opcodes.RETURN).find(), ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, 0),
					new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"),
					new InsnNode(Opcodes.DUP),
					new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false),
					new VarInsnNode(Opcodes.ALOAD, 1),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false),
					new LdcInsnNode('.'),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false),
					new VarInsnNode(Opcodes.ALOAD, 2),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false),
					new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, f_profilingName.name, f_profilingName.desc)
			));

			MethodNode m_profilingName = new MethodNode(Opcodes.ACC_PUBLIC, "profilingName", "()Ljava/lang/String;", null, null);
			m_profilingName.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			m_profilingName.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, f_profilingName.name, f_profilingName.desc));
			m_profilingName.instructions.add(new InsnNode(Opcodes.ARETURN));
			classNode.methods.add(m_profilingName);

			MethodNode clinit = ASMUtil.find(classNode, "<clinit>");
			ASMUtil.replace(clinit, ASMUtil.first(clinit).methodInsn("getDeclaredMethods").find(), ASMUtil.first(clinit).opcode(Opcodes.AALOAD).find(), ASMUtil.listOf(
					new LdcInsnNode("invoke"),
					new InsnNode(Opcodes.ICONST_1),
					new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"),
					new InsnNode(Opcodes.DUP),
					new InsnNode(Opcodes.ICONST_0),
					new LdcInsnNode(Type.getType(Event.class)),
					new InsnNode(Opcodes.AASTORE),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false)
			));
		});
		registry.add("net.minecraftforge.fml.common.eventhandler.EventBus", "post", ClassWriter.COMPUTE_FRAMES, method -> {
			ASMUtil.addLocalVariable(method, "profiler", "Lnet/minecraft/profiler/Profiler;", new LabelNode(), ASMUtil.findLocalVariable(method, "listeners").end);
			LocalVariableNode profiler = ASMUtil.findLocalVariable(method, "profiler");

			method.instructions.insert(ASMUtil.findLocalVariable(method, "listeners").start, ASMUtil.listWithLabel(label -> ASMUtil.listOf(
					// if (listeners.length == 0) {
					new VarInsnNode(Opcodes.ALOAD, ASMUtil.findLocalVariable(method, "listeners").index),
					new InsnNode(Opcodes.ARRAYLENGTH),
					new JumpInsnNode(Opcodes.IFNE, label),
					//     return false;
					new InsnNode(Opcodes.ICONST_0),
					new InsnNode(Opcodes.IRETURN),
					// }
					label,
					// Profiler profiler = ProfilingManager.PROFILER.get();
					new FieldInsnNode(Opcodes.GETSTATIC, "meldexun/lagspikeprofiler/profiler/ProfilingManager", "PROFILER", "Ljava/lang/ThreadLocal;"),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false),
					new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/profiler/Profiler"),
					new VarInsnNode(Opcodes.ASTORE, profiler.index),
					profiler.start,
					// profiler.startSection(event.getClass().getSimpleName());
					new VarInsnNode(Opcodes.ALOAD, profiler.index),
					new VarInsnNode(Opcodes.ALOAD, 1),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false),
					new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getSimpleName", "()Ljava/lang/String;", false),
					DeobfuscationUtil.createObfMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/profiler/Profiler", "func_76320_a", "(Ljava/lang/String;)V", false) // startSection
			)));
			method.instructions.insert(ASMUtil.first(method).opcode(Opcodes.AALOAD).find(), ASMUtil.listOf(
					new InsnNode(Opcodes.DUP),
					new VarInsnNode(Opcodes.ALOAD, profiler.index),
					new InsnNode(Opcodes.SWAP),
					new MethodInsnNode(Opcodes.INVOKEINTERFACE, "net/minecraftforge/fml/common/eventhandler/IEventListener", "profilingName", "()Ljava/lang/String;", true),
					DeobfuscationUtil.createObfMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/profiler/Profiler", "func_76320_a", "(Ljava/lang/String;)V", false) // startSection
			));
			method.instructions.insert(ASMUtil.first(method).methodInsn("invoke").find(), ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, profiler.index),
					DeobfuscationUtil.createObfMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/profiler/Profiler", "func_76319_b", "()V", false) // endSection
			));
			method.instructions.insertBefore(ASMUtil.last(method).opcode(Opcodes.IRETURN).find(), ASMUtil.listOf(
					new VarInsnNode(Opcodes.ALOAD, profiler.index),
					DeobfuscationUtil.createObfMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/profiler/Profiler", "func_76319_b", "()V", false) // endSection
			));
		});
	}

	@Override
	protected ClassWriter createClassWriter(int flags) {
		return new NonLoadingClassWriter(flags, REMAPPING_CLASS_UTIL);
	}

}
