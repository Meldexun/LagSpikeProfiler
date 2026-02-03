package meldexun.lagspikeprofiler.profiler;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

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

		public Section(DataInputStream in, String[] names, @Nullable Section parent) throws IOException {
			this.parent = parent;
			this.name = names[ProfilerIO.readVarInt(in)];
			this.time = ProfilerIO.readVarLong(in);
			int n = ProfilerIO.readVarInt(in);
			this.subsections = new ObjectArrayList<>(n);
			for (int i = 0; i < n; i++) {
				this.subsections.add(new Section(in, names, this));
			}
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

		public Stream<Section> stream() {
			if (subsections == null) {
				return Stream.of(this);
			}
			return Stream.concat(Stream.of(this), subsections.stream().flatMap(Section::stream));
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
