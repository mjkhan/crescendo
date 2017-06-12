package crescendo.bean.site;

import crescendo.bean.CrescendoManager;
import crescendo.system.Request;
import crescendo.system.Response;

public interface SiteManager extends CrescendoManager {
	public static SiteManager bean() {
		return new SiteManagerBean();
	}

	public static SiteManager local() {
		return Home.local(SiteManager.class);
	}

	public static SiteManager remote(String contextName) {
		return Home.remote(contextName, SiteManager.class);
	}

	public Response search(Request req);

	public Response getInfo(Request req);

	public Response viewInfo(Request req);

	public Response newInfo(Request req);

	public Response validate(Request req);

	public Response save(Request req);

	public Response reload(Request req);

	public Response setup(Request req);

	public Response setStatus(Request req);

	public static interface Remote extends SiteManager {}
}