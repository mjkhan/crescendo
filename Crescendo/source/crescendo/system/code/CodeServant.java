package crescendo.system.code;

import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Servant;
import horizon.data.Dataset;

public class CodeServant extends Servant.Generic {
	public static final CodeServant create() {
		return create(null, Feature.load("crescendo/default/common-code.xml"), "code-servant");
	}

	public Dataset search(String condition, Object[] args, String orderBy, int fetch, int start) {
		String table = "crsnd_common_code";
		return sqlDialect("code-dialect").select("search-" + table, null, table, condition, ifEmpty(orderBy, "grp_id, code"), qsupport.toList(args), start, fetch);
	}

	@Override
	public Dataset search(String condition, Object... args) {
		return search(condition, args, null, 0, -1);
	}

	public Dataset search(Request req) {
		String condition = ifEmpty(req.string("code-condition"), ""),
			   order = ifEmpty(req.string("code-order"), "");
		Object[] args = req.objects("code-args");
		int fetch = req.number("code-fetch").intValue(),
			start = req.number("code-start").intValue();
		return search(condition, args, order, fetch, start);
	}

	public Dataset searchGroups(String... groupIDs) {
		String condition = "";
		Object[] args = null;
		switch (groupIDs == null ? 0 : groupIDs.length) {
		case 0: break;
		case 1:
			condition = "grp_id = ?";
			args = groupIDs;
			break;
		default: condition = "grp_id " + qsupport.asIn(groupIDs); break;
		}
		condition += isEmpty(condition) ? "grp_id = code" : " and grp_id = code";//Code group: grp_id = code
		return search(condition, args);
	}

	public Dataset searchCodes(String... groupIDs) {
		Object[] args = null;
		String condition = "";
		switch (groupIDs == null ? 0 : groupIDs.length) {
		case 0: break;
		case 1:
			condition = "grp_id = ?";
			args = groupIDs;
			break;
		default: condition = "grp_id " + qsupport.asIn(groupIDs); break;
		}
		condition += isEmpty(condition) ? "grp_id <> code" : " and grp_id <> code";//Codes: grp_id <> code
		return search(condition, args);
	}

	public Dataset getValues(String groupID, String code) {
		String condition = "grp_id = ? and code = ?";
		Object[] args = {groupID, code};
		return search(condition, args);
	}

	private static final String EXISTS = "select count(*) a_val from crsnd_common_code where grp_id = ? and code = ? and code_val = ?";

	public boolean exists(String groupID, String code, String value) {
		Number number = adminAccess().query("a_val").getValue(EXISTS, groupID, code, value);
		return number.intValue() > 0;
	}
}