package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Row;
import net.coagulate.GPHUD.SafeMap;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * DateTime utilities.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class DateTime {

	private static String fromUnixTime(int date, DateFormat df) { return df.format(new Date(((long) (date)) * ((long) 1000))); }

	public static String fromUnixTime(int date, String timezone) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		df.setTimeZone(TimeZone.getTimeZone(timezone));
		return fromUnixTime(date, df);
	}

	public static Row inputRow(SafeMap values) {
		throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * Set up an input form
	 *
	 * @param prefix Prefix for the field names
	 * @param values Existing values
	 * @return appropriate table row
	 */
	public static Row inputDateTimeRow(String prefix, SafeMap values, String defaulttimezone) {
		Row t = new Row();
		String tz = values.get(prefix + "timezone");
		if (tz == null || tz.isEmpty()) { tz = defaulttimezone; }
		t.add(prefix);
		t.add(ti(prefix, "day", 2, values));
		t.add(ti(prefix, "month", 2, values));
		t.add(ti(prefix, "year", 4, values));
		t.add(ti(prefix, "hour", 2, values));
		t.add(ti(prefix, "minute", 2, values));
		t.add(DateTime.getTimeZoneList(prefix + "timezone", tz));
		return t;
	}

	private static TextInput ti(String prefix, String component, int size, SafeMap values) {
		String name = prefix + component;
		return new TextInput(name, values.get(name), size);
	}


	/**
	 * Set up a duration input form.
	 *
	 * @param prefix  Prefix for the field names
	 * @param values  Existing values
	 * @param spacers If true inserts blank cells to help line up with date input box
	 * @return Table Row
	 */
	public static Row inputIntervalRow(String prefix, SafeMap values, boolean spacers) {
		Row t = new Row();
		t.add(prefix);
		t.add(ti(prefix, "day", 2, values));
		if (spacers) {
			t.add("");
			t.add("");
		}
		t.add(ti(prefix, "hour", 2, values));
		t.add(ti(prefix, "minute", 2, values));
		return t;
	}

	/**
	 * Set up duration input form, without spacers.
	 *
	 * @param prefix Prefix for field names
	 * @param values Existing values
	 * @return Table Row
	 */
	public static Row inputIntervalRow(String prefix, SafeMap values) {
		return inputIntervalRow(prefix, values, false);
	}

	/**
	 * Convert the Form into a unixtime.
	 *
	 * @param prefix Prefix to extract from the value map
	 * @param values The value map
	 * @return Unix time for the input date time
	 * @throws UserException If the numbers fail to parse
	 */
	public static int outputDateTime(String prefix, SafeMap values, String defaulttimezone) throws UserException {
		try {
			String timezone = values.get(prefix + "timezone");
			if (timezone == null || timezone.isEmpty()) {
				timezone = defaulttimezone;
			}
			int day = Integer.parseInt(values.get(prefix + "day"));
			int month = Integer.parseInt(values.get(prefix + "month"));
			int year = Integer.parseInt(values.get(prefix + "year"));
			int hour = Integer.parseInt(values.get(prefix + "hour"));
			int minute = Integer.parseInt(values.get(prefix + "minute"));
			return UnixTime.create(timezone, day, month, year, hour, minute);
		} catch (NumberFormatException e) {
			throw new UserException("Failed to parse number : " + e.getMessage(), e);
		}
	}

	/**
	 * Convert the form interval into a duration (number of seconds)
	 *
	 * @param prefix Prefix to extract from the value map
	 * @param values The value map
	 * @return Number of seconds
	 * @throws UserException If the numbers fail to parse
	 */
	public static int outputInterval(String prefix, SafeMap values) throws UserException {
		try {
			int day = Integer.parseInt(values.get(prefix + "day"));
			int hour = Integer.parseInt(values.get(prefix + "hour"));
			int minute = Integer.parseInt(values.get(prefix + "minute"));

			day = day * 24 * 60 * 60;
			hour = hour * 60 * 60;
			minute = minute * 60;
			return day + hour + minute;
		} catch (NumberFormatException e) {
			throw new UserException("Failed to parse number : " + e.getMessage(), e);
		}
	}

	public static List<String> getTimeZones() {
		String[] tzs = TimeZone.getAvailableIDs();
		List<String> tzlist = new ArrayList<>();
		tzlist.add("America/Los_Angeles");
		for (String s : tzs) { tzlist.add(s); }
		return tzlist;
	}

	public static DropDownList getTimeZoneList(String name, String value) {
		DropDownList dropdown = new DropDownList(name);
		for (String tz : getTimeZones()) { dropdown.add(tz); }
		dropdown.setValue(value);
		return dropdown;
	}
}

