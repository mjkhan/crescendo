package crescendo.system.usergroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import crescendo.system.Feature;
import crescendo.system.NamedObjects;
import crescendo.system.Principal;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.SiteContext;
import crescendo.util.TimeSupport;
import horizon.data.DataRecord;
import horizon.data.Dataset;
import horizon.data.Records;

public class SiteUserServant extends Servant.Generic {
	public static final SiteUserServant create(SiteContext sctx, Feature feature) {
		Class<? extends SiteUserServant> klass = feature.klass(SiteUser.OBJ + "-servant");
		return Feature.instance(klass).set(sctx).set(feature);
	}

	public static final SiteUserServant create(SiteContext sctx, Request req) {
		return create(sctx, UserGroup.feature(sctx, req.notEmpty(Feature.ID)));
	}

	protected String siteuserTable() {
		return feature.string(SiteUser.OBJ + "-table");
	}
	@Override
	protected UserGroupPermission permission() {
		return (UserGroupPermission)permission(cref("permission"));
	}
	@Override
	public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
		condition = isEmpty(condition) ? "site_id = ?" : "site_id = ? and " + condition;
		List<Object> argList = qsupport.toList(args);
		argList.add(0, siteID());
		String table = siteuserTable();
		return sqlDialect(cref("dialect"))
			  .select("search-" + table, null, table, condition, orderBy, argList, start, fetch);
	}

	@Override
	public Dataset search(String condition, String orderBy, Object... args) {
		return search(condition, orderBy, -1, 0, args);
	}

	@Override
	public Dataset search(String condition, Object... args) {
		return search(condition, null, args);
	}

	protected static final String SEARCH = SiteUser.OBJ + "-search";

	public NamedObjects search(Request req) {
		return permission().check(req.defaultAction(SiteUser.SEARCH), () -> {
			String groupID = ifEmpty(req.string(UserGroup.ID), ""),
				   userType = ifEmpty(req.string(SiteUser.TYPE), Principal.Type.ACCOUNT::code),
				   terms = qsupport.terms().condition(feature.string(SEARCH), req.string(SiteUser.TERMS)),
				   condition = ifEmpty(req.string(SiteUser.CONDITION), ""),
				   orderBy = ifEmpty(req.string(SiteUser.ORDER), "grp_id, user_type, user_id");
			if (!isEmpty(groupID))
				condition += "".equals(condition) ? "grp_id = '" + groupID + "'" : "\nand grp_id = '" + groupID + "'";
			condition += "".equals(condition) ? "user_type = '" + userType + "'" : "\nand user_type = '" + userType + "'";
			if (!isEmpty(terms))
				condition += "".equals(condition) ? terms : "\nand " + terms;
			Object[] args = req.objects(UserGroup.ARGS);
			int fetch = req.number(SiteUser.FETCH).intValue(),
				start = req.number(SiteUser.START).intValue();
			return new NamedObjects().set(SiteUser.LIST, search(condition, orderBy, start, fetch, args));
		}, () -> NamedObjects.EMPTY, null);
	}

	public Map<String, List<String>> getUserGroupIDs(String...userIDs) {
		String select =
			"select a.user_id, case when grp_id is null then '{guest}' else grp_id end grp_id"
		+ "\nfrom ({users}) a"
		+ "\nleft outer join crsnd_site_user b on b.user_id {user-ids} and a.user_id = b.user_id"
		+ "\norder by a.user_id, grp_id",
			users = Stream.of(userIDs).map(userID -> "select '{user-id}' user_id from crsnd_dummy".replace("{user-id}", userID)).collect(Collectors.joining(" union\n"));
		select = select
				.replace("{guest}", UserGroup.Generic.GUEST.getId())
				.replace("{users}", users)
				.replace("{user-ids}", qsupport.asIn(userIDs));
		Dataset dataset = dbAccess().query("site-user-groups").getRecords(select, (Object[])null);

		Map<String, List<DataRecord>> tmp = Records.map(dataset, ArrayList::new, "user_id");
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		tmp.forEach((key, value) -> {
			result.put(key, value.stream().map(record -> record.string("grp_id")).collect(Collectors.toCollection(ArrayList::new)));
		});
		return result;
	}

	protected int add(UserGroupEvent evt, Dataset userSet, String... groupIDs) {
		if (isEmpty(groupIDs) || isEmpty(userSet) || userSet.isEmpty()) return 0;
		boolean event = evt != null;
		if (event && !evt.before()) return 0;

		boolean siteuser = userSet.hasField("user_name");
		String userNameField = siteuser ? "user_name" : "alias",
			   users = Stream.of(groupIDs).map(groupID ->
			userSet.stream().map(record -> {
				String image = record.string("img_url");
				return "    select '{site_id}' site_id, '{group-id}' grp_id, '{user}' user_id, '{user-type}' user_type, '{username}' user_name, {image} img_url, '{now}' ins_time from crsnd_dummy"
					   .replace("{group-id}", groupID).replace("{user}", record.string("user_id"))
				   	   .replace("{user-type}", siteuser ? record.string("user_type") : Principal.Type.ACCOUNT.code())
				   	   .replace("{username}", record.string(userNameField))
				   	   .replace("{image}", isEmpty(image) ? "null" : "'" + image + "'");
			}).collect(Collectors.joining(" union\n"))
		).collect(Collectors.joining(" union\n"));

		String cmd = "insert into {table} (site_id, grp_id, user_id, user_type, user_name, img_url, ins_time)"
			     + "\nselect site_id, grp_id, user_id, user_type, user_name, img_url, ins_time from ("
				 + "\n{users}\n) a where not exists ("
			     + "\n    select grp_id, user_id, user_type from {table}"
				 + "\n    where site_id = '{site_id}'"
				 + "\n    and grp_id " + qsupport.asIn(groupIDs)
				 + "\n    and user_id " + qsupport.asIn(qsupport.fieldValues(userSet, "user_id"))
				 + "\n    and grp_id = a.grp_id and user_id = a.user_id and user_type = a.user_type"
				 + "\n)";

		return dbAccess().transact(dbaccess -> {
			int result = dbaccess.update().execute(
					cmd.replace("{users}", users).replace("{table}", siteuserTable()).replace("{site_id}", siteID()).replace("{now}", TimeSupport.now().toString())
				 );
			if (event && evt.setFired(result > 0).isFired())
				evt.set(UserGroup.ID, groupIDs)
				   .set(SiteUser.ID, qsupport.asStrings(userSet, "user_id"))
				   .on();
			return result;
		}, null);
	}

	protected <T extends UserGroupEvent> T event() {
		T evt = UserGroupEvent.create(feature, cref("event"));
		evt.set(sctx);
		return evt;
	}

	public UserGroupEvent add(Dataset userSet, String... groupIDs) {
		UserGroupEvent evt = event();
		evt.set(sctx).setType(UserGroupEvent.CHANGE_USER);
		add(evt, userSet, groupIDs);
		return evt;
	}

	public UserGroupEvent add(Request req) {
		return permission().check(req.defaultAction(SiteUser.ADD), () -> {
			Dataset userSet = Dataset.class.cast(req.remove(SiteUser.LIST));
			String[] groupIDs = req.objects(UserGroup.ID);
			if (groupIDs != null)
				for (String groupID: groupIDs)
					if (UserGroup.Generic.isBlocked(groupID))
						throw new IllegalArgumentException("Use the block(...) method instead.");
			return add(userSet, groupIDs);
		}, null, null);
	}

	protected int remove(UserGroupEvent evt, String[] groupIDs, String[] userIDs) {
		boolean event = evt != null;
		if (event && !evt.before()) return 0;

		String cmd = "delete from {table} where site_id = ?{groups}{users}"
					.replace("{table}", siteuserTable())
					.replace("{groups}", !isEmpty(groupIDs) ? " and grp_id " + qsupport.asIn(groupIDs) : "")
					.replace("{users}", !isEmpty(userIDs) ? " and user_id " + qsupport.asIn(userIDs) : "");
		return dbAccess().transact(dbaccess -> {
			int result = dbaccess.update().execute(cmd, siteID());
			if (event && evt.setFired(result > 0).isFired()) {
				evt.set(UserGroup.ID, groupIDs)
				   .set(SiteUser.ID, ifEmpty(userIDs, () -> qsupport.asStrings(getUsers(groupIDs, null), "user_id")))
				   .on();
			}
			return result;
		}, null);
	}

	public UserGroupEvent remove(String[] groupIDs, String[] userIDs) {
		UserGroupEvent evt = event();
		evt.set(sctx).setType(UserGroupEvent.CHANGE_USER);
		remove(evt, groupIDs, userIDs);
		return evt;
	}

	public UserGroupEvent remove(Request req) {
		return permission().check(req.defaultAction(SiteUser.REMOVE), () -> {
			String[] groupIDs = req.objects(UserGroup.ID),
					 userIDs = req.objects(SiteUser.ID);
			return remove(groupIDs, userIDs);
		}, null, null);
	}

	protected Dataset getUsers(String[] groupIDs, String[] userIDs) {
		String condition = "";
		if (!isEmpty(groupIDs))
			condition = "grp_id " + qsupport.asIn(groupIDs);
		if (!isEmpty(userIDs))
			condition += "".equals(condition) ? "user_id " + qsupport.asIn(userIDs) : " and user_id " + qsupport.asIn(userIDs);
		return search(condition, "user_id", -1, 0, (Object[])null);
	}

	protected int set(UserGroupEvent evt, String groupID, String... userIDs) {
		boolean event = evt != null;
		if (event && !evt.before()) return 0;

		return dbAccess().transact(dbaccess -> {
			Dataset userSet = getUsers(null, userIDs);
			if (userSet.isEmpty()) return 0;

			int result = remove(null, null, userIDs)
					   + add(null, userSet, groupID);
				if (event && evt.setFired(result > 0).isFired()) {
					evt.set(UserGroup.ID, groupID)
					   .set(SiteUser.ID, userIDs)
					   .on();
				}
			return result;
		}, null);
	}

	public UserGroupEvent set(String groupID, String... userIDs) {
		UserGroupEvent evt = event();
		evt.set(sctx).setType(UserGroupEvent.CHANGE_USER);
		set(evt, groupID, userIDs);
		return evt;
	}

	public UserGroupEvent set(Request req) {
		return permission().check(req.defaultAction(SiteUser.SET), () -> {
			String groupID = req.string(UserGroup.ID);
			String[] userIDs = req.objects(SiteUser.ID);
			return set(groupID, userIDs);
		}, null, null);
	}

	protected int block(UserGroupEvent evt, Dataset userSet) {
		if (userSet == null || userSet.isEmpty()) return 0;
		boolean event = evt != null;
		if (event && !evt.before()) return 0;

		String[] userIDs = qsupport.asStrings(userSet, "user_id");

		return dbAccess().transact(dbaccess -> {
			int result = remove(null, null, userIDs)
					   + add(null, userSet, UserGroup.Generic.BLOCKED.getId());
			if (event && evt.setFired(result > 0).isFired()) {
				evt.set(UserGroup.ID, UserGroup.Generic.BLOCKED.getId())
				   .set(SiteUser.ID, userIDs)
				   .on();
			}
			return result;
		}, null);
	}

	public UserGroupEvent block(Dataset userSet) {
		UserGroupEvent evt = event();
		evt.set(sctx).setType(UserGroupEvent.CHANGE_USER);
		block(evt, userSet);
		return evt;
	}

	public UserGroupEvent block(Request req) {
		return permission().check(req.defaultAction(SiteUser.BLOCK), () -> {
			Dataset userSet = Dataset.class.cast(req.remove(SiteUser.LIST));
			return block(userSet);
		}, null, null);
	}
}