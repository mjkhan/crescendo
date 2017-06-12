package crescendo.system.action;

import crescendo.system.Entity;
import horizon.persistence.Column;
import horizon.persistence.Selector;
@Selector("select * from ${crsnd_action} where action_grp = ? and action_id = ? and action_target = ?")
public class Action extends Entity {
	private static final long serialVersionUID = 1L;

	private String
		group,
		id,
		target,
		name,
		description;
	private int sortOrder;
	@Column(name="action_grp", callon="write")
	public String getGroup() {
		return group;
	}
	@Column(name="action_grp")
	public void setGroup(String group) {
		if (equals(this.group, group)) return;
		this.group = notEmpty(group, "group");
		setModified("group");
	}
	@Column(name="action_id", callon="write")
	public String getId() {
		return id;
	}
	@Column(name="action_id")
	public void setId(String id) {
		if (equals(this.id, id)) return;
		this.id = notEmpty(id, "id");
		setModified("id");
	}
	@Column(name="action_target", callon="write")
	public String getTarget() {
		return target;
	}
	@Column(name="action_target")
	public void setTarget(String target) {
		if (equals(this.target, target)) return;
		this.target = ifEmpty(target, "*");
		setModified("target");
	}
	@Column(name="action_name", callon="write")
	public String getName() {
		return name;
	}
	@Column(name="action_name")
	public void setName(String name) {
		if (equals(this.name, name)) return;
		this.name = notEmpty(name, "name");
		setModified("name");
	}
	@Column(name="descrp", callon="write")
	public String getDescription() {
		return description;
	}
	@Column(name="descrp")
	public void setDescription(String description) {
		if (equals(this.description, description)) return;
		this.description = description;
		setModified("description");
	}
	@Column(name="sort_ord", callon="write")
	public int getSortOrder() {
		return sortOrder;
	}
	@Column(name="sort_ord")
	public void setSortOrder(int sortOrder) {
		if (this.sortOrder == sortOrder) return;
		if (sortOrder < 0)
			throw new RuntimeException("sortOrder < 0");
		this.sortOrder = sortOrder;
		setModified("sortOrder");
	}
}