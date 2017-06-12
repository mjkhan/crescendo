package crescendo.system;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;

import crescendo.util.TimeSupport;
import horizon.data.DataRecord;
import horizon.data.hierarchy.CompositeElement;
import horizon.data.hierarchy.Hierarchy;
import horizon.data.hierarchy.HierarchyElement;
import horizon.persistence.Column;

public abstract class HierarchicalEntity extends Entity implements CompositeElement {
	private static final long serialVersionUID = 1L;

	public static final String MOVE = "move";
	public static final String REORDER = "reorder";

	protected String
		siteID,
		id,
		name,
		parentID,
		creatorID;
	protected Timestamp createdAt;
	protected Status status;

	protected HierarchyInfo hinfo;
	protected HierarchicalEntity parent;
	protected LinkedHashSet<HierarchicalEntity> children;

	@Column(name="site_id", callon="write")
	public String getSiteID() {
		return siteID;
	}
	@Column(name="site_id")
	public void setSiteID(String siteID) {
		if (equals(this.siteID, siteID)) return;
		this.siteID = notEmpty(siteID, "siteID");
		setModified("siteID");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (equals(this.id, id)) return;
		this.id = notEmpty(id, "id");
		setModified("id");
	}

	protected abstract void setId();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (equals(this.name, name)) return;
		this.name = name;
		setModified("name");
	}

	@Column(name="prnt_id", callon="write")
	public String getParentID() {
		return parentID;
	}
	@Column(name="prnt_id")
	public void setParentID(String parentID) {
		if (equals(this.parentID, parentID)) return;
		this.parentID = parentID;
		setModified("parentID");
	}
	@Column(name="ins_id", callon="write")
	public String getCreatorID() {
		return creatorID;
	}
	@Column(name="ins_id")
	public void setCreatorID(String creatorID) {
		if (equals(this.creatorID, creatorID)) return;
		this.creatorID = notEmpty(creatorID, "creatorID");
		setModified("creatorID");
	}
	@Column(name="ins_time", callon="write")
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	@Column(name="ins_time")
	public void setCreatedAt(Timestamp createdAt) {
		if (equals(this.createdAt, createdAt)) return;
		this.createdAt = notEmpty(createdAt, "createdAt");
		setModified("createdAt");
	}

	public Status getStatus() {
		return ifEmpty(status, Status.ACTIVE);
	}

	public void setStatus(Status status) {
		if (equals(this.status, status)) return;
		this.status = notEmpty(status, "status");
		setModified("status");
	}
	@Override
	protected void read(String fieldName, Object value) {
		if ("status".equals(fieldName))
			setStatus(Status.codeOf(asString(value)));
		else
			super.read(fieldName, value);
	}
	@Override
	public void write(DataRecord record) {
		record.setValue("status", getStatus().code());
		if (State.CREATED.equals(state)) {
			if (isEmpty(id))
				setId();
			if (isEmpty(parentID))
				setParentID(getId());
			setCreatedAt(TimeSupport.now());
		}
		super.write(record);
	}
	@Override
	public HierarchyInfo hierarchyInfo() {
		if (hinfo == null)
			hinfo = new HierarchyInfo();
		return hinfo.setID(getId()).setParentID(getParentID());
	}
	@Override
	public HierarchicalEntity getParent() {
		return parent;
	}
	@Override
	public Collection<? extends HierarchicalEntity> getChildren() {
		return !isEmpty(children) ? children : Collections.emptySet();
	}

	public int getLevel() {
		return parent != null ? parent.getLevel() + 1 : 0;
	}
	@Override
	public void add(HierarchyElement e) {
		if (e == null || equals(e)) return;
		if (!getClass().isInstance(e))
			throw new IllegalArgumentException(this + " is not a(n) " + getClass().getName() + ".");
		HierarchicalEntity child = (HierarchicalEntity)e;
		if (equals(child.parent)) return;
		if (Support.ofBranch(child, this))
			throw new IllegalArgumentException(this + " is a child of " + child + ".");

		if (children == null)
			children = new LinkedHashSet<HierarchicalEntity>();

		if (child.parent != null && child.parent.children != null)
			child.parent.children.remove(child);

		child.parent = this;
		children.add(child);
	}
	@Override
	public String toString() {
		return String.format("%s(\"%s\", \"%s\", \"%s\")", getClass().getName(), getSiteID(), getId(), getName());
	}

	public static class Tree<T> extends Hierarchy<T> {
		private static final long serialVersionUID = 1L;

		private static String key(String prefix) {
			return prefix.trim() + "-hierarchy";
		}

		public static final <T> Tree<T> get(NamedObjects objs, String prefix) {
			return (Tree<T>)objs.get(key(prefix));
		}

		public void store(String prefix, BiConsumer<String, Tree<T>> setter) {
			setter.accept(key(prefix), this);
		}

		public void setTo(NamedObjects objs, String prefix) {
			objs.put(key(prefix), this);
		}
	}
}