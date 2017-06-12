package crescendo.system.cache;

import java.io.Serializable;

import crescendo.system.Feature;
import crescendo.system.ObjectCache;

public class DefaultCache<T extends Serializable> extends LRUCache<String, T> implements ObjectCache<T> {
	private static final long serialVersionUID = 1L;
	private boolean updateViaJMS;
	@Override
	public ObjectCache<T> init(Feature config) {
		setCapacity(config.number("cache-size").intValue());
		updateViaJMS = config.bool("update-vis-jms");
		log().debug(() -> String.format("A %s created with capacity of %d.", getClass().getName(), capacity));
		return this;
	}
	@Override
	public boolean updatesViaJMS() {
		return updateViaJMS;
	}
	@Override
	public void set(String key, T obj) {
		put (key, obj);
		log().debug(() -> String.format("%s (re)loaded to the cache(size:%d)", obj, size()));
	}
	@Override
	public void update(String key, T obj) {
		if (get(key) != null)
			set(key, obj);
	}
}