package crescendo.system;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class SiteContextEvent extends Event {
	private static final long serialVersionUID = 1L;
	public static final String SETUP_SCTX = "setup-site-context";
	public static final String ENTER_SCTX = "enter-site-context";
	public static final String LOAD_SITE = "load-site";
	public static final String LOAD_SCTX = "load-site-context";
	public static final String UNLOAD_SCTX = "unload-site-context";

	private transient AccountContext actx;
	private transient SiteContext sctx;
	private HashMap<String, NamedObjects> actxLoads;
	private HashMap<String, ArrayList<String>> actxUnloads;

	public String siteID() {
		String siteID = string(Site.ID);
		if (isEmpty(siteID)) {
			Site site = Site.class.cast(get(Site.OBJ));
			if (site != null)
				siteID = site.getId();
		}
		return siteID;
	}

	public SiteContextEvent setSiteID(String siteID) {
		set(Site.ID, siteID);
		return this;
	}

	public SiteContext siteContext() {
		if (sctx == null) {
			Event parent = parent();
			if (parent instanceof SiteContextEvent)
				return SiteContextEvent.class.cast(parent).siteContext();
		}
		return sctx;
	}

	public SiteContextEvent set(SiteContext sctx) {
		this.sctx = sctx;
		return setSiteID(
			   sctx == null || sctx.isUnknown() ? null :
			   sctx.site().getId()
			   );
	}

	public AccountContext accountContext() {
		return actx != null ? actx : Client.get(request()).accountContext();
	}

	private boolean isInherent() {
		switch (type()) {
		case SETUP_SCTX:
		case LOAD_SITE:
		case LOAD_SCTX:
		case UNLOAD_SCTX: return true;
		default: return false;
		}
	}
	@Override
	public SiteContextEvent setLoad(String name, Serializable obj) {
		if (isInherent())
			siteContext().set(name, obj);
		else
			super.setLoad(name, obj);
		return this;
	}
	@Override
	public SiteContextEvent setUnload(String name) {
		if (isInherent())
			siteContext().remove(name);
		else
			super.setUnload(name);
		return this;
	}

	public Iterable<String> accountIDs() {
		ArrayList<String> result = new ArrayList<String>();
		if (!isEmpty(actxUnloads))
			result.addAll(actxUnloads.keySet());
		if (!isEmpty(actxLoads))
			result.addAll(actxLoads.keySet());
		return result;
	}

	public SiteContextEvent setLoad(String accountID, String name, Serializable obj) {
		if (obj != null) {
			if (ENTER_SCTX.equals(type())) {
				AccountContext actx = accountContext();
				if (actx.account().getId().equals(accountID))
					actx.siteObjects().set(siteID(), name, obj);
			} else {
				if (actxLoads == null)
					actxLoads = new HashMap<String, NamedObjects>();
				NamedObjects actxLoad = actxLoads.get(accountID);
				if (actxLoad == null)
					actxLoads.put(accountID, actxLoad = new NamedObjects());
				actxLoad.set(name, obj);
			}
		}
		return this;
	}

	public SiteContextEvent setUnload(String accountID, String name) {
		if (ENTER_SCTX.equals(type())) {
			AccountContext actx = accountContext();
			if (actx.account().getId().equals(accountID))
				actx.siteObjects().remove(siteID(), name);
		} else {
			if (actxUnloads == null)
				actxUnloads = new HashMap<String, ArrayList<String>>();
			ArrayList<String> actxUnload = actxUnloads.get(accountID);
			if (actxUnload == null)
				actxUnloads.put(accountID, actxUnload = new ArrayList<String>());
			if (!actxUnload.contains(name))
				actxUnload.add(name);
		}
		return this;
	}

	public void onSetup(SiteContext sctx) {
		set(sctx).setType(SETUP_SCTX).setFired(true).on();
	}

	public void onSiteLoad(SiteContext sctx) {
		set(sctx).setType(LOAD_SITE).setFired(true).on();
	}

	public void onLoad(SiteContext sctx) {
		set(sctx).setType(LOAD_SCTX).setFired(true).on();
	}

	public void onUnload(SiteContext sctx) {
		set(sctx).setType(UNLOAD_SCTX).setFired(true).on();
	}

	public void onEnter(AccountContext actx, SiteContext sctx) {
		this.actx = actx;
		set(sctx).setType(ENTER_SCTX).setFired(true).on();
	}
	@Override
	public boolean hasUpdates() {
		if (!update)
			update = !isEmpty(loads) || !isEmpty(unloads) || !isEmpty(actxLoads) || !isEmpty(actxUnloads);
		if (!update)
			for (Event child: children()) {
				update = child.hasUpdates();
				if (update) break;
			}
		return update;
	}

	public void update(SiteContext sctx) {
		if (sctx == null) return;
		set(sctx).after();
	}

	public void update(AccountContext actx) {
		String accountID = actx.account().getId(),
			   siteID = siteID();
		if (actxUnloads != null) {
			List<String> names = actxUnloads.get(accountID);
			if (!isEmpty(names))
				names.forEach(name -> actx.siteObjects().remove(siteID, name));
		}
		if (actxLoads != null) {
			NamedObjects objs = actxLoads.get(accountID);
			if (!isEmpty(objs))
				objs.forEach((key, value) -> actx.siteObjects().update(siteID, key, (Serializable)value));
		}
		children().forEach(child -> {
			if (!(child instanceof SiteContextEvent)) return;
			SiteContextEvent subEvt = SiteContextEvent.class.cast(child);
			subEvt.update(actx);
		});
	}
	@Override
	public void after() {
		super.after();
		SiteContext ctx = siteContext();
		unloads().forEach(ctx::remove);
		loads().forEach(ctx::set);
		children().forEach(child -> child.after());
	}

	public static class Support {
		public static final Collection<SiteContextEvent> forAccounts(Collection<Event> evts) {
			return Event.find(evts, SiteContextEvent.class, evt -> !isEmpty(evt.accountIDs()));
		}

	}
}