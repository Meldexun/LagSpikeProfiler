package meldexun.lagspikeprofiler.profiler;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.profiler.Profiler;

public class LagSpikeProfiler extends Profiler {

	public static class Section {

		private final Section parent;
		private final String name;
		private long time;
		private List<Section> subsections;

		public Section(String name) {
			this(null, name);
		}

		public Section(Section parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		Section addSubsection(String name) {
			if (subsections == null)
				subsections = new ObjectArrayList<>(8);
			Section subsection = new Section(this, name);
			subsections.add(subsection);
			return subsection;
		}

		public String name() {
			return name;
		}

		public long time() {
			return time;
		}

		public List<Section> subsections() {
			return subsections != null ? subsections : Collections.emptyList();
		}

	}

	private final List<Section> frames = new ObjectArrayList<>();
	private Section current;

	public void startFrame() {
		if (current != null) {
			throw new IllegalStateException();
		}
		frames.add(current = new Section("frame"));
		current.time = System.nanoTime();
	}

	public void endFrame() {
		if (current.parent != null) {
			throw new IllegalStateException();
		}
		long time = System.nanoTime();
		current.time = time - current.time;
		current = null;
	}

	@Override
	public void startSection(String name) {
		(current = current.addSubsection(name)).time = System.nanoTime();
	}

	@Override
	public void func_194340_a(Supplier<String> p_194340_1_) {
		startSection(p_194340_1_.get());
	}

	@Override
	public void endSection() {
		long time = System.nanoTime();
		current.time = time - current.time;
		current = current.parent;
	}

	public List<Section> frames() {
		return frames;
	}

}
