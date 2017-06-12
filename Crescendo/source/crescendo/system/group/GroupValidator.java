package crescendo.system.group;

import java.util.Map;

import crescendo.system.Entity;

public class GroupValidator extends Group.Validator {
	@Override
	public Result validate(String name, Map<String, Object> args) {
		if ("name".equals(name))
			return validName(args);
		return Result.VALID;
	}

	protected Result validName(Map<String, Object> args) {
		return validName(
			Group.State.class.cast(args.get("state")),
			(String)args.get("siteID"),
			(String)args.get(Group.TYPE),
			(String)args.get("id"),
			(String)args.get("parentID"),
			(String)args.get("name")
		);
	}

	protected Result validName(Group.State state, String siteID, String groupType, String groupID, String parentID, String name) {
		if (isEmpty(name))
			return new Result().setMessage("empty-name");
		boolean create = Group.State.CREATED.equals(state);
		String select = "select count(*) a_val from {table} where site_id = ? and grp_type = ? and prnt_id = ?{on-update} and grp_name = ?"
			  .replace("{table}", cfg.table()).replace("{on-update}", create ? "" : " and grp_id <> ?");
		Object[] args = create ? new Object[]{siteID, groupType, parentID, name} : new Object[]{siteID, groupType, parentID, groupID, name};
		Number number = dataAccess.query("a_val").getValue(select, args);
		if (number.intValue() > 0)
			return new Result().setMessage("name-in-use");
		return Result.VALID;
	}

	protected Result validName(Group group) {
		return validName(group.state(), group.getSiteID(), group.getType(), group.getId(), group.getParentID(), group.getName());
	}
	@Override
	public void doValidate(Entity entity) {
		validName(Group.class.cast(entity));
	}
}