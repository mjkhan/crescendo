package crescendo.bean.menu;

import crescendo.bean.CrescendoManagerBean;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.menu.Menu;
import crescendo.system.menu.MenuServant;

public class MenuManagerBean extends CrescendoManagerBean implements MenuManager {
	@Override
	protected MenuManager getSibling(String sitespace) throws Exception {
		return MenuManager.remote(sitespace);
	}

	private final Request.Action search = (req, sctx, resp) -> resp.setAll(MenuServant.create(sctx, req).search(req));
	@Override
	public Response search(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, search));
	}

	private final Request.Action getInfo = (req, sctx, resp) -> resp.setAll(MenuServant.create(sctx, req).getInfo(req));
	@Override
	public Response getInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, getInfo));
	}

	private final Request.Action newInfo = (req, sctx, resp) -> resp.setAll(MenuServant.create(sctx, req).newInfo(req));
	@Override
	public Response newInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, newInfo));
	}

	private final Request.Action validate = (req, sctx, resp) -> MenuServant.create(sctx, req).validate(req).setTo(resp);
	@Override
	public Response validate(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, validate));
	}

	private final Request.Action save = (req, sctx, resp) -> resp.set(MenuServant.create(sctx, req).save(req));
	@Override
	public Response save(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, save));
	}

	private final Request.Action move = (req, sctx, resp) -> resp.set(MenuServant.create(sctx, req).move(req));
	@Override
	public Response move(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, move));
	}

	private final Request.Action reorder = (req, sctx, resp) -> resp.set(MenuServant.create(sctx, req).reorder(req));
	@Override
	public Response reorder(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, reorder));
	}

	private final Request.Action setStatus = (req, sctx, resp) -> resp.set(MenuServant.create(sctx, req).setStatus(req));
	@Override
	public Response setStatus(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, setStatus));
	}
	@Override
	public Response remove(Request req) {
		return setStatus(req.set(Menu.STATUS, Menu.Status.REMOVED));
	}
}