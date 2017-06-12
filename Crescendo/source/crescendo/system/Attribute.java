package crescendo.system;

import horizon.data.Convert;
import horizon.data.DataRecord;
import horizon.persistence.AbstractPersistent;
import horizon.persistence.Column;
import horizon.persistence.Selector;
import horizon.system.AbstractObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@Selector("select * from ${crsnd_attribute} where owner_type = ? and owner_id = ? and attr_name = ?")
public class Attribute extends AbstractPersistent implements Serializable {
	private static final long serialVersionUID = 1L;

	private String
		ownerType,
		ownerID,
		name;
	private Object value;

	public Attribute() {}
	@Column(name="owner_type", callon="write")
	public String getOwnerType() {
		return ownerType;
	}
	@Column(name="owner_type")
	public Attribute setOwnerType(String ownerType) {
		if (!equals(this.ownerType, ownerType)) {
			this.ownerType = notEmpty(ownerType, "ownerType");
			setModified();
		}
		return this;
	}
	@Column(name="owner_id", callon="write")
	public String getOwnerId() {
		return ownerID;
	}
	@Column(name="owner_id")
	public Attribute setOwnerId(String ownerId) {
		if (!equals(this.ownerID, ownerId)) {
			this.ownerID = notEmpty(ownerId, "ownerId");
			setModified();
		}
		return this;
	}
	@Column(name="attr_name", callon="write")
	public String getName() {
		return name;
	}
	@Column(name="attr_name")
	public Attribute setName(String name) {
		if (!equals(this.name, name)) {
			this.name = notEmpty(name, "name");
			setModified();
		}
		return this;
	}
	@Column(name="attr_val", callon="write")
	public Object getValue() {
		return value;
	}

	public String string() {
		return Convert.asString(value);
	}

	public Number number() {
		return Convert.asNumber(value);
	}
	@Column(name="attr_val")
	public Attribute setValue(Object value) {
		if (!equals(this.value, value)) {
			this.value = value;
			setModified();
		}
		return this;
	}

	public Attribute parse(String s) {
		if (!isEmpty(s)) {
			int index = s.indexOf("=");
			setName(index < 0 ? s : s.substring(0, index));
			setValue(index < 0 ? null : s.substring(index + 1));
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
		return String.format("%s(%s, %s)", getClass().getName(), name, value);
	}

	public static class List extends AbstractObject implements Serializable {
		private static final long serialVersionUID = 1L;
		private String
			ownerType,
			ownerID;
		private HashMap<String, Attribute> map;
		private ArrayList<Attribute> dirties;

		public String ownerType() {
			return ownerType;
		}

		public List setOwnerType(String ownerType) {
			this.ownerType = ownerType;
			return this;
		}

		private List readOwnerType(String ownerType) {
			if (isEmpty(this.ownerType))
				this.ownerType = ownerType;
			if (!equals(this.ownerType, ownerType))
				throw new IllegalArgumentException("A different ownerType is read: " + ownerType);
			return this;
		}

		private List readOwnerID(String ownerID) {
			if (isEmpty(this.ownerID))
				this.ownerID = ownerID;
			if (!equals(this.ownerID, ownerID))
				throw new IllegalArgumentException("A different ownerID is read: " + ownerID);
			return this;
		}

		public String ownerID()	{
			return ownerID;
		}

		public List setOwnerID(String ownerID) {
			this.ownerID = ownerID;
			return this;
		}

		public List read(Collection<DataRecord> records) {
			if (!isEmpty(records)) {
				map = new HashMap<>();
				for (DataRecord record: records) {
					Attribute attr = new Attribute();
					attr.read(record);
					readOwnerType(attr.getOwnerType()).readOwnerID(attr.getOwnerId());
					map.put(attr.name, attr);
				}
			}
			return this;
		}

		public boolean isDirty() {
			return dirties != null && !dirties.isEmpty();
		}

		public Collection<Attribute> getDirties() {
			return !isDirty() ? Collections.emptyList() : (Collection<Attribute>)dirties;
		}

		private List setDirty(Attribute attr) {
			if (attr == null || !attr.state().isDirty()) return this;
			if (dirties == null)
				dirties = new ArrayList<>();
			if (!dirties.contains(attr))
				dirties.add(attr);
			return this;
		}

		public Collection<Attribute> entries() {
			return map == null ? Collections.emptyList() : new ArrayList<>(map.values());
		}

		public Object get(String key) {
			if (map == null) return null;

			Attribute attr = map.get(key);
			return attr != null ? attr.value : null;
		}

		public List set(String key, Object value) {
			if (isEmpty(value))
				return remove(key);

			if (map == null)
				map = new HashMap<>();
			Attribute entry = map.get(key);
			if (entry == null)
				map.put(key, entry = new Attribute().setName(key));
			return setDirty(entry.setValue(value));
		}

		public List set(Map<String, Object> attrs) {
			if (!isEmpty(attrs))
				attrs.forEach(this::set);
			return this;
		}

		public List remove(String key) {
			if (map == null) return this;
			Attribute attr = map.remove(key);
			if (attr != null)
				attr.setRemoved();
			return setDirty(attr);
		}

		public List setSaved() {
			if (!isEmpty(dirties)) {
				dirties.clear();
				dirties = null;
			}
			if (!isEmpty(map))
				map.values().forEach(attr -> attr.setSaved());
			return this;
		}
	}
}