package crescendo.system;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;

import horizon.jms.TopicAccess;
import horizon.system.Log;

public abstract class Event extends Effect.Implementation {
	private static final long serialVersionUID = 1L;

	public static final String CREATE = Entity.CREATE;
	public static final String UPDATE = Entity.UPDATE;
	public static final String REMOVE = Entity.REMOVE;

	protected static final <T extends Event> T create(Feature feature, Config cfg) {
		T evt = Feature.instance(cfg.event());
		EventListener.add(cfg, evt.setPath(feature.ID())).set(Request.get(false));
		return evt;
	}

	public static final <T extends Event> T create(Feature feature, String entryID) {
		return create(feature, Config.get(feature, entryID));
	}

	public static final <T extends Event> Collection<T> find(Collection<? extends Event> evts, Class<T> klass, Predicate<T> filter) {
		if (isEmpty(evts))
			return Collections.emptySet();
		LinkedHashSet<T> result = evts.stream().filter(evt -> klass.isInstance(evt)).map(evt -> klass.cast(evt)).collect(Collectors.toCollection(LinkedHashSet::new));
		if (filter != null)
			result.removeIf(e -> !filter.test(e));
		return result;
	}

	public static final <T extends Event> void fire(Feature feature, Class<T> klass, Consumer<T> consumer) {
		feature.visit(
			obj -> obj instanceof Config && ((Config)obj).isOf(klass),
			(f, obj) -> {
				T evt = Event.create(f, (Config)obj);
				consumer.accept(evt);
			}
		);
	}

	private String path;
	private boolean fired;
	protected boolean update;
	private String type;
	private Event parent;
	private LinkedHashSet<Event> children;
	private transient LinkedHashSet<EventListener> listeners;
	private transient LinkedHashSet<Job> jobs;//Transient because the jobs are sent separately from the event

	protected NamedObjects loads;
	protected LinkedHashSet<String> unloads;

	public String type() {
		return type;
	}
	public Event setType(String type) {
		this.type = type;
		return this;
	}

	public <T extends Feature> T feature() {
		return Feature.load(path);
	}

	Event setPath(String path) {
		this.path = path;
		return this;
	}
	@Override
	public Event set(String name, Object value) {
		super.set(name, value);
		return this;
	}
	@Override
	public Event setAll(Map<String, ?> objs) {
		super.setAll(objs);
		return this;
	}

	public Event parent() {
		return parent;
	}

	public Set<Event> children() {
		return children != null ? children : Collections.emptySet();
	}

	public Event add(Event child) {
		if (child != null
		 && !this.equals(child)
		 && child.fired) {
			if (children == null)
				children = new LinkedHashSet<Event>();
			if (child.parent != null)
				child.parent.remove(child);
			child.parent = this;
			children.add(child);
		}
		return this;
	}

	public Event remove(Event child) {
		if (children != null && children.contains(child)) {
			if (this == child.parent)
				child.parent = null;
			children.remove(child);
		}
		return this;
	}

	private LinkedHashSet<Event> expand(LinkedHashSet<Event> result) {
		if (result == null)
			result = new LinkedHashSet<>();
		result.add(this);
		for (Event child: children())
			child.expand(result);
		return result;
	}

	public Set<Event> expand() {
		return expand(null);
	}

	public Event add(Job job) {
		if (job != null) {
			if (jobs == null)
				jobs = new LinkedHashSet<Job>();
			jobs.add(job);
		}
		return this;
	}

	public Collection<Job> removeJobs() {
		LinkedHashSet<Job> result = isEmpty(jobs) ? null : new LinkedHashSet<Job>(jobs);
		jobs = null;
		for (Event child: children()) {
			Collection<Job> subJobs = child.removeJobs();
			if (!subJobs.iterator().hasNext()) continue;

			if (result == null)
				result = new LinkedHashSet<Job>();
			for (Job subJob: subJobs)
				result.add(subJob);
		}
		return result != null ? result : Collections.emptyList();
	}

	public Event remove(Job job) {
		if (job != null && jobs != null && jobs.contains(job))
			jobs.remove(job);
		return this;
	}

	public boolean hasUpdates() {
		if (!update)
			update = !isEmpty(loads) || !isEmpty(unloads);
		if (!update)
			for (Event child: children()) {
				update = child.hasUpdates();
				if (update) break;
			}
		return update;
	}

	public Event setHasUpdates(boolean update) {
		this.update = update;
		return this;
	}

	public NamedObjects loads() {
		return loads != null ? loads : (loads = new NamedObjects());
	}

	public Event setLoad(String name, Serializable obj) {
		if (obj != null)
			loads().set(name, obj);
		return this;
	}

	public Collection<String> unloads() {
		return unloads != null ? unloads : Collections.emptyList();
	}

	public Event setUnload(String name) {
		if (!isEmpty(name)) {
			if (unloads == null)
				unloads = new LinkedHashSet<String>();
			unloads.add(name);
		}
		return this;
	}
	@Override
	public Object get(Object key) {
		Object obj = super.get(key);
		if (obj == null) {
			for (Event child: children()) {
				obj = child.get(key);
				if (obj != null)
					break;
			}
		}
		return obj;
	}

	public boolean isFired() {
		return fired;
	}

	public Event setFired(boolean fired) {
		this.fired = fired;
		return this;
	}

	public boolean hasListeners() {
		return !isEmpty(listeners);
	}

	public void add(EventListener listener) {
		if (listener == null || !listener.isListening()) return;
		if (listeners == null)
			listeners = new LinkedHashSet<>();
		listeners.add(listener);
	}

	public boolean before() {
		if (listeners != null) {
			log().debug(() -> "Before " + this);
			for (EventListener listener: listeners)
				if (!listener.before(this)) return false;
		}
		return true;
	}

	private Log log() {
		return Log.get(getClass());
	}

	public Event on() {
		if (isFired() && hasListeners()) {
			log().debug(() -> "On " + this);
			listeners.forEach(listener -> {listener.on(this);});
		}
		return this;
	}

	public void after() {
		log().debug(() -> "after " + this);
	}
	@Override
	protected Event removeNotSerializable() {
		super.removeNotSerializable();
		children().forEach(child -> {child.removeNotSerializable();});
		return this;
	}
	@Override
	public String toString() {
		return getClass().getName() + "[\"" + type + "\"]";
	}

	private static final String EVENT = "event";

	public static final <T extends Event> T get(Map<String, Object> map) {
		return (T)map.get(EVENT);
	}

	public Event setTo(Map<String, Object> map) {
		map.put(EVENT, this);
		return this;
	}

	public static final void send(Collection<? extends Event> evts) {
		if (isEmpty(evts)) return;

		Support.splitByConfig(evts).forEach(Event::send);
	}

	private static final void send(String topicConfig, Collection<? extends Event> evts) {
		TopicAccess.send(topicConfig, eventsWithUpdates(evts));
	}

	private static Collection<Event> eventsWithUpdates(Collection<? extends Event> evts) {
		return evts.stream().filter(evt -> evt != null && evt.isFired() && evt.hasUpdates()).distinct().collect(Collectors.toCollection(LinkedHashSet::new));
	}

	protected static class Config extends Feature.Entry {
		private static final long serialVersionUID = 1L;
		private Class<? extends Event> klass;
		private Collection<Class<?>> listenerClasses;
		private LinkedHashSet<? extends EventListener> listeners;

		public <T extends Event> Class<T> event() {
			return find(feature(), entry -> {
				Config cfg = (Config)entry;
				return cfg.klass != null ? classAs(cfg.klass) : null;
			}, () -> null);
		}

		public Collection<EventListener> listeners() {
			return find(feature(), entry -> {
				Config cfg = (Config)entry;
				return isEmpty(cfg.listeners) ? null :
					   cfg.listeners.stream().map(listener -> listener.clone()).collect(Collectors.toCollection(LinkedHashSet::new));
			}, Collections::emptySet);
		}

		public boolean isOf(Class<?> klass) {
			Class<?> evtClass = event();
			if (evtClass == null || klass == null) return false;
			return klass.isAssignableFrom(evtClass);
		}
		@Override
		protected void init() {
			if (listenerClasses.isEmpty()) return;

			listeners = listenerClasses.stream().map(klass -> EventListener.build(event(), klass)).filter(listener -> listener != null)
					   .collect(Collectors.toCollection(LinkedHashSet::new));
		}
		@Override
		public String toString() {
			return String.format("%s(%s)", getClass().getName(), ID());
		}

		protected static class Reader extends ComplexReader {
			@Override
			public Reader setElement(Element element) {
				return Reader.class.cast(super.setElement(element));
			}
			@Override
			public Config value() {
				Config cfg = new Config();
				cfg.setID(id());
				Class<? extends Event> cls = new Configuration.ClassReader().setElement(element).as("class");
				if (cls != null && !Event.class.isAssignableFrom(cls))
					throw new RuntimeException(cls.getName() + " is not a " + Event.class.getName() + ".");
				cfg.klass = cls;
				cfg.listenerClasses = listenerClasses();
				return cfg;
			}

			private Collection<Class<?>> listenerClasses() {
				String[] classNames = split(ifEmpty(element.getTextContent(), () -> "").trim());
				return isEmpty(classNames) ? Collections.emptySet() :
					   Stream.of(classNames).map(className -> classOf(className.trim())).distinct().collect(Collectors.toCollection(LinkedHashSet::new));
			}
		}
	}
}