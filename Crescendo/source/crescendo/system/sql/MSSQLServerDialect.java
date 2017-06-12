package crescendo.system.sql;

import horizon.data.Dataset;

import java.util.Date;
import java.util.List;

public class MSSQLServerDialect extends AbstractDialect {
	@Override
	public Dataset select(String name, String columns, String from, String where, String orderBy, List<Object> args, int start, int fetch) {
		return null;
	}
	@Override
	public String sqlize(Date date) {
		return "cast('" + date + "' as datetime)";
	}
/*
	public static void main(String[] args) {
		System.out.println(new MSSQLServerDialect().sqlize(TimeSupport.now()));
	}
*/
}