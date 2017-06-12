package crescendo.system;

import java.util.Map;

import horizon.database.DBAccess;
import horizon.persistence.PersistenceManager;
import horizon.system.Log;

public class SiteContext extends NamedObjects {
	private static final long serialVersionUID = 1L;
	public static final CrescendoException NOT_FOUND = new CrescendoException().setId(Site.NOT_FOUND);

	public static final SiteContext create(Site site) {
		Class<SiteContext> klass = Site.Profile.get(site).klass("site-context");
		return Site.Profile.instance(ifEmpty(klass, () -> SiteContext.class)).set(site);
	}

	private static final DBAccess dbAccess(String connectionName) {
		return DBAccess.get(connectionName);
	}

	private static final PersistenceManager persistence(DBAccess dbAccess) {
		return PersistenceManager.create(dbAccess);
	}

	public static final DBAccess adminAccess(boolean readWrite) {
		return dbAccess(Crescendo.adminConnection(readWrite));
	}

	public static final DBAccess adminAccess() {
		return adminAccess(Request.get().isReadWrite());
	}

	public static final PersistenceManager adminPersistence() {
		return persistence(adminAccess());
	}
	@Override
	public SiteContext set(String name, Object obj) {
		super.set(name, obj);
		if (obj != null)
			log().debug(() -> this + "[\"" + name + "\"] = " + obj);
		return this;
	}
	@Override
	public SiteContext setAll(Map<String, ? extends Object> objs) {
		super.setAll(objs);
		return this;
	}
	@Override
	public Object remove(Object key) {
		Object removed = super.remove(key);
		if (removed != null)
			log().debug(() -> this + "[\"" + key + "\"] removed.");
		return removed;
	}

	public Site site() {
		return Site.class.cast(get(Site.OBJ));
	}

	public SiteContext set(Site site) {
		return set(Site.OBJ, site);
	}

	public boolean isUnknown() {
		return site() == null;
	}

	public DBAccess dbAccess(boolean readWrite) {
		if (readWrite) {
			Request req = Request.get(false);
			if (req != null)
				req.ensureReadWrite();
		}
		return dbAccess(site().dbConnection(readWrite));
	}

	public DBAccess dbAccess() {
		return dbAccess(Request.get().isReadWrite());
	}

	public PersistenceManager persistence() {
		return persistence(dbAccess());
	}

	public boolean withAdminDatabase() {
		boolean readWrite = Request.get().isReadWrite();
		return Crescendo.adminConnection(readWrite).equals(site().dbConnection(readWrite));
	}

	public Site.Profile profile() {
		return Site.Profile.get(site());
	}

	public void setup() {
		if (!Site.Status.CREATED.equals(site().getStatus())) return;

		profile().fireContextEvent(evt -> evt.onSetup(this));
	}

	public void siteLoad() {
		if (Site.Status.CREATED.equals(site().getStatus())) return;

		profile().fireContextEvent(evt -> evt.onSiteLoad(this));
	}

	public boolean isLoaded() {
		return bool(SiteContextEvent.LOAD_SCTX);
	}

	public SiteContext load() {
		if (!isLoaded()
		 && !Site.Status.CREATED.equals(site().getStatus())
		 && site().sitespace() == Request.get(false).sitespace()) {
			set(SiteContextEvent.LOAD_SCTX, Boolean.TRUE);
			dbAccess().open(dbaccess -> {
				profile().fireContextEvent(evt -> evt.onLoad(this));
				return null;
			}, null);
		}
		return this;
	}

	public void unload() {
		if (!isLoaded()
		 || Site.Status.CREATED.equals(site().getStatus())) return;

		set(SiteContextEvent.LOAD_SCTX, Boolean.FALSE);
		profile().fireContextEvent(evt -> evt.onUnload(this));
	}

	public void export(Response resp) {
		export(resp, true);
	}

	private void export(Response resp, boolean current) {
		if (isUnknown()) return;

		resp.set(site().getId(), this);
		if (current) {
			load();
			resp.set(Site.ID, site().getId());
		}
	}

	protected Log log() {
		return Log.get(getClass());
	}
	@Override
	public String toString() {
		return getClass().getName()
			+ "(" + (isUnknown() ? "unknown" : "\"" + site().getId() + "\"") + ")";
	}
}