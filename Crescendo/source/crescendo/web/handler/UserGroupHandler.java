package crescendo.web.handler;

import horizon.data.FieldValues;
import crescendo.bean.usergroup.UserGroupManager;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.usergroup.SiteUser;
import crescendo.system.usergroup.UserGroup;
import crescendo.web.CrescendoHandler;

public class UserGroupHandler extends CrescendoHandler {
	public static final String name() {
		return UserGroup.OBJS;
	}
	public UserGroupHandler() {}

	public UserGroupHandler(CrescendoHandler other) {
		super(other);
	}
	@Override
	public String getName() {
		return name();
	}
	@Override
	protected UserGroupManager manager() throws Exception {
		return UserGroupManager.local();
	}
	@Override
	public Result doGet() throws Exception {
		if (param.exists(UserGroup.SEARCH)) return search(null);
		if (param.exists(UserGroup.GET)) return getInfo(null);
		if (param.exists(UserGroup.NEW)) return newInfo();
		if (param.exists(UserGroup.VALIDATE)) return validate(null);
		if (param.exists("users")) return searchUser(null);
		return doDefault("GET");
	}
	@Override
	public Result doPut() throws Exception {
		return create(null);
	}
	@Override
	public Result doPost() throws Exception {
		if (param.exists(UserGroup.CREATE)) return create(null);
		if (param.exists(UserGroup.UPDATE)) return update(null);
		if (param.exists(UserGroup.REMOVE)) return remove(null);
		if (param.exists("add-user")) return addUser(null);
		if (param.exists("set-user")) return setUser(null);
		if (param.exists("remove-user")) return removeUser(null);
		return doDefault("POST");
	}
	@Override
	public Result doDelete() throws Exception {
		return remove(null);
	}

	public Result search(Request req) throws Exception {
		Integer fetch = Integer.valueOf(param.getValue("fetch", () -> ifEmpty(initParam("default-fetch"), "0")));
		param.request().setAttribute("fetch", fetch);
		return respond(manager().search(ifEmpty(req, () ->
			request()
		   .set(UserGroup.TERMS, param.getValue("terms"))
		   .set(UserGroup.FETCH, fetch)
		   .set(UserGroup.START, Integer.valueOf(param.getValue("start", () -> "0")))
		)));
	}

	public Result getInfo(Request req) throws Exception {
		return respond(manager().getInfo(ifEmpty(req, () -> request().set(UserGroup.ID, notEmpty(param.getValue(UserGroup.GET), UserGroup.GET)))));
	}

	public Result newInfo() throws Exception {
		return respond(manager().newInfo(request()));
	}

	public Result validate(Request req) throws Exception {
		return respond(manager().validate(ifEmpty(req, () ->
			request()
		   .set(UserGroup.Validator.NAME, param.getValue(UserGroup.VALIDATE))
		   .set(UserGroup.FIELD_VALUE, param.getValue("value"))
		   .set(UserGroup.Validator.ARGS, fieldGatherer().getInfo(UserGroup.Validator.ARGS))
		)));
	}

	public Response save(Request req, FieldValues.State state) throws Exception {
		if (req.get(UserGroup.INFO) == null)
			req.set(UserGroup.INFO, fieldGatherer().getInfo(UserGroup.INFO).setState(state));
		return manager().save(req);
	}

	public Result create(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.CREATED));
	}

	public Result update(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.MODIFIED));
	}

	public Result remove(Request req) throws Exception {
		return respond(manager().remove(ifEmpty(req, () -> {
			Object[] groupIDs = notEmpty(param.getValue(UserGroup.REMOVE), UserGroup.REMOVE).split(",");
			return request().set(UserGroup.ID, groupIDs);
		})));
	}

	public Result searchUser(Request req) throws Exception {
		Integer fetch = Integer.valueOf(param.getValue("fetch", () -> ifEmpty(initParam("default-fetch"), "0")));
		param.request().setAttribute("fetch", fetch);
		return respond(manager().searchUser(ifEmpty(req, () ->
			request()
		   .set(SiteUser.TERMS, param.getValue("terms"))
		   .set(SiteUser.FETCH, fetch)
		   .set(SiteUser.START, Integer.valueOf(param.getValue("start", () -> "0")))
		)));
	}

	public Result addUser(Request req) throws Exception {
		return respond(manager().addUser(ifEmpty(req, () -> request())));
	}

	public Result setUser(Request req) throws Exception {
		return respond(manager().setUser(ifEmpty(req, () -> request())));
	}

	public Result removeUser(Request req) throws Exception {
		return respond(manager().removeUser(ifEmpty(req, () -> request())));
	}
}