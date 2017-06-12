package crescendo.system;

import java.util.Collection;

import horizon.system.AbstractObject;

public class AccountContextPool extends AbstractObject {
	private static ObjectCache<AccountContext> cache;
	private static final AccountContextPool pool = new AccountContextPool();

	public static final AccountContextPool get() {
		return pool;
	}

	public static final void configure(Feature feature) {
		Feature config = ifEmpty(feature.feature("account-context-cache"), () -> Feature.load("crescendo/cache/default-cache.xml"));
		cache = ObjectCache.create(config);
	}

	private AccountContextPool() {}

	public AccountContext get(String accountID) {
		return isEmpty(accountID) ? AccountContext.UNKNOWN : cache.get(accountID);
	}

	private AccountContext context(String accountID, Client client) {
		AccountContext actx = isEmpty(accountID) ? AccountContext.UNKNOWN : cache.get(accountID);
		if (actx == null) {
			Client.Profile cprof = client.profile();
			Feature aftr = cprof.feature(Account.OBJ);
			actx = load(accountID, aftr);
			if (!actx.isUnknown()) {
				cache.set(accountID, actx);
				actx.load();
			}
		}
		if (client != null)
			client.set(actx);
		return actx;
	}

	public AccountContext context(Request req) {
		Client client = Client.get(req);
		Session session = Session.get(client);
		if (session != null) {
			if (session.isExpired()) {
				session.timeout();
				Session.removeFrom(client);
			} else
				session.updateAccessTime();
		}
		String accountID = session != null && !session.isExpired() ? session.id() : null;
		return context(accountID, client);
	}

	private synchronized AccountContext load(String accountID, Feature feature) {
		AccountContext actx = cache.get(accountID);
		if (actx != null) return actx;

		Account account = Account.Provider.Factory.create(feature).getAccount(accountID);
		return account != null ?
			   new AccountContext().set(account) :
			   AccountContext.UNKNOWN;
	}

	public void remove(String accountID) {
		if (isEmpty(accountID)) return;

		AccountContext actx = cache.remove(accountID);
		if (actx != null)
			actx.unload();
	}

	public void propagate(Collection<? extends Event> evts) {
		if (isEmpty(evts)) return;

		if (cache.updatesViaJMS())
			Event.send(evts);
		else
			evts.forEach(evt -> {
				if (evt instanceof AccountContextEvent)
					propagate(AccountContextEvent.class.cast(evt));
				else if (evt instanceof SiteContextEvent)
					propagate(SiteContextEvent.class.cast(evt));
			});
	}

	private void propagate(AccountContextEvent evt) {
		String accountID = evt.accountID();
		AccountContext actx = cache.get(accountID);
		if (actx == null) return;

		evt.update(actx);
		cache.update(accountID, actx);
	}

	private void propagate(SiteContextEvent evt) {
		for (String accountID: evt.accountIDs()) {
			AccountContext actx = cache.get(accountID);
			if (actx == null) continue;

			evt.update(actx);
			cache.update(accountID, actx);
		}
	}
}