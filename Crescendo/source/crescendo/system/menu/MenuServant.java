package crescendo.system.menu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import crescendo.system.Account;
import crescendo.system.Client;
import crescendo.system.CrescendoException;
import crescendo.system.Feature;
import crescendo.system.NamedObjects;
import crescendo.system.PermissionInspector;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.SiteContext;
import horizon.data.DataRecord;
import horizon.data.Dataset;
import horizon.data.FieldValues;
import horizon.data.Records;
import horizon.data.hierarchy.HierarchyBuilder;

public class MenuServant extends Servant.Generic {
	public static <T extends MenuServant> T create(SiteContext sctx, String menuType) {
		Feature feature = Menu.feature(sctx, Menu.OBJ);
		T t = create(sctx, feature, Menu.OBJ + "-servant");
		t.setMenuType(menuType);
		return t;
	}

	public static MenuServant create(SiteContext sctx, Request req) {
		return create(sctx, (String)req.notEmpty(Menu.TYPE));
	}

	protected String menuType;

	void setMenuType(String type) {
		menuType = type;
	}

	protected Menu.Config config() {
		return Menu.Config.get(feature, Menu.OBJ);
	}
	@Override
	protected PermissionInspector permission() {
		return permission(Menu.OBJ + "-permission");
	}
	@Override
	public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
		List<Object> argList = qsupport.toList(args);
		String precondition = "site_id = ?";
		argList.add(0, siteID());
		if (!isEmpty(condition) && !condition.contains("menu_type ")) {
			precondition += " and menu_type = ?";
			argList.add(1, menuType);
		}
		condition = isEmpty(condition) ? precondition : precondition + "\nand " + condition;
		String table = config().table();
		return sqlDialect(Menu.OBJ + "-dialect")
			  .select("search-" + table, null, table, condition, orderBy, argList, start, fetch);
	}

	public NamedObjects search(Request req) {
		return permission().check(req.defaultAction(Menu.SEARCH), () -> {
			String condition = ifEmpty(req.string(Menu.CONDITION), ""),
				   terms = qsupport.terms().condition(feature.string(Menu.OBJ + "-search"), req.string(Menu.TERMS)),
				   orderBy = ifEmpty(req.string(Menu.ORDER), "menu_id");

			if (!isEmpty(terms))
				condition = "".equals(condition) ? terms : condition + "\nand " + terms;
			String statusCondition = Menu.Status.condition(req.string(Menu.STATUS));
			if (!isEmpty(statusCondition)) {
				statusCondition = "status " + statusCondition;
				condition = isEmpty(condition) ? statusCondition : condition + "\nand " + statusCondition;
			}

			Object[] args = req.objects(Menu.ARGS);
			int fetch = req.number(Menu.FETCH).intValue(),
				start = req.number(Menu.START).intValue();
			return new NamedObjects().set(Menu.LIST, search(condition, orderBy, start, fetch, args));
			}
			, null
			, null
		);
	}

	public Dataset getInfo(String fieldName, Object[] values) {
		String condition = qsupport.fieldCondition(fieldName, values) + " and status " + Menu.Status.condition("none");
		return search(condition, qsupport.parameterize(values) ? values : null);
	}

	public Dataset getInfo(String... menuIDs) {
		return getInfo("menu_id", menuIDs);
	}

	public NamedObjects getInfo(Request req) {
		return dbAccess().open(dbaccess -> {
			String menuID = req.notEmpty(Menu.ID);
			Dataset dataset = getInfo(menuID);
			NamedObjects result = new NamedObjects().set(Menu.INFO, dataset);
			if (dataset.isEmpty()) return result;

			return permission().check(req.defaultAction(Menu.GET).set(Menu.INFO, dataset),
				() -> result, null, () -> req.remove(Menu.INFO)
			);
		}, null);
	}

	public Dataset newInfo(String parentID, int count) {
		if (count < 1)
			throw new RuntimeException("invalid-menu-count");
		if (!isEmpty(parentID)) {
			Menu parent = getMenu(parentID);
			if (parent == null)
				throw new RuntimeException("Menu not found: " + parentID);
			if (!equals(parent.getType(), menuType))
				throw new RuntimeException("Inconsistent menu type: " + parent.getType());
		}

		Dataset menuSet = search("menu_id is null");
		for (int i = 0; i < count; ++i) {
			config().setDefaults(menuSet.append());
			menuSet.setValue("site_id", siteID());
			menuSet.setValue("menu_type", menuType);
			menuSet.setValue("prnt_id", parentID);
		}
		return menuSet;
	}

	public NamedObjects newInfo(Request req) {
		return new NamedObjects().set(Menu.INFO, newInfo(req.defaultAction(Menu.NEW).string("parent-id"), Math.max(1, req.number(Menu.COUNT).intValue())));
	}

	public <T extends Menu> Map<String, T.Tree<T>> getMenuTree() {
		String condition = "menu_type is not null and status " + Menu.Status.condition(null),
			   orderBy = "menu_type, prnt_id, sort_ord, menu_id";
		Dataset menuset = search(condition, orderBy);
		if (menuset.isEmpty()) return Collections.emptyMap();

		HierarchyBuilder<T> builder = new HierarchyBuilder<T>();
		LinkedHashMap<String, T.Tree<T>> result = new LinkedHashMap<>();
		Records.split(menuset, Dataset::new, "menu_type").forEach(dataset ->
			result.put(dataset.string("menu_type"), builder.setElements(T.Factory.create(config(), dataset)).build(T.Tree::new))
		);
		return result;
	}

	public <T extends Menu> List<T> getMenus(boolean children, String... menuIDs) {
		if (isEmpty(menuIDs))
			return Collections.emptyList();

		if (!children)
			return dbAccess().open(dbaccess -> {
				Dataset dataset = getInfo(menuIDs);
				return T.Factory.create(config(), dataset);
			}, null);
		else {
			Map<String, T.Tree<T>> trees = getMenuTree();
			T.Tree<T> tree = trees.get(menuType);
			return tree == null || tree.isEmpty() ? null :
				   Stream.of(menuIDs).map(menuID -> tree.get(menuID)).collect(Collectors.toList());
		}
	}

	public <T extends Menu> T getMenu(String menuID) {
		List<T> menus = getMenus(false, menuID);
		return !menus.isEmpty() ? menus.get(0) : null;
	}

	public Menu.Validator.Result validate(String name, Map<String, Object> args) {
		return config().validator()
			  .set(dbAccess())
			  .validate(name, args);
	}

	public Menu.Validator.Result validate(Request req) {
		return validate(
				  req.defaultAction(Menu.VALIDATE).string(Menu.Validator.NAME),
				  req.set(Menu.TYPE, menuType)
			   );
	}

	public int create(Menu menu) {
		menu.setSiteID(siteID());
		menu.setType(menuType);
		return persistence().create(menu);
	}

	public int update(Menu menu) {
		return persistence().update(menu);
	}

	protected int save(Menu menu, MenuEvent evt) {
		if (menu == null) return 0;
		return dbAccess().transact(dbaccess -> {
			int result = 0;
			boolean event = evt != null;
			switch (menu.state()) {
			case CREATED: result = create(menu); break;
			case MODIFIED: result = update(menu); break;
			default: break;
			}
			if (event && evt.setFired(result > 0).isFired()) {
				if (MenuEvent.UPDATE.equals(evt.type()))
					evt.set("changed-" + Menu.FIELD_NAME, menu.clearChangedFields());
				evt.on();
			}
			return result;
		}, null);
	}

	protected MenuEvent event() {
		MenuEvent evt = MenuEvent.create(feature);
		evt.set(sctx);
		return evt;
	}

	public MenuEvent save(Menu menu) {
		MenuEvent evt = event();
		if (menu != null
		 && evt.setType(Menu.State.CREATED.equals(menu.state()) ? MenuEvent.CREATE : MenuEvent.UPDATE)
		   .set(Menu.OBJ, menu).before())
			save(menu, evt);
		return evt;
	}

	public MenuEvent save(Request req) {
		Account account = Client.get(req).accountContext().ensureKnownPrincipal().account();

		FieldValues menuInfo = FieldValues.class.cast(req.get(Menu.INFO));
		if (isEmpty(menuInfo.get("site_id")))
			menuInfo.put("site_id", siteID());
		if (isEmpty(menuInfo.get("ins_id")))
			menuInfo.put("ins_id", account.getId());
		Menu menu = Menu.State.CREATED.equals(menuInfo.state()) ?
				Menu.Factory.create(config()) :
				getMenu(menuInfo.string("menu_id"));

		return permission().check(
			req.defaultAction(Menu.State.CREATED.equals(menuInfo.state()) ? Menu.CREATE : Menu.UPDATE).set(Menu.OBJ,  menu),
			() -> {
				if (menu != null)
					menu.read(persistence().getRecord(config().entityAs(), menuInfo));
				return save(menu);
			}, null, () -> req.remove(Menu.OBJ)
		);
	}

	private MenuEvent move(Menu.Tree<Menu> tree, String destID, String... menuIDs) {
		Menu dest = tree != null ? tree.get(destID) : null;
		MenuEvent evt = event();
		if (isEmpty(tree) || isEmpty(dest)
		 || isEmpty(destID) || isEmpty(menuIDs)
		 || !evt.setType(MenuEvent.MOVE).before()) return evt;

		return dbAccess().transact(dbaccess -> {
			for (String menuID: menuIDs) {
				Menu menu = tree.get(menuID);
				if (menu == null) continue;
				if (menu.equals(dest) || Menu.Support.ofBranch(menu, dest))
					throw CrescendoException.create(menu + " unable to move to " + dest).setId("invalid-destination");
			}
			String cmd = "update {table} set prnt_id = ?, sort_ord = ?\nwhere site_id = ? and menu_id " + qsupport.asIn(menuIDs) + " and prnt_id <> ?";
			evt.setFired(dbaccess.update().execute(cmd.replace("{table}", config().table()), destID, Integer.MAX_VALUE, siteID(), destID) > 0).on();
			return evt;
		}, null);
	}

	public MenuEvent move(String destID, String... menuIDs) {
		return move(getMenuTree().get(menuType), destID, menuIDs);
	}

	public MenuEvent move(Request req) {
		String dest = req.string("dest");
		String[] menuIDs = req.objects(Menu.ID);
		return permission().check(req.defaultAction(Menu.MOVE),
			() -> move(getMenuTree().get(menuType), dest, menuIDs),
			null,
			null
		);
	}

	public MenuEvent reorder(int offset, String... menuIDs) {
		MenuEvent evt = event();
		if (offset == 0 || isEmpty(menuIDs)
		|| !evt.setType(MenuEvent.REORDER).before()) return evt;

		String statusCondition = Menu.Status.condition(null),
			   condition = "prnt_id = (select distinct prnt_id from {table} where site_id = ? and menu_id " +  qsupport.asIn(menuIDs) + "and status " + statusCondition + ")\nand status " + statusCondition;

		return dbAccess().transact(dbaccess -> {
			Dataset dataset = search(condition.replace("{table}", config().table()), "sort_ord", siteID());
			if (dataset.isEmpty()) return evt;

			List<DataRecord> records = dataset;
			for (String menuID: menuIDs)
				Records.reorder(records, Records.indexOf(records, "menu_id", menuID), offset);

			StringBuilder buff = new StringBuilder();
			for (int i = 0, size = records.size(); i < size; ++i)
				buff.append("\n\twhen '" + records.get(i).getValue("menu_id") + "' then " + i);
			String update = "update {table} set sort_ord = case menu_id {sort_ord}\n\telse sort_ord end\nwhere site_id = ? and menu_id " + qsupport.asIn(qsupport.fieldValues(records, "menu_id"));

			evt.setFired(dbaccess.update().execute(update.replace("{table}", config().table()).replace("{sort_ord}", buff.toString()), siteID()) > 0).on();
			return evt;
		}, null);
	}

	public MenuEvent reorder(Request req) {
		String[] menuIDs = notEmpty(req.objects(Menu.ID), Menu.ID);
		int offset = req.number("offset").intValue();
		return permission().check(req.defaultAction(Menu.REORDER), () -> reorder(offset, menuIDs), null, null);
	}

	protected MenuEvent setStatus(List<Menu> categories, Menu.Status status) {
		MenuEvent evt = event();
		evt.setType(Menu.Status.remove(status) ? MenuEvent.REMOVE : MenuEvent.CHANGE_STATUS);
		if (isEmpty(categories)
		|| !evt.set(Menu.OBJS, categories).set(Menu.STATUS, status).before()) return evt;

		List<String> allIDs = Menu.Support.getIDs(categories);
		String statusCode = status.code(),
			   cmd = "update {table} set status = ? where site_id = ? and menu_type = ? and menu_id " + qsupport.asIn(allIDs) + " and status <> ?";
		evt.set(Menu.ID, allIDs)
		   .setFired(dbAccess().update().execute(cmd.replace("{table}", config().table()), statusCode, siteID(), menuType, statusCode) > 0).on();
		return evt;
	}

	public MenuEvent setStatus(boolean children, Menu.Status status, String... menuIDs) {
		return dbAccess().transact(dbaccess -> {return setStatus(getMenus(children, menuIDs), status);}, null);
	}

	public MenuEvent setStatus(Request req) {
		String[] menuIDs = req.notEmpty(Menu.ID);
		Menu.Status status = Menu.Status.class.cast(req.get(Menu.STATUS));
		List<Menu> categories = getMenus(req.bool("children"), menuIDs);

		return permission().check(
			req.defaultAction(Menu.Status.remove(status) ? Menu.REMOVE : Menu.CHANGE_STATUS).set(Menu.OBJS, categories),
			() ->  setStatus(categories, status),
			null, () -> req.remove(Menu.OBJS)
		);
	}
}