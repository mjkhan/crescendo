package crescendo.system.site;

import horizon.data.FieldAware;
import crescendo.system.AccountContext;
import crescendo.system.PermissionInspector;
import crescendo.system.Principal;
import crescendo.system.Request;
import crescendo.system.Site;

public class SitePermission extends PermissionInspector {
	@Override
	public boolean grants(Request req) {
		return adminPermits(req);
	}
	@Override
	protected boolean permits(Request req) {
		switch (req.action()) {
		case Site.SEARCH: return search(req);
		case Site.GET: return getInfo(req);
		case Site.VIEW: return viewInfo(req);
		case Site.CREATE: return createSite(req);
		case Site.UPDATE: return updateSite(req);
		case Site.REMOVE: return removeSite(req);
		case Site.CHANGE_STATUS: return setStatus(req);
		default: return false;
		}
	}

	protected boolean search(Request req) {
		return true;
	}

	protected boolean isSiteOwner(Request req) {
		return isSelf(req, () -> {
			Object ownerID = null;
			FieldAware fieldAware = (FieldAware)req.get(Site.INFO);
			if (fieldAware != null)
				ownerID = fieldAware.getValue("owner_id");
			if (isEmpty(ownerID)) {
				Site site = (Site)req.get(Site.OBJ);
				if (site != null)
					ownerID = site.getOwnerID();
			}
			return (String)ownerID;
		});
	}

	protected boolean getInfo(Request req) {
		return isSiteOwner(req);
	}

	protected boolean viewInfo(Request req) {
		return true;
	}

	protected boolean createSite(Request req) {
		AccountContext actx = accountContext(req);
		return !Principal.Generic.EVERYONE.equals(Principal.Generic.get(actx));
	}

	protected boolean updateSite(Request req) {
		return isSiteOwner(req);
	}

	protected boolean removeSite(Request req) {
		return isSiteOwner(req);
	}

	protected boolean setStatus(Request req) {
		return isSiteOwner(req);
	}
}