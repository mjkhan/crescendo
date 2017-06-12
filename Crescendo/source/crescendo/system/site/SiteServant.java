package crescendo.system.site;

import java.util.Map;

import crescendo.system.Account;
import crescendo.system.AccountContext;
import crescendo.system.Client;
import crescendo.system.Feature;
import crescendo.system.NamedObjects;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.Site;
import crescendo.system.SiteContext;
import horizon.data.Dataset;
import horizon.data.FieldValues;
import horizon.persistence.Persistent.Info;

public class SiteServant extends Servant.Generic implements Site.Provider {
	private static final String NAME = Site.OBJ + "-servant";
	protected static final String SEARCH = Site.OBJ + "-search";

	public static final <T extends SiteServant> T create(Feature feature) {
		return create(null, !isEmpty(feature) ? feature : Site.feature(null), NAME);
	}

	protected Site.Config config() {
		return Site.config(feature);
	}
	@Override
	protected SitePermission permission() {
		return (SitePermission)permission("site-permission");
	}
	@Override
	public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
		String table = config().table();
		return sqlDialect(Site.OBJ + "-dialect")
			  .select("search-" + table, null, table, condition, orderBy, !isEmpty(args) ? qsupport.toList(args) : null, start, fetch);
	}

	public NamedObjects search(Request req) {
		return permission().check(req.defaultAction(Site.SEARCH), () -> {
				String terms = qsupport.terms().condition(feature.string(SEARCH), req.string(Site.TERMS)),
					   condition = ifEmpty(req.string(Site.CONDITION), ""),
					   orderBy = ifEmpty(req.string(Site.ORDER), "site_id");
				String[] types = req.objects(Site.TYPE + "s");

				if (!isEmpty(types)) {
					condition = "".equals(condition) ? "site_type {site-types}" : "\nand site_type {site-types}";
					condition = condition.replace("{site-types}", qsupport.asIn(types));
				}
				if (!isEmpty(terms))
					condition = "".equals(condition) ? terms : condition + "\nand " + terms;
				String statusCondition = Site.Status.condition(req.string(Site.STATUS));
				if (!isEmpty(statusCondition)) {
					statusCondition = "status " + statusCondition;
					condition = isEmpty(condition) ? statusCondition : condition + "\nand " + statusCondition;
				}

				Object[] args = req.objects(Site.ARGS);
				int fetch = req.number(Site.FETCH).intValue(),
					start = req.number(Site.START).intValue();
				return new NamedObjects().set(Site.LIST, search(condition, orderBy, start, fetch, args));
			}
			, () -> NamedObjects.EMPTY, null
		);
	}

	public Dataset getInfo(String fieldName, Object[] values) {
		return search(
			qsupport.fieldCondition(fieldName, values) + " and status " + Site.Status.condition("none")
		  , qsupport.parameterize(values) ? values : null
		);
	}

	public Dataset getInfo(String... siteIDs) {
		return getInfo("site_id", siteIDs);
	}

	private static final String ATTRS = "select * from " + Info.table(Site.Attribute.class) + " where site_id = ?";

	public Dataset getAttributeInfo(String siteID) {
		return adminAccess().query(Site.ATTR).getRecords(ATTRS, new Object[]{siteID});
	}

	public NamedObjects getInfo(Request req) {
		String siteID = req.string(Site.ID);
		return adminAccess().open(dbaccess -> {
			Dataset dataset = getInfo(siteID);
			req.set(Site.INFO, dataset.get(0));
			return permission().check(req.defaultAction(Site.GET), () -> {
				return new NamedObjects()
				  .set(Site.INFO, dataset)
				  .set(Site.ATTR, dataset.isEmpty() ? null : getAttributeInfo(siteID));
				}, null, null
			);
		}, null);
	}

	public NamedObjects viewInfo(Request req) {
		String siteID = req.string(Site.ID);
		return adminAccess().open(dbaccess -> {
			Dataset dataset = getInfo(siteID);
			if (!dataset.isEmpty())
				req.set(Site.INFO, dataset.get(0));
			return permission().check(req.defaultAction(Site.VIEW), () -> {
				return new NamedObjects()
				  .set(Site.INFO, dataset)
				  .set(Site.ATTR, dataset.isEmpty() ? null : getAttributeInfo(siteID));
				}, null, null
			);
		}, null);
	}

	public Dataset newInfo(int count) {
		if (count < 1)
			throw new RuntimeException("invalid-site-count");
		Dataset siteSet = search("site_id is null");
		for (int i = 0; i < count; ++i)
			config().setDefaults(siteSet.append());
		return siteSet;
	}

	public NamedObjects newInfo(Request req) {
		return new NamedObjects().set(Site.INFO, newInfo(Math.max(1, req.defaultAction(Site.NEW).number(Site.COUNT).intValue())));
	}
	@Override
	public Site getSite(String siteID, String siteType) {
		boolean siteTyped = !isEmpty(siteType);
		String condition = "site_id = ?{site-type} and status {site-status}".replace("{site-type}", !siteTyped ? "" : " and site_type = ?").replace("{site-status}", Site.Status.condition(null));
		Object[] args = siteTyped ? new Object[]{siteID, siteType} : new Object[]{siteID};

		return adminAccess().open(dbaccess -> {
			Dataset siteSet = search(condition, args);
			Site site = null;
			if (!siteSet.isEmpty())
				setAttributes(site = Site.Factory.create(config(), siteSet.get(0)));
			return site;
		}, null);
	}

	public Site getSite(String siteID) {
		return getSite(siteID, null);
	}

	protected void setAttributes(Site site) {
		if (site == null) return;
		site.attributes().read(getAttributeInfo(site.getId()));
	}

	public Site.Validator.Result validate(String name, Map<String, Object> args) {
		return config().validator()
			  .set(adminAccess())
			  .validate(name, args);
	}

	public Site.Validator.Result validate(Request req) {
		return validate(
				  req.defaultAction(Site.VALIDATE).string(Site.Validator.NAME),
				  req
			   );
	}

	public int create(Site site) {
		return adminPersistence().create(site);
	}

	public int update(Site site) {
		return adminPersistence().update(site);
	}

	protected int save(Site site, SiteEvent evt) {
		if (site == null) return 0;
		return adminAccess().transact(dbaccess -> {
			int result = 0;
			boolean event = evt != null;
			switch (site.state()) {
			case CREATED: result = create(site); break;
			case MODIFIED: result = update(site); break;
			default:
				if (site.attributes().isDirty())
					result = update(site);
				break;
			}
			if (event && evt.setFired(result > 0).isFired()) {
				if (SiteEvent.UPDATE.equals(evt.type()))
					evt.set("changed-" + Site.FIELD_NAME, site.clearChangedFields());
				evt.on();
			}
			if (event && evt.isFired() && !Site.Status.CREATED.equals(site.getStatus()))
				switch (evt.type()) {
				case SiteEvent.UPDATE:
				case SiteEvent.CHANGE_STATUS: evt.setLoad(Site.OBJ, site); break;
				case SiteEvent.REMOVE: evt.setHasUpdates(true); break;
				}
			return result;
		}, null);
	}

	public SiteEvent save(Site site) {
		SiteEvent evt = SiteEvent.create(feature);
		if (site != null
		 && evt.setType(Site.State.CREATED.equals(site.state()) ? SiteEvent.CREATE : SiteEvent.UPDATE)
		   .set(Site.OBJ, site)
		   .before()
		   )
			save(site, evt);
		return evt;
	}

	public SiteEvent save(FieldValues siteInfo, FieldValues attributes) {
		Site site = null;
		switch (siteInfo.state()) {
		case CREATED: site = Site.Factory.create(config()); break;
		default: site = getSite(siteInfo.string("site_id")); break;
		}
		if (site != null) {
			site.read(adminPersistence().getRecord(config().entityAs(), siteInfo));
			if (attributes != null)
				site.attributes().set(attributes);
		}
		return save(site);
	}

	public SiteEvent save(Request req) {
		AccountContext actx = Client.get(req).accountContext().ensureKnownPrincipal();
		Account account = actx.account();
		FieldValues siteInfo = FieldValues.class.cast(req.get(Site.INFO)),
					attributes = FieldValues.class.cast(req.get(Site.ATTR));
		if (isEmpty(siteInfo.get("ins_id")))
			siteInfo.put("ins_id", account.getId());
		if (isEmpty(siteInfo.get("ins_name")))
			siteInfo.put("ins_name", account.getAlias());
		return permission().check(req.defaultAction(Site.State.CREATED.equals(siteInfo.state()) ? Site.CREATE : Site.UPDATE), () -> save(siteInfo, attributes), null, null);
	}

	public SiteEvent setStatus(Site site, Site.Status status) {
		SiteEvent evt = SiteEvent.create(feature);
		if (site != null
		 && evt.setType(Site.Status.remove(status) ? SiteEvent.REMOVE : SiteEvent.CHANGE_STATUS)
			   .set(Site.OBJ, site).set(Site.STATUS, status)
		 	   .before()) {
			site.setStatus(status);
			save(site, evt);
		}
		return evt;
	}

	public SiteEvent setStatus(Request req) {
		String siteID = req.string(Site.ID);
		Site.Status status = Site.Status.class.cast(req.remove(Site.STATUS));
		Site site = !isEmpty(siteID) && !isEmpty(status) ? getSite(siteID) : null;
		return permission().check(req.defaultAction(Site.Status.remove(status)? Site.REMOVE : Site.CHANGE_STATUS).set(Site.OBJ, site), () -> setStatus(site, status), null, null);
	}

	public boolean setup(Site site) {
		if (site == null || !Site.Status.CREATED.equals(site.getStatus())) return false;

		set(SiteContext.create(site));

		return dbAccess().transact(dbaccess -> {
			sctx.setup();
			return true;
		}, null);
	}
}