package crescendo.system.site;

import crescendo.system.Feature;
import crescendo.system.Site;
import crescendo.system.SiteContextEvent;

public class SiteEvent extends SiteContextEvent {
	private static final long serialVersionUID = 1L;
	private static final String NAME = Site.OBJ + "-event";
	public static final String CHANGE_STATUS = Site.CHANGE_STATUS;

	public static final <T extends SiteEvent> T create(Feature feature) {
		if (feature == null)
			feature = Site.feature(null);
		return create(feature, NAME);
	}

	public Site site() {
		return Site.class.cast(get(Site.OBJ));
	}
}