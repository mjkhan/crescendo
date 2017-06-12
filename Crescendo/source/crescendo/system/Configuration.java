package crescendo.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import crescendo.system.sql.QuerySupport;
import horizon.system.Klass;
import horizon.system.Log;
import horizon.util.FileSupport;
import horizon.util.Text;
import horizon.util.Xmlement;

public abstract class Configuration extends NamedObjects {
	private static final long serialVersionUID = 1L;

	public static final <T> Class<T> classOf(String className) {
		return Klass.of(className);
	}

	public static final <T> Class<T> classAs(Class<?> klass) {
		return Klass.as(klass);
	}

	public static final <T> T instance(Class<T> klass) {
		return Klass.instance(klass);
	}

	public static final Iterable<Class<?>> getClasses(String... classNames) {
		if (isEmpty(classNames))
			return Collections.emptyList();
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for (String name: classNames) {
			Class<?> klass = classOf(name);
			if (klass != null && !classes.contains(klass))
				classes.add(klass);
		}
		return classes;
	}

	protected static void onChange(String path, Runnable handler) {
		FileSupport.get().onChange(FileSupport.absolutePath(path), handler);
	}

	private String id;

	public String ID() {
		return id;
	}

	public Configuration setID(String id) {
		this.id = id;
		return this;
	}

	protected <T> List<T> getObjects(String name) {
		ArrayList<T> objs = (ArrayList<T>)get(name);
		if (objs == null)
			set(name, objs = new ArrayList<T>());
		return objs;
	}

	protected Log log() {
		return Log.get(getClass());
	}
	@Override
	public String toString() {
		return getClass().getName() + "(\"" + ID() + "\")";
	}

	protected static abstract class Reader {
		public static final Xmlement xml = Xmlement.get();

		public static final String[] split(String s) {
			return Text.split(s, ",");
		}

		protected static <E extends Reader> Iterable<E> find(Element parent, String nodeName, Supplier<E> entrySupplier) {
			return xml.getChildren(parent, nodeName).stream()
				  .map(child -> {E e = entrySupplier.get(); e.setElement(child); return e;})
				  .collect(Collectors.toList());
		}

		protected Element element;

		public Reader setElement(Element element) {
			this.element = element;
			return this;
		}

		public String id() {
			return xml.attribute(element, "id");
		}

		public abstract Object value();
	}

	protected static abstract class SimpleReader extends Reader {
		protected static final boolean isSimple(Object obj) {
			return obj instanceof String
				|| obj instanceof Number
				|| obj instanceof Boolean
				|| obj instanceof Class;
		}
		@Override
		public SimpleReader setElement(Element element) {
			super.setElement(element);
			return this;
		}

		public abstract Object value(String attr);

		protected String getValue(String attr) {
			return xml.attribute(element, attr);
		}

		protected String getValue() {
			String result = getValue("value");
			return isEmpty(result) ? element.getTextContent() : result.trim();
		}
	}

	protected static class StringReader extends SimpleReader {
		@Override
		public StringReader setElement(Element element) {
			super.setElement(element);
			return this;
		}
		@Override
		public String value(String attr) {
			return getValue(attr);
		}
		@Override
		public String value() {
			return getValue();
		}
	}

	protected static class NumberReader extends SimpleReader {
		@Override
		public NumberReader setElement(Element element) {
			super.setElement(element);
			return this;
		}

		private Number number(String s) {
			try {
				return Double.valueOf(ifEmpty(s, () -> "0"));
			} catch (Exception e) {
				throw new RuntimeException("Configuration entry \"" + id() + "\" is not a number.");
			}
		}
		@Override
		public Number value(String attr) {
			return number(getValue(attr));
		}
		@Override
		public Number value() {
			return number(getValue());
		}
	}

	protected static class BooleanReader extends SimpleReader {
		@Override
		public BooleanReader setElement(Element element) {
			super.setElement(element);
			return this;
		}

		private Boolean bool(String s) {
			return Boolean.valueOf(s);
		}
		@Override
		public Boolean value(String attr) {
			return bool(getValue(attr));
		}
		@Override
		public Boolean value() {
			return bool(getValue());
		}
	}

	protected static class ClassReader extends SimpleReader {
		@Override
		public ClassReader setElement(Element element) {
			super.setElement(element);
			return this;
		}
		@Override
		protected String getValue() {
			String result = super.getValue();
			if (isEmpty(result))
				result = getValue("class");
			return result;
		}
		@Override
		public Class<?> value(String attr) {
			return classOf(getValue(attr));
		}
		@Override
		public Class<?> value() {
			return classOf(getValue());
		}

		public <T> Class<T> as() {
			return classAs(value());
		}

		public <T> Class<T> as(String attr) {
			return classAs(value(attr));
		}
	}

	protected static class SearchReader extends Reader {
		@Override
		public SearchReader setElement(Element element) {
			return SearchReader.class.cast(super.setElement(element));
		}
		@Override
		public String value() {
			String fieldNames = xml.attribute(element, "on");
			if (isEmpty(fieldNames))
				fieldNames = element.getTextContent().trim();
			return isEmpty(fieldNames) ? null : QuerySupport.get().terms().clause(split(fieldNames));
		}
	}

	protected static abstract class ComplexReader extends Reader {
		protected static void readSimpleEntries(Configuration cfg, Element element) {
			Consumer<Iterable<? extends Reader>> configure = entries -> entries.forEach(entry -> cfg.set(entry.id(), entry.value()));

			configure.accept(Reader.find(element, "boolean", BooleanReader::new));
			configure.accept(Reader.find(element, "number", NumberReader::new));
			configure.accept(Reader.find(element, "string", StringReader::new));
			configure.accept(Reader.find(element, "search", SearchReader::new));
			configure.accept(Reader.find(element, "class", ClassReader::new));
		}

		protected void readSimpleEntries(Configuration cfg) {
			readSimpleEntries(cfg, element);
		}
	}
}