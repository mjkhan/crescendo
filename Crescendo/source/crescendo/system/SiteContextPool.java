package crescendo.system;

import java.util.Collection;

import horizon.system.AbstractObject;

public class SiteContextPool extends AbstractObject {
	private static ObjectCache<SiteContext> cache;
	private static final SiteContextPool pool = new SiteContextPool();

	public static final SiteContextPool get() {
		return pool;
	}

	public static final void configure(Feature feature) {
		Feature config = ifEmpty(feature.feature("site-context-cache"), () -> Feature.load("crescendo/cache/default-cache.xml"));
		cache = ObjectCache.create(config);
	}

	private SiteContextPool() {}

	public SiteContext get(String siteID, String siteType) {
		SiteContext sctx = cache.get(siteID);
		if (sctx != null && !isEmpty(siteType)) {
			if (!siteType.equals(sctx.site().getType()))
				sctx = null;
		}
		return sctx;
	}

	public SiteContext context(String siteID, String siteType) {
		if (isEmpty(siteID)) return null;
		SiteContext sctx = get(siteID, siteType);

		if (sctx == null) {
			sctx = load(siteID, siteType);
			if (sctx != null) {
				cache.set(siteID, sctx);
				sctx.siteLoad();
				if (sctx.withAdminDatabase())
					sctx.load();
			}
		}
		return sctx;
	}

	public SiteContext context(Request req) {
		return context(req.string(Site.CURRENT), req.string(Site.TYPE));
	}

	private synchronized SiteContext load(String siteID, String siteType) {
		SiteContext sctx = get(siteID, siteType);
		if (sctx != null) return sctx;

		Feature sftr = Client.Profile.get().feature(Site.OBJ);
		Site site = Site.Provider.Factory.create(sftr).getSite(siteID, siteType);
		if (site != null)
			sctx = SiteContext.create(site);
		return sctx;
	}

	public void remove(String siteID) {
		if (isEmpty(siteID)) return;

		SiteContext sctx = cache.remove(siteID);
		if (sctx != null)
			sctx.unload();
	}

	public void propagate(Collection<Event> evts) {
		if (isEmpty(evts)) return;

		if (cache.updatesViaJMS())
			Event.send(evts);
		else
			evts.forEach(e -> {
				SiteContextEvent evt = SiteContextEvent.class.cast(e);
				String siteID = evt.siteID();
				SiteContext sctx = cache.get(siteID);
				if (sctx != null) {
					evt.update(sctx);
					cache.update(siteID, sctx);
				}
			});
		AccountContextPool.get().propagate(SiteContextEvent.Support.forAccounts(evts));
	}
}