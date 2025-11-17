package meldexun.lagspikeprofiler.profiler;

import java.util.function.Supplier;

public interface ProfilingManager {

	LagSpikeProfiler NON_PROFILING = new LagSpikeProfiler() {
		@Override
		public void startFrame() {

		}

		@Override
		public void endFrame() {

		}

		@Override
		public void startSection(String name) {

		}

		@Override
		public void func_194340_a(Supplier<String> p_194340_1_) {

		}

		@Override
		public void endSection() {

		}
	};
	ThreadLocal<LagSpikeProfiler> PROFILER = ThreadLocal.withInitial(() -> NON_PROFILING);

	default boolean tryStart(long millis) {
		try {
			start(millis);
			return true;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	void start(long millis) throws IllegalStateException;

	void stop() throws IllegalStateException;

}
