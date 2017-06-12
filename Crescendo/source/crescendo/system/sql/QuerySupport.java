package crescendo.system.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import horizon.data.Dataset;
import horizon.data.FieldAware;
import horizon.data.GenericMap;
import horizon.data.Records;
import horizon.system.AbstractObject;
import horizon.util.Text;

public class QuerySupport extends AbstractObject {
	private static final QuerySupport qs = new QuerySupport();

	public static final QuerySupport get() {return qs;}

	private final Terms terms = new Terms();

	private QuerySupport() {}

	public Terms terms() {
		return terms;
	}

	public final String[] split(String s) {
		return Text.split(s, ",");
	}

	public GenericMap<Object> namedParams()	{
		return new GenericMap<Object>();
	}

	public final <T> List<T> toList(T... ts) {
		Supplier<List<T>> factory = ArrayList::new;
		return !isEmpty(ts) ? Stream.of(ts).collect(Collectors.toCollection(factory)) : factory.get();
	}

	public final String[] asStrings(Collection<? extends FieldAware> fieldAwares, String fieldName) {
		if (isEmpty(fieldAwares)) return null;

		List<Object> values = fieldValues(fieldAwares, fieldName);
		return isEmpty(values) ? null : Arrays.copyOf(values.toArray(), values.size(), String[].class);
	}

	public final List<Object> fieldValues(Collection<? extends FieldAware> fieldAwares, String fieldName) {
		return Records.values(fieldAwares, fieldName, false);
	}

	public final String toString(Iterable<?> objs) {
		return toString(objs, ",");
	}

	public final String toString(Iterable<?> objs, String delimiter) {
		return Text.toString(objs, delimiter);
	}

	public final <T> String asIn(T... objs) {
		return asIn(toList(objs));
	}

	public final String toString(Iterable<?> list, String itemFormat, String delim) {
		if (isEmpty(list)) return "";
		int index = 0;
		StringBuilder buff = new StringBuilder();
		for (Object item: list) {
			if (buff.length() > 0)
				buff.append(delim);
			buff.append(itemFormat.replace("{item}", "" + item).replace("{index}", Integer.toString(index)));
			++index;
		}
		return buff.toString();
	}

	public final String toString(Iterable<? extends Map<String, ?>> list, String itemFormat, String delim, String... fieldNames) {
		if (isEmpty(list)) return "";
		int index = 0;
		StringBuilder buff = new StringBuilder();
		for (Map<String, ?> item: list) {
			if (buff.length() > 0)
				buff.append(delim);
			String line = itemFormat;
			for (String fieldname: fieldNames)
				line = line.replace("{" + fieldname + "}", "" + item.get(fieldname));
			buff.append(line.replace("{index}", Integer.toString(index)));
			++index;
		}
		return buff.toString();
	}

	public final String quote(Object obj) {
		String s = obj == null ? "" : obj.toString();
		return "'" + s.replace("'", "''") + "'";
	}

	public final String asIn(Collection<?> objs) {
		if (isEmpty(objs)) return "in ('')";

		return "in (" + objs.stream().filter(obj -> !isEmpty(obj)).map(obj -> {
			boolean quote = !(obj instanceof Number) && !(obj instanceof Boolean);
			return quote ? quote(obj) : obj.toString();
		}).collect(Collectors.joining(", ")) + ")";
	}

	public boolean isIn(Object lv, Object... rvs) {
		return Text.isIn(lv, rvs);
	}

	public final int rowCount(Dataset dataset) {
		return dataset.number("row_cnt").intValue();
	}

	public final Dataset setDataBounds(Dataset dataset, int fetchSize, int totalCount, int start) {
		if (dataset != null && !dataset.isEmpty())
			dataset.setFetchSize(fetchSize).setTotalSize(totalCount).setStart(start);
		return dataset;
	}

	public boolean parameterize(Object obj) {
		if (isEmpty(obj)) return false;
		if (obj instanceof Object[]) {
			Object[] objs = (Object[])obj;
			return objs.length == 1 && objs[0] != null;
		}
		if (obj instanceof Collection) {
			Collection<?> objs = (Collection<?>)obj;
			return objs.size() == 1;
		}
		return true;
	}

	public String fieldCondition(String fieldname, Object obj) {
		if (obj == null)
			return fieldname + " is null";
		if (!(obj instanceof Object[]))
			return fieldname + " = ?";
		Object[] objs = (Object[])obj;
		switch (objs.length) {
		case 1: return objs[0] == null ? fieldname + " is null" : fieldname + " = ?";
		default: return fieldname + " " + asIn(objs);
		}
	}

	public static class Terms {
		public String clause(String... fieldNames) {
			return Stream.of(fieldNames).filter(fieldName -> !isEmpty(fieldName != null)).map(fieldName -> fieldName + " like '%{term}%'").collect(Collectors.joining(" or"));
		}

		public String condition(String clause, String[] terms) {
			return isEmpty(clause) || isEmpty(terms) ? "" : "(" + Stream.of(terms).map(term -> clause.replace("{term}", term.replace("'", "''"))).collect(Collectors.joining("or ")) + ")";
		}

		public String condition(String clause, String terms) {
			return condition(clause, Text.split(terms, " "));
		}
	}
/*
	public static void main(String[] args) {
		Terms terms = new Terms();
		System.out.println(terms.condition(terms.clause("abc"), "d'ef"));

		String s = "ab\"c";
		System.out.println(s + ": " + Text.Quote.DOUBLE.get(s));
		s = "ab'c";
		System.out.println(s + ": " + Text.Quote.SINGLE.get(s));
		testFormat();
	}

	private static void testFormat() {
		ArrayList<GenericMap<Object>> dataset = new ArrayList<>();
		dataset.add(new GenericMap<Object>().caseSensitiveKey(false).set("col0", "val0-0").set("col1", "val0-1"));
		dataset.add(new GenericMap<Object>().caseSensitiveKey(false).set("col0", "val1-0").set("col1", "val1-1"));
		System.out.println(QuerySupport.get().toString(dataset, "    select '{col0}' grp_type, '{col1}' grp_id from crsnd_dummy", " union\n", "col0", "col1"));
	}
*/
}