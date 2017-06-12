package crescendo.system.menu;

import java.util.Map;

import crescendo.system.Entity;

public class MenuValidator extends Menu.Validator {
	@Override
	public Result validate(String name, Map<String, Object> args) {
		if ("name".equals(name))
			return validName(args);
		return Result.VALID;
	}

	protected Result validName(Map<String, Object> args) {
		return validName(
			Menu.State.class.cast(args.get("state")),
			(String)args.get("siteID"),
			(String)args.get(Menu.TYPE),
			(String)args.get("id"),
			(String)args.get("parentID"),
			(String)args.get("name")
		);
	}

	protected Result validName(Menu.State state, String siteID, String menuType, String menuID, String parentID, String name) {
		if (isEmpty(name))
			return new Result().setMessage("empty-name");
		boolean create = Menu.State.CREATED.equals(state);
		String select = "select count(*) a_val from {table} where site_id = ? and menu_type = ? and prnt_id = ?{on-update} and menu_name = ?"
			  .replace("{table}", cfg.table()).replace("{on-update}", create ? "" : " and menu_id <> ?");
		Object[] args = create ? new Object[]{siteID, menuType, parentID, name} : new Object[]{siteID, menuType, parentID, menuID, name};
		Number number = dataAccess.query("a_val").getValue(select, args);
		if (number.intValue() > 0)
			return new Result().setMessage("name-in-use");
		return Result.VALID;
	}

	protected Result validName(Menu menu) {
		return validName(menu.state(), menu.getSiteID(), menu.getType(), menu.getId(), menu.getParentID(), menu.getName());
	}
	@Override
	public void doValidate(Entity entity) {
		validName(Menu.class.cast(entity));
	}
}