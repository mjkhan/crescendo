package crescendo.bean.account;

import crescendo.bean.CrescendoManagerBean;
import crescendo.system.Account;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.account.AccountServant;
import crescendo.system.account.LoginServant;

public class AccountManagerBean extends CrescendoManagerBean implements AccountManager {
	@Override
	protected AccountManager getSibling(String sitespace) throws Exception {
		return AccountManager.remote(sitespace);
	}

	protected static AccountServant accountServant() {
		return AccountServant.create((Feature)null);
	}

	private final Request.Action search = (req, sctx, resp) -> resp.setAll(accountServant().search(req));
	@Override
	public Response search(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, search));
	}

	private final Request.Action getInfo = (req, sctx, resp) -> resp.setAll(accountServant().getInfo(req));
	@Override
	public Response getInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, getInfo));
	}

	private final Request.Action viewInfo = (req, sctx, resp) -> resp.setAll(accountServant().viewInfo(req));
	@Override
	public Response viewInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, viewInfo));
	}

	private final Request.Action newInfo = (req, sctx, resp) -> resp.setAll(accountServant().newInfo(req));
	@Override
	public Response newInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, newInfo));
	}

	private final Request.Action validate = (req, sctx, resp) -> accountServant().validate(req).setTo(resp);
	@Override
	public Response validate(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_ADMIN, validate));
	}

	private final Request.Action save = (req, sctx, resp) -> resp.set(accountServant().save(req));
	@Override
	public Response save(Request req) {
		return writeAdmin(req, save);
	}

	private final Request.Action login = (req, sctx, resp) -> resp.set(LoginServant.create(sctx).login(req));
	@Override
	public Response login(Request req) {
		return writeAdmin(req, login);
	}

	private final Request.Action reload = (req, sctx, resp) -> accountContexts().remove(req.notEmpty(Account.ID));
	@Override
	public Response reload(Request req) {
		return writeAdmin(req, reload);
	}

	private final Request.Action logout = (req, sctx, resp) -> resp.set(LoginServant.create(sctx).logout(req));
	@Override
	public Response logout(Request req) {
		return writeAdmin(req, logout);
	}

	private final Request.Action setStatus = (req, sctx, resp) -> resp.set(accountServant().setStatus(req));
	@Override
	public Response setStatus(Request req) {
		return writeAdmin(req, setStatus);
	}
}