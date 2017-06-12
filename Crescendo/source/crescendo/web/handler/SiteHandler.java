package crescendo.web.handler;

import horizon.data.FieldValues;
import crescendo.bean.site.SiteManager;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.Site;
import crescendo.web.CrescendoHandler;

public class SiteHandler extends CrescendoHandler {
	public static final String name() {
		return Site.OBJS;
	}

	public SiteHandler() {}

	public SiteHandler(CrescendoHandler other) {
		super(other);
	}
	@Override
	public String getName() {
		return name();
	}
	@Override
	protected SiteManager manager() throws Exception {
		return SiteManager.local();
	}
	@Override
	public Result doGet() throws Exception {
		if (param.exists(Site.SEARCH)) return search(null);
		if (param.exists(Site.GET)) return getInfo(null);
		if (param.exists(Site.VIEW)) return viewInfo(null);
		if (param.exists(Site.NEW)) return newInfo();
		if (param.exists(Site.VALIDATE)) return validate(null);
		if (param.exists(Site.RELOAD)) return reload(null);
		return doDefault("GET", this::search);
	}
	@Override
	public Result doPut() throws Exception {
		return create(null);
	}
	@Override
	public Result doPost() throws Exception {
		if (param.exists(Site.CREATE)) return create(null);
		if (param.exists(Site.UPDATE)) return update(null);
		if (param.exists(Site.SETUP)) return setup(null);
		if (param.exists("active") || param.exists("inactive")) return setStatus(null);
		if (param.exists("remove")) return remove(null);
		return doDefault("POST");
	}
	@Override
	public Result doDelete() throws Exception {
		return remove(null);
	}

	protected String siteID(String name) {
		return param.getValue(name, this::thisSite);
	}

	public Result search(Request req) throws Exception {
		Integer fetch = Integer.valueOf(param.getValue("fetch", () -> ifEmpty(initParam("default-fetch"), "0")));
		param.request().setAttribute("fetch", fetch);
		return respond(manager().search(ifEmpty(req, () ->
				request()
			   .set(Site.TERMS, param.getValue("terms"))
			   .set(Site.FETCH, fetch)
			   .set(Site.START, Integer.valueOf(param.getValue("start", () -> "0")))
		)));
	}

	public Result getInfo(Request req) throws Exception {
		return respond(manager().getInfo(ifEmpty(req, () -> request().set(Site.ID, siteID(Site.GET)))));
	}

	public Result viewInfo(Request req) throws Exception {
		return respond(manager().viewInfo(ifEmpty(req, request().set(Site.ID, siteID(Site.VIEW)))));
	}

	public Result newInfo() throws Exception {
		return respond(manager().newInfo(request()));
	}

	public Result validate(Request req) throws Exception {
		return respond(manager().validate(ifEmpty(req, () ->
				request()
			   .set(Site.Validator.NAME, param.getValue(Site.VALIDATE))
			   .set("value", param.getValue("value"))
			   .set(Site.Validator.ARGS, fieldGatherer().getInfo(Site.Validator.ARGS))
		)));
	}

	protected Response save(Request req, FieldValues.State state) throws Exception {
		if (req.get(Site.INFO) == null)
			req.set(Site.INFO, fieldGatherer().getInfo(Site.INFO).setState(state));
		return manager().save(req);
	}

	public Result create(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.CREATED));
	}

	public Result setup(Request req) throws Exception {
		return respond(manager().setup(ifEmpty(req, () -> request().set(Site.ID, siteID(Site.SETUP)))));
	}

	public Result update(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.MODIFIED));
	}

	public Result setStatus(Request req) throws Exception {
		return respond(manager().setStatus(ifEmpty(req, () -> {
			String siteID = null;
			Site.Status status = null;
			if (param.exists("active")) {
				siteID = siteID("active");
				status = Site.Status.ACTIVE;
			} else if (param.exists("inactive")) {
				siteID = siteID("inactive");
				status = Site.Status.INACTIVE;
			}
			return request()
				  .set(Site.ID, siteID)
				  .set(Site.STATUS, status);
		})));
	}

	public Result remove(Request req) throws Exception {
		return respond(manager().setStatus(ifEmpty(req, () -> request().set(Site.ID, siteID(Site.REMOVE))).set(Site.STATUS, Site.Status.REMOVED)));
	}

	public Result reload(Request req) throws Exception {
		return respond(manager().reload(ifEmpty(req, () -> request().set(Site.ID, param.getValue(Site.RELOAD)))));
	}
}