package crescendo.system.group;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import crescendo.system.Account;
import crescendo.system.AccountContext;
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

public class GroupServant extends Servant.Generic {
	public static <T extends GroupServant> T create(SiteContext sctx, String featureID) {
		Feature feature = Group.feature(sctx, featureID);
		return create(sctx, feature, feature.string("servant"));
	}

	public static GroupServant create(SiteContext sctx, Request req) {
		GroupServant servant = create(sctx, (String)req.notEmpty(Feature.ID));
		servant.groupType = req.string(Group.TYPE);
		servant.ownerType = req.string(Group.OWNER_TYPE);
		if (!isEmpty(servant.ownerType()))
			servant.ownerID = req.string("owner-id");
		return servant;
	}

	private String
		groupType,
		ownerType,
		ownerID;

	protected String groupType() {
		return notEmpty(ifEmpty(groupType, () -> feature.string(Group.TYPE)), Group.TYPE);
	}

	protected String ownerType() {
		return ifEmpty(ownerType, () -> feature.string(Group.OWNER_TYPE));
	}

	protected String ownerID() {
		return ownerID;
	}

	protected Group.Config config() {
		return Group.Config.get(feature, cref("obj"));
	}
	@Override
	protected PermissionInspector permission() {
		return permission(cref("permission"));
	}
	@Override
	public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
		List<Object> argList = qsupport.toList(args);
		argList.add(0, siteID());
		argList.add(1, groupType());
		String precondition = "site_id = ? and grp_type = ?",
			   ownerType = ownerType();
		if (!isEmpty(ownerType)) {
			precondition += " and owner_type = ?";
			argList.add(2, ownerType);
			if (!isEmpty(ownerID())) {
				precondition += " and owner_id = ?";
				argList.add(3, ownerID());
			}
		}
		condition = isEmpty(condition) ? precondition : precondition + "\nand " + condition;
		String table = config().table();
		return sqlDialect(cref("dialect"))
			  .select("search-" + table, null, table, condition, orderBy, argList, start, fetch);
	}

	public NamedObjects search(Request req) {
		return permission().check(req.defaultAction(Group.SEARCH), () -> {
			String condition = ifEmpty(req.string(Group.CONDITION), ""),
				   terms = qsupport.terms().condition(feature.string(cref("search")), req.string(Group.TERMS)),
				   orderBy = ifEmpty(req.string(Group.ORDER), "grp_id");

			if (!isEmpty(terms))
				condition = "".equals(condition) ? terms : condition + "\nand " + terms;
			String statusCondition = Group.Status.condition(req.string(Group.STATUS));
			if (!isEmpty(statusCondition)) {
				statusCondition = "status " + statusCondition;
				condition = isEmpty(condition) ? statusCondition : condition + "\nand " + statusCondition;
			}

			Object[] args = req.objects(Group.ARGS);
			int fetch = req.number(Group.FETCH).intValue(),
				start = req.number(Group.START).intValue();
			return new NamedObjects().set(cref("list"), search(condition, orderBy, start, fetch, args));
			}
			, null
			, null
		);
	}

	public Dataset getInfo(String fieldName, Object[] values) {
		String condition = qsupport.fieldCondition(fieldName, values) + " and status " + Group.Status.condition("none");
		return search(condition, qsupport.parameterize(values) ? values : null);
	}

	public Dataset getInfo(String... groupIDs) {
		return getInfo("grp_id", groupIDs);
	}

	public NamedObjects getInfo(Request req) {
		return dbAccess().open(dbaccess -> {
			String groupID = req.notEmpty(cref("id"));
			Dataset dataset = getInfo(groupID);
			NamedObjects result = new NamedObjects().set(cref("info"), dataset);
			if (dataset.isEmpty()) return result;

			return permission().check(req.defaultAction(Group.GET).set(cref("info"), dataset),
				() -> result, null, () -> req.remove(cref("info"))
			);
		}, null);
	}

	public NamedObjects viewInfo(Request req) {
		return dbAccess().open(dbaccess -> {
			Group.Tree<Group> tree = getGroupTree();
			Group group = tree != null ? tree.get(req.notEmpty(cref("id"))) : null;
			NamedObjects result = new NamedObjects().set(cref("obj"), group);
			if (group == null) return result;

			return permission().check(req.defaultAction(Group.VIEW).set(cref("obj"), group),
				() -> result, null, () -> req.remove(cref("obj"))
			);
		}, null);
	}

	public Dataset newInfo(String parentID, int count) {
		if (count < 1)
			throw new RuntimeException("invalid-group-count");
		String groupType = groupType();
		if (!isEmpty(parentID)) {
			Group parent = getGroup(parentID);
			if (parent == null)
				throw new RuntimeException("Group not found: " + parentID);
			if (!equals(parent.getType(), groupType))
				throw new RuntimeException("Inconsistent group type: " + parent.getType());
		}

		String ownerType = ownerType(),
			   ownerID = ownerID();
		Dataset groupSet = search("grp_id is null");
		for (int i = 0; i < count; ++i) {
			config().setDefaults(groupSet.append());
			groupSet.setValue("site_id", siteID());
			groupSet.setValue("grp_type", groupType);
			groupSet.setValue("prnt_id", parentID);
			groupSet.setValue("owner_type", ownerType);
			groupSet.setValue("owner_id", ownerID);
		}
		return groupSet;
	}

	public NamedObjects newInfo(Request req) {
		return new NamedObjects().set(cref("info"), newInfo(req.defaultAction(Group.NEW).string("parent-id"), Math.max(1, req.number(Group.COUNT).intValue())));
	}

	public <T extends Group> T.Tree<T> getGroupTree() {
		String condition = "status " + Group.Status.condition(null),
			   orderBy = "prnt_id, sort_ord, grp_id";
		Dataset dataset = search(condition, orderBy);
		if (dataset.isEmpty()) return null;

		return new HierarchyBuilder<T>()
			  .setElements(T.Factory.create(config(), dataset))
			  .build(T.Tree::new);
	}

	public <T extends Group> List<T> getCategories(boolean children, String... groupIDs) {
		if (isEmpty(groupIDs))
			return Collections.emptyList();

		if (!children)
			return dbAccess().open(dbaccess -> {
				Dataset dataset = getInfo(groupIDs);
				return T.Factory.create(config(), dataset);
			}, null);
		else {
			T.Tree<T> tree = getGroupTree();
			return tree == null || tree.isEmpty() ? null :
				   Stream.of(groupIDs).map(groupID -> tree.get(groupID)).collect(Collectors.toList());
		}
	}

	public <T extends Group> T getGroup(String groupID) {
		List<T> categories = getCategories(false, groupID);
		return !categories.isEmpty() ? categories.get(0) : null;
	}

	public Group.Validator.Result validate(String name, Map<String, Object> args) {
		return config().validator()
			  .set(dbAccess())
			  .validate(name, args);
	}

	public Group.Validator.Result validate(Request req) {
		return validate(
				  req.defaultAction(Group.VALIDATE).string(Group.Validator.NAME),
				  req.set(Group.TYPE, groupType()).set(Group.OWNER_TYPE, ownerType())
			   );
	}

	public int create(Group group) {
		group.setSiteID(siteID());
		group.setType(groupType());
		group.setOwnerType(ownerType());
		group.setOwnerID(ownerID());
		return persistence().create(group);
	}

	public int update(Group group) {
		return persistence().update(group);
	}

	protected int save(Group group, GroupEvent evt) {
		if (group == null) return 0;
		return dbAccess().transact(dbaccess -> {
			int result = 0;
			boolean event = evt != null;
			switch (group.state()) {
			case CREATED: result = create(group); break;
			case MODIFIED: result = update(group); break;
			default: break;
			}
			if (event && evt.setFired(result > 0).isFired()) {
				if (GroupEvent.UPDATE.equals(evt.type()))
					evt.set("changed-" + Group.FIELD_NAME, group.clearChangedFields());
				evt.on();
			}
			return result;
		}, null);
	}

	protected <T extends GroupEvent> T event() {
		T evt = GroupEvent.create(feature, cref("event"));
		evt.set(sctx);
		return evt;
	}

	public GroupEvent save(Group group) {
		GroupEvent evt = event();
		if (group != null
		 && evt.setType(Group.State.CREATED.equals(group.state()) ? GroupEvent.CREATE : GroupEvent.UPDATE)
		   .set(cref("obj"), group).before())
			save(group, evt);
		return evt;
	}

	public GroupEvent save(Request req) {
		Account account = AccountContext.get().ensureKnownPrincipal().account();

		FieldValues groupInfo = FieldValues.class.cast(req.get(cref("info")));
		if (isEmpty(groupInfo.get("site_id")))
			groupInfo.put("site_id", siteID());
		if (isEmpty(groupInfo.get("ins_id")))
			groupInfo.put("ins_id", account.getId());
		Group group = Group.State.CREATED.equals(groupInfo.state()) ?
				Group.Factory.create(config()) :
				getGroup(groupInfo.string("grp_id"));

		return permission().check(
			req.defaultAction(Group.State.CREATED.equals(groupInfo.state()) ? Group.CREATE : Group.UPDATE).set(cref("obj"),  group),
			() -> {
				if (group != null)
					group.read(persistence().getRecord(config().entityAs(), groupInfo));
				return save(group);
			}, null, () -> req.remove(cref("obj"))
		);
	}

	private GroupEvent move(Group.Tree<Group> tree, String destID, String... groupIDs) {
		Group dest = tree != null ? tree.get(destID) : null;
		GroupEvent evt = event();
		if (isEmpty(tree) || isEmpty(dest)
		 || isEmpty(destID) || isEmpty(groupIDs)
		 || !evt.setType(GroupEvent.MOVE).before()) return evt;

		return dbAccess().transact(dbaccess -> {
			for (String groupID: groupIDs) {
				Group group = tree.get(groupID);
				if (group == null) continue;
				if (group.equals(dest) || Group.Support.ofBranch(group, dest))
					throw CrescendoException.create(group + " unable to move to " + dest).setId("invalid-destination");
			}
			String cmd = "update {table} set prnt_id = ?, sort_ord = ?\nwhere site_id = ? and grp_id " + qsupport.asIn(groupIDs) + " and prnt_id <> ?";
			evt.setFired(dbaccess.update().execute(cmd.replace("{table}", config().table()), destID, Integer.MAX_VALUE, siteID(), destID) > 0).on();
			return evt;
		}, null);
	}

	public GroupEvent move(String destID, String... groupIDs) {
		return move(getGroupTree(), destID, groupIDs);
	}

	public GroupEvent move(Request req) {
		Group.Tree tree = getGroupTree();
		String dest = req.string("dest");
		String[] groupIDs = req.objects(cref("id"));
		return permission().check(req.defaultAction(Group.MOVE).set(cref("obj"), tree.get(dest)),
			() -> move(tree, dest, groupIDs),
			null,
			() -> req.remove(cref("obj"))
		);
	}

	public GroupEvent reorder(int offset, String... groupIDs) {
		GroupEvent evt = event();
		if (offset == 0 || isEmpty(groupIDs)
		|| !evt.setType(GroupEvent.REORDER).before()) return evt;

		String statusCondition = Group.Status.condition(null),
			   condition = "prnt_id = (select distinct prnt_id from {table} where site_id = ? and grp_id " +  qsupport.asIn(groupIDs) + "and status " + statusCondition + ")\nand status " + statusCondition;

		return dbAccess().transact(dbaccess -> {
			Dataset dataset = search(condition.replace("{table}", config().table()), "sort_ord", siteID());
			if (dataset.isEmpty()) return evt;

			List<DataRecord> records = dataset;
			for (String groupID: groupIDs)
				Records.reorder(records, Records.indexOf(records, "grp_id", groupID), offset);

			StringBuilder buff = new StringBuilder();
			for (int i = 0, size = records.size(); i < size; ++i)
				buff.append("\n\twhen '" + records.get(i).getValue("grp_id") + "' then " + i);
			String update = "update {table} set sort_ord = case grp_id {sort_ord}\n\telse sort_ord end\nwhere site_id = ? and grp_id " + qsupport.asIn(qsupport.fieldValues(records, "grp_id"));

			evt.setFired(dbaccess.update().execute(update.replace("{table}", config().table()).replace("{sort_ord}", buff.toString()), siteID()) > 0).on();
			return evt;
		}, null);
	}

	public GroupEvent reorder(Request req) {
		String[] groupIDs = notEmpty(req.objects(cref("id")), cref("id"));
		int offset = req.number("offset").intValue();
		return permission().check(req.defaultAction(Group.REORDER), () -> reorder(offset, groupIDs), null, null);
	}

	protected GroupEvent setStatus(List<Group> categories, Group.Status status) {
		GroupEvent evt = event();
		evt.setType(Group.Status.remove(status) ? GroupEvent.REMOVE : GroupEvent.CHANGE_STATUS);
		if (isEmpty(categories)
		|| !evt.set(cref("objs"), categories).set(Group.STATUS, status).before()) return evt;

		List<String> allIDs = Group.Support.getIDs(categories);
		String statusCode = status.code(),
			   cmd = "update {table} set status = ? where site_id = ? and grp_type = ? and grp_id " + qsupport.asIn(allIDs) + " and status <> ?";
		evt.set(cref("id"), allIDs)
		   .setFired(dbAccess().update().execute(cmd.replace("{table}", config().table()), statusCode, siteID(), groupType(), statusCode) > 0).on();
		return evt;
	}

	public GroupEvent setStatus(boolean children, Group.Status status, String... groupIDs) {
		return dbAccess().transact(dbaccess -> {return setStatus(getCategories(children, groupIDs), status);}, null);
	}

	public GroupEvent setStatus(Request req) {
		String[] groupIDs = req.notEmpty(cref("id"));
		Group.Status status = Group.Status.class.cast(req.get(Group.STATUS));
		List<Group> categories = getCategories(req.bool("children"), groupIDs);

		return permission().check(
			req.defaultAction(Group.Status.remove(status) ? Group.REMOVE : Group.CHANGE_STATUS).set(cref("objs"), categories),
			() ->  setStatus(categories, status),
			null, () -> req.remove(cref("objs"))
		);
	}
}