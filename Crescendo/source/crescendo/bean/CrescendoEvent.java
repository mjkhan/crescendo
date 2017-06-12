package crescendo.bean;

import crescendo.system.AccountContext;
import crescendo.system.AccountContextEvent;
import crescendo.system.SiteContext;
import crescendo.system.SiteContextEvent;

public class CrescendoEvent extends CrescendoMessageBean {
	@Override
	protected void consume(Object obj) throws Exception {
		if (obj instanceof Iterable)
			process(Iterable.class.cast(obj));
		else if (obj instanceof Object[])
			process(Object[].class.cast(obj));
		else
			process(obj);
	}

	private void process(Iterable<?> objs) throws Exception {
		for (Object obj: objs)
			if (obj != null)
				process(obj);
	}

	private void process(Object... objs) throws Exception {
		for (Object obj: objs)
			if (obj != null)
				process(obj);
	}

	private void process(Object obj) throws Exception {
		boolean processed = false;
		if (obj instanceof SiteContextEvent) {
			process(SiteContextEvent.class.cast(obj));
			processed = true;
		}
		if (obj instanceof AccountContextEvent) {
			process(AccountContextEvent.class.cast(obj));
			processed = true;
		}
		if (!processed)
			log().debug(() -> obj + " ignored, neither a " + SiteContextEvent.class.getName() + " nor a " + AccountContextEvent.class.getName());
	}

	private void process(AccountContextEvent evt) throws Exception {
		AccountContext actx = accountContexts().get(evt.accountID());
		if (actx == null || actx.isUnknown()) return;

		evt.update(actx);
	}

	private void process(SiteContextEvent evt) throws Exception {
		SiteContext sctx = siteContexts().get(evt.siteID(), null);
		if (sctx == null || sctx.isUnknown()) return;

		evt.update(sctx);
	}
}