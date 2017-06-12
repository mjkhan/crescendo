package crescendo.system;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

import horizon.data.Convert;
import horizon.data.Dataset;
import horizon.data.GenericMap;
import horizon.system.Assert;

public class NamedObjects extends GenericMap<Object> {
	private static final long serialVersionUID = 1L;
	public static final NamedObjects EMPTY = new NamedObjects() {
		private static final long serialVersionUID = 1L;
		@Override
		public Object put(String key, Object value) {throw new UnsupportedOperationException();};
		@Override
		public void putAll(Map<? extends String,? extends Object> m) {throw new UnsupportedOperationException();};
	};

	protected static final CrescendoException exception(Throwable t) {
		return CrescendoException.create(t);
	}
	/**Returns the named field's value cast to String.
	 * @param name a field's name
	 * @return the named field's value
	 * @throws ClassCastException
	 */
	public String string(String name) {
		return Convert.asString(get(name));
	}
	/**Returns the named field's value cast to Number.
	 * @param name a field's name
	 * @return
	 * <ul><li>the value cast to Number</li>
	 * 	   <li>Integer(0) if the value is null</li>
	 * </ul>
	 * @throws ClassCastException
	 */
	public Number number(String name) {
		return Convert.asNumber(get(name));
	}
	/**Returns the named field's value converted to boolean.
	 * @param name a field's name
	 * @return
	 * <ul><li>the value converted to boolean</li>
	 * 	   <li>false if the value is null</li>
	 * </ul>
	 */
	public boolean bool(String name) {
		Object obj = get(name);
		if (obj == null)
			return false;

		Boolean bool = obj instanceof Boolean ? Boolean.class.cast(obj) : Boolean.valueOf(obj.toString());
		return bool.booleanValue();
	}

	public Dataset dataset(String name) {
		return Dataset.class.cast(get(name));
	}

	public <T> T[] objects(String name) {
		return (T[])get(name);
	}

	@Override
	public NamedObjects set(String name, Object obj) {
		super.set(name, obj);
		return this;
	}

	public NamedObjects setAll(Map<String, ?> objs) {
		if (!isEmpty(objs))
			super.putAll(objs);
		return this;
	}

	protected NamedObjects removeNotSerializable() {
		for (String key: keySet().toArray(new String[size()])) {
			Object obj = get(key);
			if (obj instanceof Serializable) continue;
			remove(key);
		}
		return this;
	}
	/**See {@link Assert#isEmpty(Object) Assert.isEmpty(...)}.
	 */
	protected static boolean isEmpty(Object obj) {
		return Assert.isEmpty(obj);
	}
	/**See {@link Assert#ifEmpty(Object, Supplier) Assert.ifEmpty(...)}*/
	protected static <T> T ifEmpty(T t, Supplier<T> nt) {
		return Assert.ifEmpty(t, nt);
	}
	/**See {@link Assert#ifEmpty(Object, Object) Assert.ifEmpty(...)}.
	 */
	protected static <T> T ifEmpty(T t, T nt) {
		return Assert.ifEmpty(t, nt);
	}
	/**See {@link Assert#notEmpty(Object, String) Assert.notEmpty(...)}.
	 */
	protected static <T> T notEmpty(T t, String name) {
		return Assert.notEmpty(t, name);
	}

	public <T> T notEmpty(String key) {
		return (T)notEmpty(get(key), "The value for '" + key + "'");
	}
}