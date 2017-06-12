package crescendo.util;

import horizon.system.AbstractObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
/**A utility to help work with Date.
 */
public class TimeSupport extends AbstractObject {
	private TimeSupport() {}
	/**Returns the Timestamp as of now.
	 * @return a Timestamp
	 */
	public static final Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}
	/**Returns the string representation of the time formatted after the pattern.
	 * @param time		time
	 * @param pattern	a date format
	 * @return string representation of the time formatted after the pattern
	 */
	public static final String format(Date time, String pattern) {
		return new SimpleDateFormat(pattern).format(time);
	}
	/**Returns the string representation of <code>now()</code> formatted after the pattern.
	 * @param pattern a date format
	 * @return the string representation of a Timestamp
	 */
	public static final String now(String pattern) {
		return format(now(), pattern);
	}
	/**Converts str to a Timestamp and returns it.
	 * @param str	a String
	 * @return the converted Timestamp
	 */
	public static final Timestamp timeOf(String str) {
		return isEmpty(str) ? null : Timestamp.valueOf(str);
	}
	/**Tests if lv and rv are of the same date.
	 * @param lv left-hand sided value
	 * @param rv right-hand sided value
	 * @return
	 * <ul><li>true if both are of the same date
	 * 	   <li>false otherwise
	 * </ul>
	 */
	public static final boolean sameDate(Date lv, Date rv) {
		return equals(lv, rv) ?
			true :
			format(lv, "yyyy-MM-dd").equals(format(rv, "yyyy-MM-dd"));
	}

	public static final TimeZone getTimeZone(int rawOffset) {
		String[] ids = TimeZone.getAvailableIDs(rawOffset * 1000 * 60);
		return isEmpty(ids) ? null : TimeZone.getTimeZone(ids[0]);
	}

	public static final int getAge(Date date) {
		if (date == null) return -1;

		GregorianCalendar
			birthday = new GregorianCalendar(),
			now = new GregorianCalendar();
		birthday.setTime(date);
		int age = now.get(GregorianCalendar.YEAR) - birthday.get(GregorianCalendar.YEAR);
		if (birthday.get(GregorianCalendar.MONTH) > now.get(GregorianCalendar.MONTH)
		 || birthday.get(GregorianCalendar.MONTH) == now.get(GregorianCalendar.MONTH)
		 && birthday.get(GregorianCalendar.DAY_OF_MONTH) > now.get(GregorianCalendar.DAY_OF_MONTH))
			--age;
		return age;
	}

	public static final int getAge(String date) {
		return isEmpty(date) ? -1 : getAge(timeOf(date + " 00:00:01"));
	}
}