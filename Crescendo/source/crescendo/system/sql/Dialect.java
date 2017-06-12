package crescendo.system.sql;

import horizon.data.Dataset;

import java.util.Date;
import java.util.List;

import crescendo.system.Feature;
import crescendo.system.Servant;
import crescendo.system.SiteContext;

public interface Dialect extends Servant {
	public static <T extends Dialect> T create(SiteContext sctx, Feature feature, String entryID) {
		return Servant.Generic.create(sctx, feature, entryID);
	}

	public Dataset select(String name, String columns, String from, String where, String orderBy, List<Object> args, int start, int fetch);

	public String sqlize(Date date);
}