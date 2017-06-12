package crescendo.system.site;

import java.util.Map;

import crescendo.system.Entity;
import crescendo.system.Site;
import crescendo.system.code.CodeServant;

public class SiteValidator extends Site.Validator {
	@Override
	public Result validate(String name, Map<String, Object> args) {
		if ("id".equals(name))
			return validID((String)args.get("value"));
		return Result.VALID;
	}

	protected Result validID(String id) {
		if (isEmpty(id))
			return new Result().setMessage("empty-" + Site.ID);

		String pattern = cfg.string(Site.ID + "-pattern");
		if (!isEmpty(pattern) && !id.matches(pattern))
			return new Result().setMessage("invalid-" + Site.ID);
		if (getCount(cfg.table(), "site_id = '" + id + "'") > 0)
			return new Result().setMessage(Site.ID + "-in-use");
		if (CodeServant.create().exists("site", "reserved-id", id))
			return new Result().setMessage("reserved-" + Site.ID);
		return Result.VALID;
	}
	@Override
	protected void doValidate(Entity entity) {
		Site site = Site.class.cast(entity);
		switch (site.state()) {
		case CREATED:
			Result result = validate(Site.ID, site.getId());
			if (!result.isValid())
				throw result.invalid();
			break;
		default: break;
		}
	}
}