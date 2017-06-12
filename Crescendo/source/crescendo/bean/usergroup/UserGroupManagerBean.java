package crescendo.bean.usergroup;

import crescendo.bean.group.GroupManagerBean;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.account.AccountServant;
import crescendo.system.usergroup.SiteUser;
import crescendo.system.usergroup.SiteUserServant;

public class UserGroupManagerBean extends GroupManagerBean implements UserGroupManager {
	@Override
	protected UserGroupManager getSibling(String sitespace) throws Exception {
		return UserGroupManager.remote(sitespace);
	}

	private final Request.Action searchUser = (req, sctx, resp) -> resp.setAll(SiteUserServant.create(sctx, req).search(req));
	@Override
	public Response searchUser(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, searchUser));
	}

	private final Request.Action getAccounts = (req, sctx, resp) -> {
		String[] accountIDs = req.objects(SiteUser.ID);
		req.set(SiteUser.LIST, AccountServant.create((Feature)null).getInfo(accountIDs));
	};

	private final Request.Action addUser = (req, sctx, resp) -> resp.set(SiteUserServant.create(sctx, req).add(req));
	@Override
	public Response addUser(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.READ_ADMIN, getAccounts).set(Request.Action.WRITE_SITE, addUser));
	}

	private final Request.Action removeUser = (req, sctx, resp) -> resp.set(SiteUserServant.create(sctx, req).remove(req));
	@Override
	public Response removeUser(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, removeUser));
	}

	private final Request.Action setUser = (req, sctx, resp) -> resp.set(SiteUserServant.create(sctx, req).set(req));
	@Override
	public Response setUser(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, setUser));
	}

	private final Request.Action blockUser = (req, sctx, resp) -> resp.set(SiteUserServant.create(sctx, req).block(req));
	@Override
	public Response blockUser(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.READ_ADMIN, getAccounts).set(Request.Action.WRITE_SITE, blockUser));
	}
}