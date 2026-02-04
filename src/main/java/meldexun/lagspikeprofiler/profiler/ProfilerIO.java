package meldexun.lagspikeprofiler.profiler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meldexun.lagspikeprofiler.profiler.LagSpikeProfiler.Section;

public class ProfilerIO {

	public static final String FILE_FORMAT_VERSION = "1.1.0";

	public static void write(OutputStream dst, List<Section> profilerResult) throws IOException {
		try (DataOutputStream out = new DataOutputStream(dst)) {
			out.writeUTF(FILE_FORMAT_VERSION);

			Object2IntSortedMap<String> names = profilerResult.stream().flatMap(Section::stream).map(Section::name).collect(toIdentityMapping());

			out.writeInt(names.size());
			for (String name : names.keySet()) {
				out.writeUTF(name);
			}

			out.writeInt(profilerResult.size());
			Stack<Section> stack = new ObjectArrayList<>(Lists.reverse(profilerResult));
			while (!stack.isEmpty()) {
				Section section = stack.pop();
				writeVarInt(out, names.getInt(section.name()));
				writeVarLong(out, section.time());
				writeVarInt(out, section.subsections().size());
				Lists.reverse(section.subsections()).forEach(stack::push);
			}
		}
	}

	public static List<Section> read(InputStream src) throws IOException {
		try (DataInputStream in = new DataInputStream(src)) {
			in.readUTF(); // version

			String[] names = new String[in.readInt()];
			for (int i = 0; i < names.length; i++) {
				names[i] = in.readUTF();
			}

			int n = in.readInt();
			List<Section> profilerResult = new ObjectArrayList<>(n);
			for (int i = 0; i < n; i++) {
				profilerResult.add(new Section(in, names, null));
			}
			return profilerResult;
		}
	}

	private static <T> Collector<T, ?, Object2IntSortedMap<T>> toIdentityMapping() {
		return new Collector<T, Object2IntMap<T>, Object2IntSortedMap<T>>() {
			@Override
			public Supplier<Object2IntMap<T>> supplier() {
				return Object2IntOpenHashMap::new;
			}

			@Override
			public BiConsumer<Object2IntMap<T>, T> accumulator() {
				return (m, k) -> m.put(k, m.getInt(k) + 1);
			}

			@Override
			public BinaryOperator<Object2IntMap<T>> combiner() {
				return (m1, m2) -> {
					m2.object2IntEntrySet().forEach(e -> m1.put(e.getKey(), m1.get(e.getKey()) + e.getIntValue()));
					return m1;
				};
			}

			@Override
			public Function<Object2IntMap<T>, Object2IntSortedMap<T>> finisher() {
				return a -> {
					List<Object2IntMap.Entry<T>> l = new ObjectArrayList<>(a.object2IntEntrySet());
					l.sort(Comparator.comparingInt(Object2IntMap.Entry::getIntValue));
					Object2IntSortedMap<T> r = new Object2IntLinkedOpenHashMap<>();
					for (int i = 0; i < l.size(); i++) {
						r.put(l.get(i).getKey(), i);
					}
					return r;
				};
			}

			@Override
			public Set<Characteristics> characteristics() {
				return EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED);
			}
		};
	}

	public static void writeVarInt(OutputStream out, int x) throws IOException {
		while ((x & (~0 << 7)) != 0) {
			out.write(x | 1 << 7);
			x >>>= 7;
		}
		out.write(x);
	}

	public static void writeVarLong(OutputStream out, long x) throws IOException {
		while ((x & (~0 << 7)) != 0) {
			out.write((int) x | 1 << 7);
			x >>>= 7;
		}
		out.write((int) x);
	}

	public static int readVarInt(InputStream in) throws IOException {
		int x = 0;
		for (int i = 0; i < 5; i++) {
			int b = in.read();
			if (b < 0)
				throw new EOFException();
			x |= (b & ~(1 << 7)) << 7 * i;
			if ((b & (1 << 7)) == 0)
				break;
		}
		return x;
	}

	public static long readVarLong(InputStream in) throws IOException {
		long x = 0;
		for (int i = 0; i < 10; i++) {
			int b = in.read();
			if (b < 0)
				throw new EOFException();
			x |= (long) (b & ~(1 << 7)) << 7 * i;
			if ((b & (1 << 7)) == 0)
				break;
		}
		return x;
	}

}
