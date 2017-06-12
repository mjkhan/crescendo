package crescendo.system;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import crescendo.system.cache.LRUCache;
import horizon.system.Log;

public class AccountContext extends NamedObjects {
	private static final long serialVersionUID = 1L;
	static final String NAME = "account-context";
	public static final AccountContext UNKNOWN = new AccountContext().set(new Account());

	private ObjectsInSites objsInSites;

	public static final AccountContext get() {
		Client client = Client.get(false);
		return !isEmpty(client) ? client.accountContext() : UNKNOWN;
	}
	@Override
	public AccountContext set(String name, Object obj) {
		super.set(name, obj);
		if (obj != null)
			log().debug(() -> this + "[\"" + name + "\"] = " + obj);
		return this;
	}

	public Account account() {
		return Account.class.cast(get(Account.OBJ));
	}

	public AccountContext set(Account account) {
		return set(Account.OBJ, account);
	}
	@Override
	public Object remove(Object key) {
		Object removed = super.remove(key);
		if (removed != null)
			log().debug(() -> this + "[\"" + key + "\"] removed.");
		return removed;
	}

	public boolean isUnknown() {
		Account account = account();
		return account == null || account.isUnknown();
	}

	public AccountContext ensureKnownPrincipal() {
		if (isUnknown())
			throw PermissionInspector.deny;
		return this;
	}

	public ObjectsInSites siteObjects() {
		if (objsInSites == null) {
			objsInSites = new ObjectsInSites();
			objsInSites.actx = this;
		}
		return objsInSites;
	}

	private void fireAccountContextEvent(Consumer<AccountContextEvent> consumer) {
		AccountContextEvent.fire(Client.Profile.get(), AccountContextEvent.class, consumer);
	}

	public void load() {
		if (Account.Status.CREATED.equals(account().getStatus())) return;

		fireAccountContextEvent(evt -> evt.onLoad(this));
	}

	public boolean enter(SiteContext sctx) {
		if (isUnknown()) return false;

		String siteID = sctx.site().getId();
		NamedObjects objs = siteObjects().get(siteID);
		if (objs != null) return false;

		log().debug(() -> account() + " enters " + sctx.site() + ".");
		objsInSites.put(siteID, NamedObjects.EMPTY);
		sctx.profile().fireContextEvent(evt -> evt.onEnter(this, sctx));
		return true;
	}

	public void unload() {
		if (Account.Status.CREATED.equals(account().getStatus())) return;

		fireAccountContextEvent(evt -> evt.onUnload(this));
	}

	public Collection<Principal> getPrincipals(SiteContext sctx) {
		Principal generic = Principal.Generic.get(this);
		LinkedHashSet<Principal> result = new LinkedHashSet<>();
		result.add(generic);
		if (Principal.Generic.EVERYONE.equals(generic) || Principal.Generic.SYS_ADMIN.equals(generic)) return result;

		if (sctx.site().getOwnerID().equals(account().getId())) {
			result.remove(Principal.Generic.LOGGED_IN);
			result.add(Principal.Generic.SITE_OWNER);
			return result;
		}

		ArrayList<String> principalIDs = (ArrayList<String>)siteObjects().get(sctx.site().getId(), Principal.ID);
		if (isEmpty(principalIDs) || principalIDs.contains(Principal.Generic.GUEST.getId())) {
			result.add(Principal.Generic.EVERYONE);
			result.add(Principal.Generic.GUEST);
			return result;
		}
		if (principalIDs.contains(Principal.Generic.BLOCKED.getId())) {
			result.clear();
			result.add(Principal.Generic.BLOCKED);
			return result;
		}

		HierarchicalEntity.Tree<Principal> tree = (HierarchicalEntity.Tree<Principal>)sctx.get(Principal.OBJS);
		principalIDs.stream().map(principalID -> Principal.find(tree.getElements(), principalID)).filter(principal -> principal != null).collect(Collectors.toCollection(() -> result));
		result.add(Principal.Generic.SITE_USER);
		result.add(Principal.Generic.EVERYONE);
		result.add(account());

		return result;
	}

	static final String TZ = "timezone";

	public TimeZone getTimeZone() {
		TimeZone zone = TimeZone.class.cast(get(TZ));
		return zone != null ? zone : TimeZone.getDefault();
	}

	public AccountContext setTimeZone(TimeZone zone) {
		return set(TZ, zone);
	}

	public void export(Response resp) {
		resp.set("accountContext", this);
	}

	protected Log log() {
		return Log.get(getClass());
	}
	@Override
	public String toString() {
		return getClass().getName() + "(" + (isUnknown() ? "unknown" : "\"" + account().getId() + "\"") + ")";
	}

	public static class ObjectsInSites extends LRUCache<String, NamedObjects> {
		private static final long serialVersionUID = 1L;

		private AccountContext actx;

		private ObjectsInSites() {
			setCapacity(Crescendo.siteObjectsInAccountContext());
		}

		public Object get(String siteID, String key) {
			return get(siteID).get(key);
		}

		public String string(String siteID, String key) {
			return get(siteID).string(key);
		}

		public Number number(String siteID, String key) {
			return get(siteID).number(key);
		}

		public boolean bool(String siteID, String key) {
			return get(siteID).bool(key);
		}

		public ObjectsInSites set(String siteID, String key, Serializable value) {
			if (value == null)
				remove(siteID, key);
			else {
				NamedObjects objs = get(siteID);
				if (objs == null || EMPTY.equals(objs))
					put(siteID, objs = new NamedObjects());
				objs.put(key, value);
				log().debug(() -> actx + "[\"" + siteID + "\"][\"" + key + "\"] = " + value);
			}
			return this;
		}

		public ObjectsInSites update(String siteID, String key, Serializable value) {
			NamedObjects objs = get(siteID);
			if (objs == null || EMPTY.equals(objs)) return this;

			if (value == null)
				remove(siteID, key);
			else {
				objs.put(key, value);
				log().debug(() -> actx + "[\"" + siteID + "\"][\"" + key + "\"] = " + value);
			}
			return this;
		}

		public Object remove(String siteID, String key) {
			NamedObjects objs = get(siteID);
			Object removed = objs == null || objs.equals(EMPTY) ? null : objs.remove(key);
			if (removed != null)
				log().debug(() -> actx + "[\"" + siteID + "\"][\"" + key + "\"] removed.");
			return removed;
		}
	}
}