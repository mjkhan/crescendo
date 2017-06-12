package crescendo.system.menu;

import crescendo.system.PermissionInspector;
import crescendo.system.Request;

public class MenuPermission extends PermissionInspector {
	@Override
	protected boolean permits(Request req) {
		switch (req.action()) {
		case Menu.SEARCH: return search(req);
		case Menu.GET: return getInfo(req);
		case Menu.CREATE: return create(req);
		case Menu.UPDATE: return update(req);
		case Menu.MOVE: return move(req);
		case Menu.REORDER: return reorder(req);
		case Menu.REMOVE: return remove(req);
		case Menu.CHANGE_STATUS: return setStatus(req);
		default: return false;
		}
	}

	protected boolean search(Request req) {
		return false;
	}

	protected boolean getInfo(Request req) {
		return false;
	}

	protected boolean create(Request req) {
		return false;
	}

	protected boolean update(Request req) {
		return false;
	}

	protected boolean move(Request req) {
		return false;
	}

	protected boolean reorder(Request req) {
		return false;
	}

	protected boolean remove(Request req) {
		return false;
	}

	protected boolean setStatus(Request req) {
		return false;
	}
}