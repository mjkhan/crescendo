package crescendo.bean.site;

import horizon.database.DBAccess;
import crescendo.bean.CrescendoManagerBean;
import crescendo.system.AccountContext;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.Site;
import crescendo.system.SiteContext;
import crescendo.system.site.SiteServant;

public class SiteManagerBean extends CrescendoManagerBean implements SiteManager {
	@Override
	protected SiteManager getSibling(String sitespace) throws Exception {
		return SiteManager.remote(sitespace);
	}

	protected SiteServant siteServant() {
		return SiteServant.create((Feature)null);
	}

	private final Request.Action search = (req, sctx, resp) -> resp.setAll(siteServant().search(req));
	@Override
	public Response search(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, search));
	}

	private final Request.Action getInfo = (req, sctx, resp) -> resp.setAll(siteServant().getInfo(req));
	@Override
	public Response getInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, getInfo));
	}

	private final Request.Action viewInfo = (req, sctx, resp) -> resp.setAll(siteServant().viewInfo(req));
	@Override
	public Response viewInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, viewInfo));
	}

	private final Request.Action newInfo = (req, sctx, resp) -> resp.setAll(siteServant().newInfo(req));
	@Override
	public Response newInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, newInfo));
	}

	private final Request.Action validate = (req, sctx, resp) -> siteServant().validate(req).setTo(resp);
	@Override
	public Response validate(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, validate));
	}

	private final Request.Action save = (req, sctx, resp) -> resp.set(siteServant().save(req));
	@Override
	public Response save(Request req) {
		return writeAdmin(req, save);
	}
	private final Request.Action reload = (req, sctx, resp) -> siteContexts().remove(req.notEmpty(Site.ID));
	@Override
	public Response reload(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, reload));
	}
	@Override
	public Response setup(Request req) {
		req.setCurrent().setReadWrite(true);
		Response resp = new Response();
		try {
			DBAccess dbaccess = adminAccess();
			dbaccess.open();

			AccountContext actx = acontext(req);
			SiteContext current = scontext(req);
			SiteServant servant = siteServant();
			Site site = servant.getSite(req.string(Site.ID));
			if (site == null)
				throw SiteContext.NOT_FOUND;

			dbaccess = swap(dbaccess, DBAccess.get(site.dbConnection()));
			String sitespace = site.sitespace();
			if (equals(sitespace, req.sitespace())) {
				resp.set(Site.SETUP, Boolean.valueOf(servant.setup(site)));
				dbaccess = swap(dbaccess, current.dbAccess());
				current.export(resp);
				actx.enter(current);
			} else {
				resp = getSibling(sitespace).setup(req.setSitespace(sitespace));
			}

			boolean setup = resp.bool(Site.SETUP);
			if (setup) {
				dbaccess = swap(dbaccess, adminAccess());
				site.setStatus(Site.Status.ACTIVE);
				servant.update(site);
				resp.set(Site.OBJ, site);
			}
			dbaccess.close();

			req.export(resp).release();
			return resp;
		} catch (Exception e) {
			return resp.set(exception(e));
		}
	}

	private final Request.Action setStatus = (req, sctx, resp) -> resp.set(siteServant().setStatus(req));
	@Override
	public Response setStatus(Request req) {
		return writeAdmin(req, setStatus);
	}
}