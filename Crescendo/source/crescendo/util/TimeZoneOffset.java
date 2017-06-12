package crescendo.util;

import horizon.system.AbstractObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeZoneOffset extends AbstractObject {
	public static final int local = -TimeZone.getDefault().getRawOffset() / (1000 * 60);

	public static final int diff(int offset) {
		return local - offset;
	}

	public static final String string(int offset) {
		int tmp = -offset;
		String prefix = tmp >= 0 ? "+" : "-";
		tmp = Math.abs(tmp);
		int hour = tmp / 60,
			min = tmp % 60;
		return prefix + (hour > 9 ? hour : "0" + hour) + ":" + (min > 9 ? min : "0" + min);
	}

	public static final Date get(Date date, int offset) {
		int diff = diff(offset);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, diff);
		return cal.getTime();
	}

	public static final Date get(Date date, TimeZone timezone) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(timezone);
		return cal.getTime();
	}

	public static void main(String[] args) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm Z");
		TimeZone zone = TimeZone.getTimeZone("America/New_York");
		Date now = TimeSupport.now();
		Date date = get(now, zone);
		System.out.println("now: " + dateFormat.format(now) + ", date: " + dateFormat.format(date));
	}
}