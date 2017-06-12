package crescendo.system.account;

import crescendo.system.Account;
import crescendo.system.Client;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.Session;
import crescendo.system.SiteContext;

public class LoginServant extends Servant.Generic {
	private static final String NAME = Account.LOGIN;

	public static final LoginServant create(SiteContext sctx) {
		Class<? extends LoginServant> klass = sctx.profile().klass(NAME);
		return ifEmpty(Feature.instance(klass), LoginServant::new).set(sctx).set(Account.feature(null));
	}

	public AccountEvent login(Request req) {
		Client client = Client.get(req);
		AccountEvent evt = AccountEvent.create(feature);
		evt.setType(AccountEvent.LOGIN);

		if (client.isUnknown() && evt.before()) {
			String accountID = req.notEmpty(Account.ID),
				   password = req.notEmpty("password");
			Account account = AccountServant.create(feature).getAccount(accountID);
			boolean loggedin = account != null;
			String reason = null;

			if (!loggedin)
				reason = "account-not-found";
			else {
				loggedin = account.getPassword().equals(password);
				if (!loggedin)
					reason = "password-mismatch";
				else {
					loggedin = !Account.Status.CREATED.equals(account.getStatus());
					if (!loggedin)
						reason = "invalid-account-status";
				}
			}
			evt.set(Account.LOGGED_IN, Boolean.valueOf(loggedin))
			   .set("reason", reason);

			if (loggedin) {
				evt.set(Account.OBJ, account);
				new Session().setId(account.getId()).updateAccessTime().setExpiration(req.bool(Session.PERMANENT) ? 0 : sctx.profile().number("session-max-idle-time").intValue()).setTo(client);
			}
			adminAccess().transact(dbaccess -> {
				evt.setFired(true).on();
			}, null);
		}
		return evt;
	}

	public AccountEvent logout(Request req) {
		AccountEvent evt = AccountEvent.create(feature);
		evt.setType(AccountEvent.LOGOUT);
		Client client = Client.get(req);
		if (!client.isUnknown() && evt.before()) {
			Session session = Session.removeFrom(client);
			boolean loggedout = session != null;
			evt.setFired(loggedout)
			   .set(Account.OBJ, client.accountContext().account())
			   .on()
			   .setHasUpdates(true);
		}
		return evt;
	}
}