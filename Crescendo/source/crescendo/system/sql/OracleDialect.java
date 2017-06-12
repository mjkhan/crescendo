package crescendo.system.sql;

import horizon.data.Dataset;

import java.util.Date;
import java.util.List;

public class OracleDialect extends AbstractDialect {
	@Override
	public Dataset select(String name, String columns, String from, String where, String orderBy, List<Object> args, int start, int fetch) {
		return null;
	}
	@Override
	public String sqlize(Date date) {
		return "to_timestamp('" + date + "', 'yyyy-mm-dd hh24:mi:ss.ff')";
	}
/*
	public static void main(String[] args) {
		System.out.println(new OracleDialect().sqlize(TimeSupport.now()));
	}
*/
}