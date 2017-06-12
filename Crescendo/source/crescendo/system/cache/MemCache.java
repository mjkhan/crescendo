package crescendo.system.cache;

import java.io.Serializable;

import crescendo.system.Feature;
import crescendo.system.ObjectCache;
import horizon.system.AbstractObject;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

public class MemCache<T extends Serializable> extends AbstractObject implements ObjectCache<T> {
	private int expiration;
	private MemcachedClient memcachedClient;
	@Override
	public ObjectCache<T> init(Feature config) {
		try {
			expiration = config.number("expiration").intValue();
			String addresses = config.string("addresses");
			memcachedClient = new MemcachedClient(AddrUtil.getAddresses(addresses));
			log().debug(() -> String.format("A %s created with expiration of %d at %s.", getClass().getName(), expiration, addresses));
			return this;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}
	@Override
	public T get(String key) {
		return (T)memcachedClient.get(key);
	}
	@Override
	public void set(String key, T obj) {
		memcachedClient.add(key, expiration, obj);
		log().debug(() -> String.format("%s (re)loaded to the cache", obj));
	}
	@Override
	public void update(String key, T obj) {
		memcachedClient.replace(key, expiration, obj);
	}
	@Override
	public T remove(String key) {
		T removed = get(key);
		if (removed != null)
			memcachedClient.delete(key);
		return removed;
	}
}