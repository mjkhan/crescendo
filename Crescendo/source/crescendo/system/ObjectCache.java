package crescendo.system;

import java.io.Serializable;

public interface ObjectCache<T extends Serializable> {
	public static <T extends Serializable> ObjectCache<T> create(Feature config) {
		Class<ObjectCache<T>> klass = config.klass("cache-class");
		return Feature.instance(klass).init(config);
	}

	public ObjectCache<T> init(Feature config);

	public default boolean updatesViaJMS() {
		return false;
	}

	public T get(String key);

	public void set(String key, T obj);

	public void update(String key, T obj);

	public T remove(String key);
}