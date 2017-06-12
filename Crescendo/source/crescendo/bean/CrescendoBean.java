package crescendo.bean;

import horizon.database.DBAccess;
import horizon.system.AbstractObject;
import crescendo.system.AccountContextPool;
import crescendo.system.CrescendoException;
import crescendo.system.Request;
import crescendo.system.SiteContext;
import crescendo.system.SiteContextPool;

public abstract class CrescendoBean extends AbstractObject {
	protected DBAccess adminAccess() {
		return SiteContext.adminAccess(Request.get().isReadWrite());
	}

	protected AccountContextPool accountContexts() {
		return AccountContextPool.get();
	}

	protected SiteContextPool siteContexts() {
		return SiteContextPool.get();
	}

	protected CrescendoException exception(Throwable t) {
		DBAccess.close(false);
		Request req = Request.get(false);
		if (req != null)
			req.release();
		CrescendoException e = CrescendoException.create(t);
		if (!e.isIdentified())
			throw e;
		return e;
	}
}