package meldexun.lagspikeprofiler.asm.tweaker;

import java.io.File;
import java.util.List;

import meldexun.lagspikeprofiler.asm.LagSpikeProfilerTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class LagSpikeProfilerTweaker implements ITweaker {

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {

	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		classLoader.registerTransformer(LagSpikeProfilerTransformer.class.getName());
	}

	@Override
	public String getLaunchTarget() {
		throw new RuntimeException("Invalid for use as a primary tweaker");
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

}
