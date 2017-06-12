package crescendo.system;

import horizon.system.AbstractObject;
import horizon.system.AnnotatedMethods;
import horizon.system.Klass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EventListener extends AbstractObject implements Cloneable {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Repeatable(Befores.class)
	public static @interface Before {
		Class<? extends Event> event();
		String[] types();
	}
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Befores {
		Before[] value();
	}
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Repeatable(Ons.class)
	public static @interface On {
		Class<? extends Event> event();
		String[] types();
	}
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Ons {
		On[] value();
	}

	static final EventListener IGNORE = new EventListener(){
		@Override
		public boolean before(Event evt) {return true;}
		@Override
		public void on(Event evt) {}
	};

	private Class<?> listener;
	private BeforeCallbacks beforeCallbacks;
	private OnCallbacks onCallbacks;

	private EventListener() {}

	boolean isListening() {
		return listeningToOn() || listeningToBefore();
	}

	private boolean listeningToBefore() {
		return listener != null && !isEmpty(beforeCallbacks);
	}

	EventListener addBefore(String type, Method method) {
		if (beforeCallbacks == null)
			beforeCallbacks = new BeforeCallbacks();
		beforeCallbacks.add(type, method);
		return this;
	}

	EventListener removeBefore(String type, Method method) {
		if (beforeCallbacks != null)
			beforeCallbacks.remove(type, method);
		return this;
	}

	boolean before(Event evt) {
		return evt != null && listeningToBefore() ? beforeCallbacks.execute(evt, Klass.instance(listener)) : true;
	}

	private boolean listeningToOn() {
		return listener != null && !isEmpty(onCallbacks);
	}

	EventListener addOn(String type, Method method) {
		if (onCallbacks == null)
			onCallbacks = new OnCallbacks();
		onCallbacks.add(type, method);
		return this;
	}

	EventListener removeOn(String type, Method method) {
		if (onCallbacks != null)
			onCallbacks.remove(type, method);
		return this;
	}

	void on(Event evt) {
		if (evt != null && listeningToOn())
			onCallbacks.execute(evt, Klass.instance(listener));
	}
	@Override
	protected EventListener clone() {
		try {
			EventListener copy = EventListener.class.cast(super.clone());
			if (!isEmpty(beforeCallbacks))
				copy.beforeCallbacks = beforeCallbacks.clone();
			if (!isEmpty(onCallbacks))
				copy.onCallbacks = onCallbacks.clone();
			return copy;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	void print() {
		System.out.format("========== %s ==========", listener);
		if (beforeCallbacks != null) beforeCallbacks.print();
		if (onCallbacks != null) onCallbacks.print();
	}

	private static class Callbacks extends HashMap<String, ArrayList<Method>> implements Cloneable {
		private static final long serialVersionUID = 1L;

		static final boolean applicable(Parameter[] params, Class<? extends Event> evt) {
			if (params == null || params.length != 1) return false;
			Parameter param = params[0];
			return !param.isVarArgs()
				&& param.getType().isAssignableFrom(evt);
		}

		void add(String type, Method method) {
			if (type == null || method == null) return;
			ArrayList<Method> listeners = get(type);
			if (listeners == null)
				put(type, listeners = new ArrayList<>());
			if (!listeners.contains(method))
				listeners.add(method);
		}

		void remove(String type, Method method) {
			if (type == null) return;

			if (method != null) {
				ArrayList<Method> listeners = get(type);
				if (listeners != null)
					listeners.remove(method);
			} else
				remove(type);
		}

		Iterable<Method> methods(String type) {
			Iterable<Method> methods = get(type);
			return methods != null ? methods : Collections.emptyList();
		}

		void print() {
			forEach((type, methods) -> {
				System.out.print(type + ": ");
				methods.forEach(System.out::println);
			});
		}
	}

	private static class BeforeCallbacks extends Callbacks {
		private static final long serialVersionUID = 1L;

		static final boolean isValid(Method method, Class<? extends Event> evt) {
			return applicable(method.getParameters(), evt)
				&& (boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType()));
		}

		static final BeforeCallbacks get(Class<?> listenerClass, Class<? extends Event> eventClass) {
			BeforeCallbacks callbacks = new BeforeCallbacks();
			new AnnotatedMethods<Before>().get(listenerClass, Before.class, annotation -> {return eventClass.equals(annotation.event());}).forEach(ref -> {
				Method method = ref.method();
				if (!isValid(method, eventClass))
					throw new RuntimeException(method + " not applicable for the @Before annotation");
				ref.annotations().forEach(before -> {
					for (String type: before.types())
						callbacks.add(type, method);
				});
			});
			return !callbacks.isEmpty() ? callbacks : null;
		}

		boolean execute(Event evt, Object obj) {
			try {
				for (Method method: methods(evt.type())) {
					if (!method.isAccessible())
						method.setAccessible(true);
					boolean proceed = boolean.class.cast(method.invoke(obj, evt));
					if (!proceed)
						return false;
				}
				return true;
			} catch (Exception e) {
				throw runtimeException(e);
			}
		}
		@Override
		public BeforeCallbacks clone() {
			return BeforeCallbacks.class.cast(super.clone());
		}
	}

	private static class OnCallbacks extends Callbacks {
		private static final long serialVersionUID = 1L;

		static final boolean isValid(Method method, Class<? extends Event> evt) {
			return applicable(method.getParameters(), evt);
		}

		static final OnCallbacks get(Class<?> listenerClass, Class<? extends Event> eventClass) {
			OnCallbacks callbacks = new OnCallbacks();
			new AnnotatedMethods<On>().get(listenerClass, On.class, annotation -> {return eventClass.equals(annotation.event());}).forEach(ref -> {
				Method method = ref.method();
				if (!isValid(method, eventClass))
					throw new RuntimeException(method + " not applicable for the @On annotation");
				ref.annotations().forEach(on -> {
					for (String type: on.types())
						callbacks.add(type, method);
				});
			});
			return !callbacks.isEmpty() ? callbacks : null;
		}

		void execute(Event evt, Object obj) {
			methods(evt.type()).forEach((method)->{
				try {
					if (!method.isAccessible())
						method.setAccessible(true);
					method.invoke(obj, evt);
				} catch (Exception e) {
					throw runtimeException(e);
				}
			});
		}
		@Override
		public OnCallbacks clone() {
			return OnCallbacks.class.cast(super.clone());
		}
	}

	static EventListener build(Class<? extends Event> eventClass, Class<?> listenerClass) {
		if (eventClass == null || listenerClass == null) return null;

		BeforeCallbacks beforeCallbacks = BeforeCallbacks.get(listenerClass, eventClass);
		OnCallbacks onCallbacks = OnCallbacks.get(listenerClass, eventClass);
		if (beforeCallbacks == null && onCallbacks == null)
			return IGNORE;
		EventListener listener = new EventListener();
		listener.listener = listenerClass;
		listener.beforeCallbacks = beforeCallbacks;
		listener.onCallbacks = onCallbacks;
		return listener;
	}

	static final Event add(Event.Config cfg, Event evt) {
		if (cfg != null && evt != null)
			cfg.listeners().forEach(listener -> {evt.add(listener);});
		return evt;
	}
}