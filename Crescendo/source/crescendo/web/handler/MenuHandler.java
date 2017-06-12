package crescendo.web.handler;

import horizon.data.FieldValues;
import crescendo.bean.menu.MenuManager;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.menu.Menu;
import crescendo.web.CrescendoHandler;

public class MenuHandler extends CrescendoHandler {
	public static final String name() {
		return Menu.OBJS;
	}

	public MenuHandler() {}

	public MenuHandler(CrescendoHandler other) {
		super(other);
	}
	@Override
	public String getName() {
		return name();
	}
	@Override
	protected MenuManager manager() throws Exception {
		return MenuManager.local();
	}
	@Override
	protected Request newRequest() {
		return super.newRequest()
			.set(Feature.ID, param.notEmpty(Feature.ID))
			.set("owner-id", param.getValue("owner-id"));
	}
	@Override
	public Result doGet() throws Exception {
		if (param.exists(Menu.GET)) return getInfo(null);
		if (param.exists(Menu.NEW)) return newInfo(null);
		if (param.exists(Menu.VALIDATE)) return validate(null);
		return doDefault("GET");
	}
	@Override
	public Result doPut() throws Exception {
		return create(null);
	}
	@Override
	public Result doPost() throws Exception {
		if (param.exists(Menu.CREATE)) return create(null);
		if (param.exists(Menu.UPDATE)) return update(null);
		if (param.exists(Menu.MOVE)) return move(null);
		if (param.exists(Menu.REORDER)) return reorder(null);
		if (param.exists("active") || param.exists("inactive")) return setStatus(null);
		return doDefault("POST");
	}

	public Result getInfo(Request req) throws Exception {
		return respond(manager().getInfo(ifEmpty(req, () -> request().set(Menu.ID, param.notEmpty(Menu.GET)))));
	}

	public Result newInfo(Request req) throws Exception {
		return respond(manager().newInfo(request()));
	}

	public Result validate(Request req) throws Exception {
		return respond(manager().validate(ifEmpty(req, () ->
		request()
	   .set(Menu.Validator.NAME, param.getValue(Menu.VALIDATE))
	   .set(Menu.FIELD_VALUE, param.getValue("value"))
	   .set(Menu.Validator.ARGS, fieldGatherer().getInfo(Menu.Validator.ARGS))
	)));
	}

	public Response save(Request req, FieldValues.State state) throws Exception {
		if (req.get(Menu.INFO) == null)
			req.set(Menu.INFO, fieldGatherer().getInfo(Menu.INFO).setState(state));
		return manager().save(req);
	}

	public Result create(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.CREATED));
	}

	public Result update(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.MODIFIED));
	}

	public Result move(Request req) throws Exception {
		String[] menuIDs = param.notEmpty("move").split(",");
		String dest = param.notEmpty("to");
		return respond(manager().move(request().set(Menu.ID, menuIDs).set("dest", dest)));
	}

	public Result reorder(Request req) throws Exception {
		String[] menuIDs = param.notEmpty("reorder").split(",");
		return respond(manager().reorder(request().set(Menu.ID, menuIDs).set("offset", Integer.valueOf(param.getValue("offset", () -> "0"))))
		);
	}

	public Result setStatus(Request req) throws Exception {
		Menu.Status status = null;
		String menuID = param.getValue("active");
		if (!isEmpty(menuID))
			status = Menu.Status.ACTIVE;
		else {
			menuID = param.getValue("inactive");
			status = Menu.Status.INACTIVE;
		}
		return respond(manager().setStatus(request().set(Menu.ID, menuID).set(Menu.STATUS, status)));
	}
}