package meldexun.lagspikeprofiler.mixin;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.util.concurrent.ListenableFuture;

import meldexun.lagspikeprofiler.command.CommandProfile;
import meldexun.lagspikeprofiler.profiler.LagSpikeProfiler;
import meldexun.lagspikeprofiler.profiler.LagSpikeProfiler.Section;
import meldexun.lagspikeprofiler.profiler.ProfilerIO;
import meldexun.lagspikeprofiler.profiler.ProfilingManager;
import meldexun.lagspikeprofiler.profiler.ProfilingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements ProfilingManager {

	@Shadow
	@Mutable
	private Profiler profiler;
	@Shadow
	private WorldClient world;
	@Shadow
	public EntityPlayerSP player;

	@Unique
	private ProfilingState state = ProfilingState.IDLE;
	@Unique
	private long start;
	@Unique
	private long duration;

	@Inject(method = "init", at = @At("RETURN"))
	public void registerCommands(CallbackInfo info) {
		ClientCommandHandler.instance.registerCommand(new CommandProfile());
	}

	@Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V", shift = Shift.BEFORE))
	public void preUpdateDisplay(CallbackInfo info) {
		profiler.startSection("updateDisplay");
	}

	@Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V", shift = Shift.AFTER))
	public void postUpdateDisplay(CallbackInfo info) {
		profiler.endSection(); // end "updateDisplay"
		profiler.endSection(); // end "root"

		// end previous frame
		if (state == ProfilingState.PROFILING || state == ProfilingState.STOPPING) {
			((LagSpikeProfiler) profiler).endFrame();

			if (state == ProfilingState.STOPPING || System.nanoTime() >= start + duration) {
				// save profiler result to disk
				List<Section> profilerResult = ((LagSpikeProfiler) profiler).frames();
				CompletableFuture.runAsync(() -> {
					Path file = Paths.get("profiler-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".lagspikeprofile");
					try (OutputStream out = new BufferedOutputStream(new BZip2CompressorOutputStream(new BufferedOutputStream(Files.newOutputStream(file))))) {
						ProfilerIO.write(out, profilerResult);

						addScheduledTask(() -> {
							if (player != null) {
								player.sendMessage(new TextComponentString("Profiler result saved to " + file));
							}
						});
					} catch (Throwable e) {
						crashed(new CrashReport("Saving profiler result", e));
					}
				});

				// stop profiling
				ProfilingManager.PROFILER.set(ProfilingManager.NON_PROFILING);
				profiler = new Profiler();
				if (world != null) {
					((WorldProfilerAccessor) world).profiler(profiler);
				}
				state = ProfilingState.IDLE;
				if (player != null) {
					player.sendMessage(new TextComponentString("Profiling finished."));
				}
			}
		}

		// start next frame
		if (state == ProfilingState.STARTING || state == ProfilingState.PROFILING) {
			if (state == ProfilingState.STARTING) {
				// start profiling
				LagSpikeProfiler lagSpikeProfiler = new LagSpikeProfiler();
				ProfilingManager.PROFILER.set(lagSpikeProfiler);
				profiler = lagSpikeProfiler;
				if (world != null) {
					((WorldProfilerAccessor) world).profiler(lagSpikeProfiler);
				}
				state = ProfilingState.PROFILING;
				start = System.nanoTime();
				if (player != null) {
					player.sendMessage(new TextComponentString("Profiling started."));
				}
			}

			((LagSpikeProfiler) profiler).startFrame();
		} else if (state != ProfilingState.IDLE) {
			throw new IllegalStateException("Illegal state at frame start: " + state);
		}

		profiler.startSection("root");
	}

	@Shadow
	public abstract ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule);

	@Shadow
	public abstract void crashed(CrashReport crash);

	@Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 0))
	public void skip1(Profiler profiler, String name) {

	}

	@Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 4))
	public void skip2(Profiler profiler) {

	}

	@Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 5))
	public void skip3(Profiler profiler, String name) {

	}

	@Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", ordinal = 6))
	public void skip4(Profiler profiler) {

	}

	@Override
	public void start(long millis) throws IllegalStateException {
		if (state == ProfilingState.IDLE) {
			state = ProfilingState.STARTING;
			duration = TimeUnit.MILLISECONDS.toNanos(millis);
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public void stop() throws IllegalStateException {
		if (state == ProfilingState.PROFILING) {
			state = ProfilingState.STOPPING;
		} else {
			throw new IllegalStateException();
		}
	}

}
