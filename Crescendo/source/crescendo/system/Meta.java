package crescendo.system;

import horizon.data.Convert;
import horizon.system.AbstractObject;
import horizon.system.Klass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Meta {
	String[] value();

	public static class Info extends AbstractObject {
		private static final String NULL = "__null__";
		private static final HashMap<Class<?>, Info> cache = new HashMap<Class<?>, Meta.Info>();

		private static final Info create(Class<?> klass) {
			if (klass == null) return null;
			Meta meta = klass.getAnnotation(Meta.class);
			Info info = new Info();
			info.klass = klass;
			if (meta != null && !isEmpty(meta.value()))
				for (String s: meta.value())
					info.add(Entry.parse(s));
			return info;
		}

		public static final Info get(Class<?> klass) {
			return cache.computeIfAbsent(klass, cls -> create(cls));
		}

		private Class<?> klass;
		private HashMap<String, String> entries;

		public void add(Entry entry) {
			if (entry == null) return;
			if (entries == null)
				entries = new HashMap<String, String>();
			String key = entry.key(),
				   value = entry.value();
			entries.put(key, value != null ? value : NULL);
		}

		private static Class<?> ancestor(Class<?> klass) {
			if (klass == null || Object.class.equals(klass)) return null;
			Class<?> base = klass.getSuperclass();
			Meta meta = base.getAnnotation(Meta.class);
			if (meta != null) return base;
			return ancestor(base);
		}

		private static String get(Class<?> klass, String key) {
			if (klass == null || Object.class.equals(klass) || isEmpty(key)) return null;

			Class<?> base = ancestor(klass);
			if (base == null) return null;

			Info info = get(base);
			String value = info.get(key);
			return value != null ? value : get(base, key);
		}

		public String get(String key) {
			String value = !isEmpty(entries) ? entries.get(key) : null;
			if (value != null)
				return !NULL.equals(value) ? value : null;
			return get(klass, key);
		}

		public Number number(String key) {
			return Convert.asNumber(get(key));
		}

		public boolean bool(String key) {
			String str = get(key);
			return !isEmpty(str) ? Boolean.valueOf(str).booleanValue() : false;
		}

		public <T> Class<T> klass(String key) {
			return Klass.of(get(key));
		}

		private static boolean contains(Class<?> klass, String key) {
			if (klass == null || Object.class.equals(klass) || isEmpty(key)) return false;

			Class<?> base = ancestor(klass);
			if (base == null) return false;

			Info info = get(base);
			boolean contained = info.contains(key);
			return contained ? contained : contains(base, key);
		}

		public boolean contains(String key) {
			boolean contained = !isEmpty(entries) && entries.containsKey(key);
			return contained ? true : contains(klass, key);
		}

		private static class Entry {
			public static Entry parse(String s) {
				if (isEmpty(s)) return null;

				int index = s.indexOf("=");
				String key = (index < 0 ? s : s.substring(0, index)).trim(),
					   value = index < 0 ? null : s.substring(index + 1);
				return new Entry().setKey(key).setValue(value);
			}

			private String
				key,
				value;

			public String key() {
				return key;
			}

			public Entry setKey(String key) {
				this.key = notEmpty(key, "key");
				return this;
			}

			public String value() {
				return value;
			}

			public Entry setValue(String value) {
				this.value = value;
				return this;
			}
		}
	}
}