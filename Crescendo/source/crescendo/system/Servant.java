package crescendo.system;

import java.util.function.Supplier;

import crescendo.system.sql.Dialect;
import crescendo.system.sql.QuerySupport;
import horizon.data.Dataset;
import horizon.database.DBAccess;
import horizon.persistence.PersistenceManager;
import horizon.system.AbstractObject;

public interface Servant {
	public <T extends Servant> T set(SiteContext sctx);

	public <T extends Servant> T set(Feature feature);

	public static class Generic extends AbstractObject implements Servant {
		public static final <T extends Servant> T create(SiteContext sctx, Feature feature, String entryID) {
			T t = Feature.instance(notEmpty(feature.klass(entryID), entryID));
			return t.set(sctx).set(feature);
		}

		protected static final QuerySupport qsupport = QuerySupport.get();
		protected SiteContext sctx;
		protected Feature feature;

		protected static final CrescendoException exception(Throwable t) {
			return CrescendoException.create(t);
		}

		protected UnsupportedOperationException unsupportedOperation(String method) {
			return new UnsupportedOperationException(String.format("Implement the method %s.%s.", getClass().getName(), method));
		}

		public String cref(String key) {
			return feature != null ? feature.string(key) : null;
		}

		protected DBAccess adminAccess() {
			return SiteContext.adminAccess();
		}

		protected PersistenceManager adminPersistence() {
			return SiteContext.adminPersistence();
		}

		public <T extends SiteContext> T siteContext() {
			return (T)sctx;
		}

		protected String siteID() {
			return sctx != null ? sctx.site().getId() : null;
		}
		@Override
		public <T extends Servant> T set(SiteContext sctx) {
			if (!equals(this.sctx, sctx))
				this.sctx = sctx;
			return (T)this;
		}
		@Override
		public <T extends Servant> T set(Feature feature) {
			if (!equals(this.feature, feature))
				this.feature = feature;
			return (T)this;
		}

		protected PermissionInspector permission(String name) {
			return ifEmpty(PermissionInspector.create(feature, name), PermissionInspector::new).set(sctx).set(feature);
		}

		protected PermissionInspector permission() {
			return permission(null);
		}

		protected <T> T ifPermitted(Request req, Supplier<T> action, Runnable teardown) {
			return permission().check(req, action, null, teardown);
		}

		protected <T> T ifPermitted(Request req, Supplier<T> action) {
			return ifPermitted(req, action, null);
		}

		protected DBAccess dbAccess() {
			return !"admin".equals(feature.string("dbaccess")) ? sctx.dbAccess() : adminAccess();
		}

		protected PersistenceManager persistence() {
			return sctx.persistence();
		}

		protected Client client() {
			return Client.get(false);
		}

		protected AccountContext accountContext() {
			return AccountContext.get();
		}
		//TODO:Decide how to get the current client and account context
		protected Client client(Request req) {
			return Client.get(req);
		}

		protected AccountContext accountContext(Request req) {
			Client client = client(req);
			return client != null ? client.accountContext() : AccountContext.UNKNOWN;
		}

		protected Dialect sqlDialect(String dialectName) {
			return Dialect.create(sctx, feature, dialectName);
		}

		public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
			throw unsupportedOperation("search(String, String, int, int, Object...)");
		}

		public Dataset search(String condition, String orderBy, Object... args) {
			return search(condition, orderBy, -1, 0, args);
		}

		public Dataset search(String condition, Object... args) {
			return search(condition, null, args);
		}
	}
}