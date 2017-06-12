package crescendo.system;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.w3c.dom.Element;

public class Feature extends Configuration {
	private static final long serialVersionUID = 1L;
	public static final String ID = "feature-id";
	protected static final HashMap<String, Feature> features = new HashMap<String, Feature>();

	public static <T extends Feature> T get(SiteContext sctx, String id) {
		return sctx.profile().feature(id);
	}

	public static <T extends Feature> T load(String path) {
		if (isEmpty(path)) return null;
		Feature feature = features.get(path);
		if (feature == null) {
			Element doc = RefReader.xml.getDocument(path);
			Feature ancestor = load(RefReader.xml.attribute(doc, "extends"));
			boolean extended = ancestor != null;

			if (extended) {
				feature = instance(ancestor.getClass());
			} else {
				Class<? extends Feature> klass = classOf(RefReader.xml.attribute(doc, "class"));
				if (klass == null)
					klass = classOf(RefReader.xml.attribute(doc, "loader"));
				if (klass == null)
					klass = Feature.class;
				feature = instance(klass);
			}

			feature.ancestor = extended ? ancestor.ID() : null;
			feature.setID(path);
			feature.read(doc);

			features.put(path, feature);
		}
		return (T)feature;
	}

	public static final Feature unload(String path) {
		return features.remove(path);
	}

	private String ancestor;

	private void gatherKeys(Set<String> keys) {
		if (!isEmpty(ancestor))
			ancestor().gatherKeys(keys);
		keys.addAll(Feature.super.keySet());
	}
	@Override
	public Set<String> keySet() {
		if (isEmpty(ancestor))
			return Feature.super.keySet();

		LinkedHashSet<String> result = new LinkedHashSet<>();
		gatherKeys(result);
		return result;
	}

	public <T extends Feature> T ancestor() {
		return !isEmpty(ancestor) ? load(ancestor) : null;
	}

	protected Object inherited(Object key) {
		Feature a = ancestor();
		return !isEmpty(a) ? a.get(key) : null;
	}

	protected void read(Element doc) {
		ComplexReader.readSimpleEntries(this, doc);
		Consumer<? super Reader> setEntry = entry -> {
			Entry featureEntry = (Entry)entry.value();
			set(entry.id(), featureEntry);
			featureEntry.setFeature(this);
		};
		Reader.find(doc, "entity", Entity.Config.Reader::new).forEach(setEntry);
		Reader.find(doc, "permission", PermissionInspector.Config.Reader::new).forEach(setEntry);
		Reader.find(doc, "event", Event.Config.Reader::new).forEach(setEntry);
		Reader.find(doc, "feature", RefReader::new).forEach(entry -> set(entry.id(), entry.value()));
	}
	@Override
	public Object get(Object key) {
		Object obj = super.get(key);
		if (obj == null)
			obj = inherited(key);
		return obj;
	}

	public <T> Class<T> klass(String name) {
		return (Class<T>)get(name);
	}

	public <T extends Feature> T feature(String entryID) {
		return ((Ref)get(entryID)).get();
	}

	protected void visit(Predicate<Object> filter, BiConsumer<Feature, Object> consumer) {
		if (filter == null)
			filter = obj -> true;
		for (String key: keySet()) {
			Object obj = get(key);
			if (obj instanceof Feature)
				((Feature)obj).visit(filter, consumer);
			else if (obj instanceof Feature.Ref)
				((Feature.Ref)obj).get().visit(filter, consumer);
			else if (filter.test(obj))
				consumer.accept(this, obj);
		}
	}

	static class Ref {
		private String path;

		Ref(String path) {
			this.path = path;
		}

		public <T extends Feature> T get() {
			return load(path);
		}
	}

	private static class RefReader extends SimpleReader {
		@Override
		public RefReader setElement(Element element) {
			return RefReader.class.cast(super.setElement(element));
		}
		@Override
		public Ref value(String attr) {
			return new Ref(getValue(attr));
		}
		@Override
		public Ref value() {
			return new Ref(getValue());
		}
	}

	public static class Entry extends Configuration {
		private static final long serialVersionUID = 1L;

		public static <T extends Entry> T get(Feature feature, String entryID) {
			return (T)feature.get(entryID);
		}

		private Feature parent;

		public Entry setFeature(Feature parent) {
			this.parent = parent;
			init();
			return this;
		}

		public <T extends Feature> T feature() {
			return (T)parent;
		}

		protected void init() {}

		protected <T> T find(Feature feature, Function<Entry, T> func, Supplier<T> empty) {
			if (feature == null) return empty.get();
			Entry entry = Entry.get(feature, ID());
			if (entry == null) return empty.get();
			T t = func.apply(entry);
			return t != null ? t : find(feature.ancestor(), func, empty);
		}

		private Object inherited(Object key) {
			//TODO:How to call the method of a particular ancestor?
			return super.get(key);
		}
		@Override
		public Object get(Object key) {
			return find(parent, entry -> entry.inherited(key), () -> null);
		}
	}
}