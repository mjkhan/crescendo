package crescendo.web.handler;

import horizon.data.FieldValues;
import crescendo.bean.account.AccountManager;
import crescendo.system.Account;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.Session;
import crescendo.web.CrescendoHandler;

public class AccountHandler extends CrescendoHandler {
	public static final String name() {
		return Account.OBJS;
	}

	public AccountHandler() {}

	public AccountHandler(CrescendoHandler other) {
		super(other);
	}
	@Override
	public String getName() {
		return name();
	}
	@Override
	protected AccountManager manager() throws Exception {
		return AccountManager.local();
	}
	@Override
	public Result doGet() throws Exception {
		if (param.exists(Account.SEARCH)) return search(null);
		if (param.exists(Account.GET)) return getInfo(null);
		if (param.exists(Account.VIEW)) return viewInfo(null);
		if (param.exists(Account.NEW)) return newInfo(null);
		if (param.exists(Account.VALIDATE)) return validate(null);
		if (param.exists("site")) return getSite(null);
		if (param.exists(Account.RELOAD)) return reload(null);
		return doDefault("GET", this::search);
	}
	@Override
	public Result doPut() throws Exception {
		return create(null);
	}
	@Override
	public Result doPost() throws Exception {
		if (param.exists(Account.CREATE)) return create(null);
		if (param.exists(Account.UPDATE)) return update(null);
		if (param.exists(Account.LOGIN)) return login(null);
		if (param.exists(Account.LOGOUT)) return logout();
		if (param.exists("active") || param.exists("inactive")) return setStatus();
		if (param.exists(Account.REMOVE)) return remove(null);
		return doDefault("POST");
	}
	@Override
	public Result doDelete() throws Exception {
		return remove(null);
	}

	public Result search(Request req) throws Exception {
		Integer fetch = Integer.valueOf(param.getValue("fetch", () -> ifEmpty(initParam("default-fetch"), "0")));
		param.request().setAttribute("fetch", fetch);
		return respond(manager().search(
				ifEmpty(req, () -> request()
			   .set(Account.TERMS, param.getValue("terms"))
			   .set(Account.FETCH, fetch)
			   .set(Account.START, Integer.valueOf(param.getValue("start", () -> "0")))
		)));
	}

	public Result getInfo(Request req) throws Exception {
		return respond(manager().getInfo(ifEmpty(req, () -> request().set(Account.ID, param.getValue("get")))));
	}

	public Result viewInfo(Request req) throws Exception {
		return respond(manager().viewInfo(ifEmpty(req, () -> request().set(Account.ID, param.getValue("view")))));
	}

	public Result newInfo(Request req) throws Exception {
		return respond(manager().newInfo(ifEmpty(req, () -> request())));
	}

	public Result validate(Request req) throws Exception {
		return respond(manager().validate(
				ifEmpty(req, () -> request()
			   .set(Account.Validator.NAME, param.getValue(Account.VALIDATE))
			   .set("value", param.getValue("value"))
			   .set(Account.Validator.ARGS, fieldGatherer().getInfo(Account.Validator.ARGS))
		)));
	}

	protected Response save(Request req, FieldValues.State state) throws Exception {
		if (req.get(Account.INFO) == null)
			req.set(Account.INFO, fieldGatherer().getInfo(Account.INFO).setState(state));
		if (req.get(Account.ATTR) == null)
			req.set(Account.ATTR, fieldGatherer().getInfo(Account.ATTR));
		return manager().save(req);
	}

	public Result create(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.CREATED));
	}

	public Result login(Request req) throws Exception {
		return respond(manager().login(ifEmpty(req, () -> request()
				.set(Account.ID, param.getValue(Account.LOGIN))
				.set("password", param.getValue("pwd"))
				.set(Session.PERMANENT, "true".equals(param.getValue("remember")))
		)));
	}

	public Result logout() throws Exception {
		return respond(manager().logout(request().set(Account.ID, param.getValue(Account.LOGOUT))));
	}

	public Result update(Request req) throws Exception {
		return respond(save(ifEmpty(req, () -> request()), FieldValues.State.MODIFIED));
	}

	public Result setStatus() throws Exception {
		Account.Status status = null;
		String accountID = param.getValue("active");
		if (!isEmpty(accountID))
			status = Account.Status.ACTIVE;
		else {
			accountID = param.getValue("inactive");
			status = Account.Status.INACTIVE;
		}
		return respond(manager().setStatus(request().set(Account.ID, accountID).set(Account.STATUS, status)));
	}

	public Result remove(Request req) throws Exception {
		return respond(manager().setStatus(ifEmpty(req, () -> request().set(Account.ID, param.getValue(Account.REMOVE)).set(Account.STATUS, Account.Status.REMOVED))));
	}

	public Result reload(Request req) throws Exception {
		return respond(manager().reload(ifEmpty(req, () -> request().set(Account.ID, param.getValue(Account.RELOAD)))));
	}
}