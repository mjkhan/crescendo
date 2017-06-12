package crescendo.util;

import horizon.system.AbstractObject;
import horizon.system.PropertiesConfig;

import java.util.HashMap;
import java.util.Properties;

public class StringResource extends AbstractObject {
	private static final HashMap<String, StringResource> systemStrings = new HashMap<String, StringResource>();
	private static final String SYSTEM_STRING = "crescendo/system-string";

	public static final String path(String resource, String locale) {
		return isEmpty(resource) ? null : (!isEmpty(locale) ? resource + "_" + locale : resource) + ".properties";
	}

	private static StringResource systemStringResource(String locale) {
		return systemStrings.computeIfAbsent(ifEmpty(locale, "empty-locale"), (key) -> new StringResource().setResource(SYSTEM_STRING).setLocale(!"empty-locale".equals(key) ? key : null));
	}

	private String
		locale,
		resource,
		localized;

	public StringResource setLocale(String locale) {
		this.locale = locale;
		setLocalized();
		return this;
	}

	public StringResource setResource(String resource) {
		this.resource = resource;
		setLocalized();
		return this;
	}

	private void setLocalized() {
		localized = path(resource, locale);
	}

	public Properties getResource() {
		return PropertiesConfig.getProperties(notEmpty(localized, "String resource path"));
	}

	public String string(String key) {
		return ifEmpty(ifEmpty(getResource().getProperty(key), () -> SYSTEM_STRING.equals(resource) ? key : systemStringResource(locale).string(key)), key);
	}

	public String format(String key, String... args) {
		String str = string(key);
		for (int i = 0, count = args == null ? 0 : args.length; i < count; ++i)
			str = str.replace("{" + i + "}", ifEmpty(string(args[i]), ""));
		return str;
	}
	@Override
	public String toString() {
		return String.format("%s(%s)", getClass().getName(), localized);
	}
}