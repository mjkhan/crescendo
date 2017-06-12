package crescendo.system.usergroup;

import crescendo.system.Request;
import crescendo.system.group.GroupPermission;

public class UserGroupPermission extends GroupPermission {
	@Override
	protected boolean permits(Request req) {
		switch (req.action()) {
		case SiteUser.SEARCH: return searchUser(req);
		case SiteUser.ADD: return addUser(req);
		case SiteUser.REMOVE: return removeUser(req);
		case SiteUser.SET: return setUser(req);
		case SiteUser.BLOCK: return blockUser(req);
		default: return super.permits(req);
		}
	}

	protected boolean searchUser(Request req) {
		return true;
	}

	protected boolean addUser(Request req) {
		return false;
	}

	protected boolean setUser(Request req) {
		return false;
	}

	protected boolean removeUser(Request req) {
		return false;
	}

	protected boolean blockUser(Request req) {
		return false;
	}
}