package crescendo.system.group;

import java.util.List;
import java.util.Map;

import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.SiteContext;
import crescendo.util.TimeSupport;
import horizon.persistence.Persistent.Info;

public class MemberServant extends Servant.Generic {
	public static <T extends MemberServant> T create(SiteContext sctx, String featureID) {
		Feature feature = Group.feature(sctx, featureID);
		return create(sctx, feature, feature.string("member-servant"));
	}

	public static <T extends MemberServant> T create(SiteContext sctx, Request req) {
		return create(sctx, (String)req.notEmpty(Feature.ID));
	}

	protected String memberTable() {
		return Info.table(feature.klass("member"));
	}

	protected String groupType() {
		return feature.feature("group").string("group-type");
	}

	protected String memberType() {
		return feature.string("member-type");
	}

	protected String selectGroups(String prefix, String... groupIDs) {
		return qsupport.toString(qsupport.toList(groupIDs), prefix + "select '{item}' grp_id from crsnd_dummy", " union\n");
	}

	protected String selectMembers(String prefix, List<? extends Map<String, ?>> memberset) {
		return qsupport.toString(memberset, prefix + "select '{mb_type}' mb_type, '{mb_id}' mb_id from crsnd_dummy", " union\n", "mb_type", "mb_id");
	}

	protected int doAdd(List<? extends Map<String, ?>> memberset, String... groupIDs) {
		if (isEmpty(memberset) || isEmpty(groupIDs)) return 0;

		String groups = selectGroups("    ", groupIDs),
			   members = selectMembers("    ", memberset),
			   cmd = new StringBuilder()
			.append("insert into {table} (site_id, grp_type, grp_id, mb_type, mb_id, sort_ord, ins_id, ins_time)")
			.append("\nselect :site-id, :group-type, grp_id, mb_type, mb_id, :sort-ord, :ins-id, :ins-time")
			.append("\nfrom (\n{groups}) a, (\n{members}) b")
			.append("\nwhere not exists (")
			.append("\n    select mb_type, mb_id from {table} c")
			.append("\n    where site_id = :site-id and grp_type = :group-type and grp_id " + qsupport.asIn(groupIDs))
			.append("\n    and c.mb_type = b.mb_type and c.mb_id = b.mb_id")
			.append("\n)").toString()
			.replace("{table}", memberTable()).replace("{groups}", groups).replace("{members}", members);
		return dbAccess().update().statement(cmd)
			.param("site-id", siteID()).param("group-type", groupType()).param("sort-ord", feature.number("unordered"))
			.param("ins-id", accountContext().account().getId()).param("ins-time", TimeSupport.now())
			.exec();
	}

	protected int doAdd(String[] memberIDs, String... groupIDs) {
		return doAdd(Member.toList(memberType(), memberIDs), groupIDs);
	}

	protected int doCopy(List<? extends Map<String, ?>> memberset, String src, String dest) {
		if (isEmpty(src) || isEmpty(dest)) return 0;

		StringBuilder buff = new StringBuilder()
			.append("insert into {table} (site_id, grp_type, grp_id, mb_type, mb_id, sort_ord, ins_id, ins_time)")
			.append("\nselect site-id, grp-type, :dest, mb_type, mb_id, :sort-ord, :ins-id, :ins-time")
			.append("\nfrom {table} a")
			.append("\nwhere site_id = :site-id and grp_type = :group-type and grp_id = :src");
		if (!isEmpty(memberset))
			buff.append("\nand exists (\n")
				.append(selectMembers("    ", memberset))
				.append("\n    where mb_type = a.mb_type and mb_id = a.mb_id")
				.append("\n)");
		buff.append("\nand not exists (");
		buff.append("\n    select mb_type, mb_id from {table} where site_id = :site-id and grp_type = :group-type and grp_id = :dest");
		buff.append("\n    and mb_type = a.mb_type and mb_id = a.mb_id");
		buff.append("\n)");

		return dbAccess().update().statement(buff.toString().replace("{table}", memberTable()))
				.param("site-id", siteID()).param("group-type", groupType()).param("src", src).param("dest", dest)
				.param("sort-ord", feature.number("unordered")).param("ins-id", accountContext().account().getId()).param("ins-time", TimeSupport.now())
				.exec();
	}

	protected int doCopy(String[] memberIDs, String src, String dest) {
		return doCopy(Member.toList(memberType(), memberIDs), src, dest);
	}

	protected int doDelete(List<? extends Map<String, ?>> memberset, boolean in, String... groupIDs) {
		StringBuilder buff = new StringBuilder("delete from " + memberTable() + " a where a.site_id = :site-id and a.grp_type = :group-type");
		if (!isEmpty(groupIDs))
			buff.append("and a.grp_id " + (in ? "" : "not ") + qsupport.asIn(groupIDs));
		if (!isEmpty(memberset))
			buff.append("\nand exists (")
				.append("\n    select mb_type, mb_id from (\n")
				.append(selectMembers("        ", memberset))
				.append("\n    ) b")
				.append("\nwhere b.mb_type = a.mb_type and b.mb_id = a.mb_id)");
		return dbAccess().update().statement(buff.toString()).param("site-id", siteID()).param("group-type", groupType()).exec();
	}

	protected int doDelete(String[] memberIDs, boolean in, String... groupIDs) {
		return doDelete(Member.toList(memberType(), memberIDs), in, groupIDs);
	}

	protected int doSet(List<? extends Map<String, ?>> memberset, String... groupIDs) {
		return dbAccess().transact(dbaccess -> doAdd(memberset, groupIDs) + doDelete(memberset, false, groupIDs), null);
	}

	protected int doSet(String[] memberIDs, String... groupIDs) {
		return doSet(Member.toList(memberType(), memberIDs), groupIDs);
	}

	protected int doMove(List<? extends Map<String, ?>> memberset, String src, String dest) {
		return dbAccess().transact(dbaccess -> doCopy(memberset, src, dest) + doDelete(memberset, false, src), null);
	}

	protected int doMove(String[] memberIDs, String src, String dest) {
		return doMove(Member.toList(memberType(), memberIDs), src, dest);
	}

	protected int doReorder(String groupID, List<? extends Map<String, ?>> memberset) {
		if (isEmpty(groupID) || isEmpty(memberset)) return 0;

		String orders = qsupport.toString(memberset, "    when mb_type = '{mb-type}' and mb_id = '{mb-id}' then {index}", "\n"),
			   cmd = new StringBuilder("update {table} set sort_ord = case\n{orders}\n    else :unordered end")
				.append("\nwhere site_id = :site-id and grp_type = :group-type and grp_id = :group-id").toString()
				.replace("{table}", memberTable()).replace("{orders}", orders);
		return dbAccess().update().statement(cmd)
				.param("site-id", siteID()).param("group-type", groupType()).param("group-id", groupID)
				.exec();
	}

	protected int doReorder(String groupID, String... memberIDs) {
		return doReorder(groupID, Member.toList(memberType(), memberIDs));
	}
}