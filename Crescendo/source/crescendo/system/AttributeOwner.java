package crescendo.system;

import horizon.data.DataRecord;
import horizon.persistence.AbstractPersistent;
import horizon.persistence.Column;
import horizon.system.AbstractObject;
import horizon.system.Klass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface AttributeOwner {
	public Object getAttribute(String name);

	public static class Attributes extends AbstractObject implements Serializable {
		private static final long serialVersionUID = 1L;
		private Class<? extends Entry> entryType;
		private HashMap<String, Entry> map;
		private ArrayList<Entry> dirties;

		public Attributes setEntryType(Class<? extends Entry> entryType) {
			this.entryType = entryType;
			return this;
		}

		public Attributes read(Iterable<DataRecord> records) {
			if (records != null && records.iterator().hasNext()) {
				map = new HashMap<>();
				for (DataRecord record: records) {
					Entry entry = Klass.instance(entryType);
					entry.read(record);
					map.put(entry.key, entry);
				}
			}
			return this;
		}

		public boolean isDirty() {
			return dirties != null && !dirties.isEmpty();
		}

		public <T extends Entry> Iterable<T> getDirties() {
			return !isDirty() ? Collections.emptyList() : (Iterable<T>)dirties;
		}

		private Attributes setDirty(Entry entry) {
			if (entry == null || !entry.state().isDirty()) return this;
			if (dirties == null)
				dirties = new ArrayList<>();
			if (!dirties.contains(entry))
				dirties.add(entry);
			return this;
		}

		public <T extends Entry> Iterable<T> entries() {
			return map == null ? Collections.emptyList() : (Iterable<T>)new ArrayList<>(map.values());
		}

		public Object get(String key) {
			if (map == null) return null;

			Entry entry = map.get(key);
			return entry != null ? entry.value : null;
		}

		public Attributes set(String key, Object value) {
			if (isEmpty(value))
				return remove(key);

			if (map == null)
				map = new HashMap<>();
			Entry entry = map.get(key);
			if (entry == null)
				map.put(key, entry = Klass.instance(entryType).setKey(key));
			return setDirty(entry.setValue(value));
		}

		public Attributes set(Map<String, Object> attrs) {
			if (!isEmpty(attrs))
				attrs.forEach(this::set);
			return this;
		}

		public Attributes remove(String key) {
			if (map == null) return this;
			Entry entry = map.remove(key);
			if (entry != null)
				entry.setRemoved();
			return setDirty(entry);
		}

		public Attributes setSaved() {
			if (!isEmpty(dirties)) {
				dirties.clear();
				dirties = null;
			}
			if (!isEmpty(map))
				for (Entry entry: map.values())
					entry.setSaved();
			return this;
		}

		public static class Entry extends AbstractPersistent implements Serializable {
			private static final long serialVersionUID = 1L;
			private String key;
			private Object value;
			@Column(name="attr_key", callon="write")
			public String key() {
				return key;
			}
			@Column(name="attr_key")
			public Entry setKey(String key) {
				if (!equals(this.key, key)) {
					this.key = key;
					setModified();
				}
				return this;
			}
			@Column(name="attr_val", callon="write")
			public Object value() {
				return value;
			}
			@Column(name="attr_val")
			public Entry setValue(Object value) {
				if (!equals(this.value, value)) {
					this.value = value;
					setModified();
				}
				return this;
			}

			private void setSaved() {
				state = state.save();
			}

			private void setRemoved() {
				state = state.remove();
			}
			@Override
			public String toString() {
				return getClass().getName() + "(" + key + ": " + value + ")";
			}
		}
	}
}