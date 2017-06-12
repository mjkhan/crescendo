package crescendo.system.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import horizon.system.AbstractObject;
/**An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of elements (<code>cacheSize</code>).
 * If the cache is full and another entry is added, the LRU (least recently used) entry is dropped.
 * <p>
 * This class is thread-safe. All methods of this class are synchronized.<br>
 * Author: Christian d'Heureuse (<a href="http://www.source-code.biz">www.source-code.biz</a>)<br>
 * License: <a href="http://www.gnu.org/licenses/lgpl.html">LGPL</a>.<br>
 * Modified by Emjay Khan
 */
public class LRUCache<K, V> extends AbstractObject implements Serializable {
	private static final long serialVersionUID = 1L;

	protected static final float LOAD_FACTOR = 0.75f;

	protected int capacity;
	private LinkedHashMap<K, V> map;
/*
	*//**Creates a new LRU cache.
	 * @param capacity the maximum number of entries that will be kept in this cache.
	 *//*
	public LRUCache (int capacity) {
	   this.capacity = capacity;
	   map = getMap();
	}
*/
	/**Sets the capacity of the LRUCache.
	 * @param capacity the maximum number of entries that will be kept in this cache.
	 * @return the LRUCache
	 */
	public LRUCache<K, V> setCapacity(int capacity) {
		this.capacity = capacity;
		this.map = getMap();
		return this;
	}
	/**Returns a set of the keys.
	 * @return set of the keys
	 */
	public Set<K> keySet() {
		return map.keySet();
	}
	/**Returns a LinkedHashMap to hold the cache entries.
	 * @return LinkedHashMap
	 */
	protected LinkedHashMap<K, V> getMap() {
	   int hashTableCapacity = (int)Math.ceil(capacity / LOAD_FACTOR) + 1;
	   return new LinkedHashMap<K, V>(hashTableCapacity, LOAD_FACTOR, true) {
		      private static final long serialVersionUID = 1;
		      @Override
		      protected boolean removeEldestEntry (Map.Entry<K, V> eldest) {
		         return LRUCache.this.size() > LRUCache.this.capacity;
		      }
		   };
	}
	/**Retrieves an entry from the cache.<br>
	 * The retrieved entry becomes the MRU (most recently used) entry.
	 * @param key the key whose associated value is to be returned.
	 * @return    the value associated to this key, or null if no value with this key exists in the cache.
	 */
	public synchronized V get (K key) {
	   return map.get(key);
	}
	/**Adds an entry to this cache.
	 * If the cache is full, the LRU (least recently used) entry is dropped.
	 * @param key    the key with which the specified value is to be associated.
	 * @param value  a value to be associated with the specified key.
	 */
	public synchronized void put (K key, V value) {
	   map.put (key,value);
	}
	/**Clears the cache.
	 */
	public synchronized void clear() {
	   map.clear();
	}
	/**Returns the number of used entries in the cache.
	 * @return the number of entries currently in the cache.
	 */
	public synchronized int usedEntries() {
	   return map.size();
	}
	/**Returns a <code>Collection</code> that contains a copy of all cache entries.
	 * @return a <code>Collection</code> with a copy of the cache content.
	 */
	public synchronized Collection<Map.Entry<K, V>> getAll() {
	   return new ArrayList<Map.Entry<K, V>>(map.entrySet());
	}
	/**Returns the size of the cache.
	 * @return the size of the cache
	 */
	public synchronized int size() {
		return map.size();
	}
	/**Returns the capacity of the cache.
	 * @return the capacity of the cache
	 */
	public int capacity() {
		return capacity;
	}
	/**Removes the value with the key
	 * @param key Key
	 * @return removed entry
	 */
	public synchronized V remove(K key) {
		return map.remove(key);
	}

/*	public static void main(String[] args) {
		LRUCache<Integer, String> cache = new LRUCache<Integer, String>(3);
		cache.put(Integer.valueOf(0), "Zero");
		print(cache.map.values());
		cache.put(Integer.valueOf(1), "One");
		print(cache.map.values());
		cache.put(Integer.valueOf(2), "Two");
		print(cache.map.values());
		cache.put(Integer.valueOf(3), "Three");
		print(cache.map.values());
		cache.put(Integer.valueOf(4), "Four");
		print(cache.map.values());
	}

	private static void print(Iterable<String> strs) {
		for (String s: strs)
			System.out.println(s);
		System.out.println("");
	}
*/
}