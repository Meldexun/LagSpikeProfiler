package meldexun.lagspikeprofiler.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;

@Mixin(World.class)
public interface WorldProfilerAccessor {

	@Accessor("profiler")
	void profiler(Profiler profiler);

}
