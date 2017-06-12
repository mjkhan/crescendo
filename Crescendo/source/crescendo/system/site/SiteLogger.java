package crescendo.system.site;

import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.SiteContext;

public class SiteLogger extends Servant.Generic {
	private static final String NAME = "site-logger";

	public static final SiteLogger create(SiteContext sctx) {
		Class<? extends SiteLogger> klass = sctx.profile().klass(NAME);
		if (klass == null)
			klass = SiteLogger.class;
		SiteLogger logger = Feature.instance(klass);
		logger.set(sctx);
		return logger;
	}

	protected int log(String siteID, String accountID, String prevLog) {
		if (!isEmpty(prevLog)) {

		} else {

		}
		return 0;
	}

	public SiteEvent log(Request req) {
		return null;
	}
}