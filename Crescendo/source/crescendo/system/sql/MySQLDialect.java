package crescendo.system.sql;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import horizon.data.Dataset;

public class MySQLDialect extends AbstractDialect {
	private static final String SELECT =
		"select sql_calc_found_rows {columns}\nfrom {table}{where}{orderBy}{fetch};"
	+ "\nselect found_rows() row_cnt;";
	@Override
	public Dataset select(String name, String columns, String from, String where, String orderBy, List<Object> args, int start, int fetch) {
		String query = SELECT.replace("{columns}", ifEmpty(columns, "*")).replace("{table}", notEmpty(from, "from"))
			  .replace("{where}", isEmpty(where) ? "" : "\nwhere " + where)
			  .replace("{orderBy}", isEmpty(orderBy) ? "" : "\norder by " + orderBy);
		boolean fetchAll = fetch < 1;
		query = query.replace("{fetch}", fetchAll ? "" : "\nlimit ?, ?");
		if (!fetchAll) {
			if (args == null)
				args = new ArrayList<>();
			args.add(Integer.valueOf(start));
			args.add(Integer.valueOf(fetch));
		}
		List<Dataset> datasets = dbAccess().query(name).getDatasets(query, isEmpty(args) ? null : args.toArray());
		Dataset dataset = datasets.get(0),
				countSet = datasets.get(1);
		if (!fetchAll)
			qsupport.setDataBounds(dataset, fetch, qsupport.rowCount(countSet), start);
		return dataset;
	}
	@Override
	public String sqlize(Date date) {
		return "'" + date + "'";
	}
/*
	public static void main(String[] args) {
		System.out.println(new MySQLDialect().sqlize(TimeSupport.now()));
	}
*/
}