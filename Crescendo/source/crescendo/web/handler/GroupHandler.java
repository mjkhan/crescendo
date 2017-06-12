package crescendo.web.handler;

import crescendo.bean.group.GroupManager;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.group.Group;
import crescendo.web.CrescendoHandler;
import horizon.data.FieldValues;

public class GroupHandler extends CrescendoHandler {
	public static final String name() {
		return Group.OBJS;
	}

	public GroupHandler() {}

	public GroupHandler(CrescendoHandler other) {
		super(other);
	}
	@Override
	public String getName() {
		return name();
	}
	@Override
	protected GroupManager manager() throws Exception {
		return GroupManager.local();
	}
	@Override
	protected Request newRequest() {
		return super.newRequest()
			.set(Feature.ID, param.notEmpty(Feature.ID))
			.set("owner-id", param.getValue("owner-id"));
	}
	@Override
	public Result doGet() throws Exception {
		if (param.exists(Group.SEARCH)) return search(null);
		if (param.exists(Group.GET)) return getInfo(null);
		if (param.exists(Group.VIEW)) return viewInfo(null);
		if (param.exists(Group.NEW)) return newInfo(null);
		if (param.exists(Group.VALIDATE)) return validate(null);
		return doDefault("GET", this::search);
	}
	@Override
	public Result doPut() throws Exception {
		return create(null);
	}
	@Override
	public Result doPost() throws Exception {
		if (param.exists(Group.CREATE)) return create(null);
		if (param.exists(Group.UPDATE)) return update(null);
		if (param.exists(Group.MOVE)) return move(null);
		if (param.exists(Group.REORDER)) return reorder(null);
		if (param.exists("active") || param.exists("inactive")) return setStatus(null);
		return doDefault("POST");
	}

	public Result search(Request req) throws Exception {
		Integer fetch = Integer.valueOf(param.getValue("fetch", () -> ifEmpty(initParam("default-fetch"), "0")));
		param.request().setAttribute("fetch", fetch);
		return respond(manager().search(ifEmpty(req, () ->
			request()
		   .set(Group.TERMS, param.getValue("terms"))
		   .set(Group.FETCH, fetch)
		   .set(Group.START, Integer.valueOf(param.getValue("start", () -> "0")))
		)));
	}

	public Result getInfo(Request req) throws Exception {
		return respond(manager().getInfo(ifEmpty(req, () -> request().set(Group.ID, param.notEmpty(Group.GET)))));
	}

	public Result newInfo(Request req) throws Exception {
		return respond(manager().newInfo(request()));
	}

	public Result viewInfo(Request req) throws Exception {
		return respond(manager().viewInfo(ifEmpty(req, () -> request().set(Group.ID, param.notEmpty(Group.VIEW)))));
	}

	public Result validate(Request req) throws Exception {
		return respond(manager().validate(ifEmpty(req, () ->
		request()
	   .set(Group.Validator.NAME, param.getValue(Group.VALIDATE))
	   .set(Group.FIELD_VALUE, param.getValue("value"))
	   .set(Group.Validator.ARGS, fieldGatherer().getInfo(Group.Validator.ARGS))
	)));
	}

	public Response save(Request req, FieldValues.State state) throws Exception {
		if (req.get(Group.INFO) == null)
			req.set(Group.INFO, fieldGatherer().getInfo(Group.INFO).setState(state));
		return manager().save(req);
	}

	public Result create(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.CREATED));
	}

	public Result update(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.MODIFIED));
	}

	public Result move(Request req) throws Exception {
		String[] groupIDs = param.notEmpty("move").split(",");
		String dest = param.notEmpty("to");
		return respond(manager().move(request().set(Group.ID, groupIDs).set("dest", dest)));
	}

	public Result reorder(Request req) throws Exception {
		String[] groupIDs = param.notEmpty("reorder").split(",");
		return respond(manager().reorder(request().set(Group.ID, groupIDs).set("offset", Integer.valueOf(param.getValue("offset", () -> "0"))))
		);
	}

	public Result setStatus(Request req) throws Exception {
		Group.Status status = null;
		String groupID = param.getValue("active");
		if (!isEmpty(groupID))
			status = Group.Status.ACTIVE;
		else {
			groupID = param.getValue("inactive");
			status = Group.Status.INACTIVE;
		}
		return respond(manager().setStatus(request().set(Group.ID, groupID).set(Group.STATUS, status)));
	}
}