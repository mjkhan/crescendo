package crescendo.bean.group;

import crescendo.bean.CrescendoManagerBean;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.group.Group;
import crescendo.system.group.GroupServant;

public class GroupManagerBean extends CrescendoManagerBean implements GroupManager {
	@Override
	protected GroupManager getSibling(String sitespace) throws Exception {
		return GroupManager.remote(sitespace);
	}

	private final Request.Action search = (req, sctx, resp) -> resp.setAll(GroupServant.create(sctx, req).search(req));
	@Override
	public Response search(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, search));
	}

	private final Request.Action getInfo = (req, sctx, resp) -> resp.setAll(GroupServant.create(sctx, req).getInfo(req));
	@Override
	public Response getInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, getInfo));
	}

	private final Request.Action viewInfo = (req, sctx, resp) -> resp.setAll(GroupServant.create(sctx, req).viewInfo(req));
	@Override
	public Response viewInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, viewInfo));
	}

	private final Request.Action newInfo = (req, sctx, resp) -> resp.setAll(GroupServant.create(sctx, req).newInfo(req));
	@Override
	public Response newInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, newInfo));
	}

	private final Request.Action validate = (req, sctx, resp) -> GroupServant.create(sctx, req).validate(req).setTo(resp);
	@Override
	public Response validate(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, validate));
	}

	private final Request.Action save = (req, sctx, resp) -> resp.set(GroupServant.create(sctx, req).save(req));
	@Override
	public Response save(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, save));
	}

	private final Request.Action move = (req, sctx, resp) -> resp.set(GroupServant.create(sctx, req).move(req));
	@Override
	public Response move(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, move));
	}

	private final Request.Action reorder = (req, sctx, resp) -> resp.set(GroupServant.create(sctx, req).reorder(req));
	@Override
	public Response reorder(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, reorder));
	}

	private final Request.Action setStatus = (req, sctx, resp) -> resp.set(GroupServant.create(sctx, req).setStatus(req));
	@Override
	public Response setStatus(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, setStatus));
	}
	@Override
	public Response remove(Request req) {
		return setStatus(req.set(Group.STATUS, Group.Status.REMOVED));
	}
}