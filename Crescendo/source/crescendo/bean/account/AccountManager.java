package crescendo.bean.account;

import crescendo.bean.CrescendoManager;
import crescendo.system.Request;
import crescendo.system.Response;

public interface AccountManager extends CrescendoManager {
	public static AccountManager bean() {
		return new AccountManagerBean();
	}

	public static AccountManager local() {
		return Home.local(AccountManager.class);
	}

	public static AccountManager remote(String contextName) {
		return Home.remote(contextName, AccountManager.class);
	}

	public Response search(Request req);

	public Response getInfo(Request req);

	public Response viewInfo(Request req);

	public Response newInfo(Request req);

	public Response validate(Request req);

	public Response save(Request req);

	public Response login(Request req);

	public Response reload(Request req);

	public Response logout(Request req);

	public Response setStatus(Request req);

	public static interface Remote extends AccountManager {}
}