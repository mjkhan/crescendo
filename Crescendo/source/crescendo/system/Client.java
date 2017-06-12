package crescendo.system;

import horizon.system.AbstractObject;

import java.io.Serializable;
import java.util.List;
import java.util.TimeZone;

public class Client extends AbstractObject implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final ThreadLocal<Client> current = new ThreadLocal<Client>();
	private NamedObjects values;

	public static final Client get(boolean create) {
		Client client = current.get();
		if (client == null && create)
			client = new Client().setCurrent();
		return client;
	}

	public static final Client get() {
		return get(true);
	}

	public boolean isCurrent() {
		return equals(get(false));
	}

	public Client setCurrent() {
		if (!isCurrent())
			current.set(this);
		return this;
	}

	public Client release() {
		if (isCurrent())
			current.remove();
		return this;
	}

	private static final String CLIENT = "client";

	public static final Client get(NamedObjects objs) {
		return objs != null ? Client.class.cast(objs.get(CLIENT)) : null;
	}

	public Client setTo(NamedObjects objs) {
		if (objs != null)
			objs.set(CLIENT, this);
		return this;
	}

	public Object get(String name) {
		return !isEmpty(values) ? values.get(name) : null;
	}

	public String string(String name) {
		return !isEmpty(values) ? values.string(name) : null;
	}

	protected Client set(String name, Object obj) {
		if (values == null)
			values = new NamedObjects();
		values.set(name, obj);
		return this;
	}

	public AccountContext accountContext() {
		Object obj = get(AccountContext.NAME);
		return obj != null ? AccountContext.class.cast(obj) : AccountContext.UNKNOWN;
	}

	public Client set(AccountContext actx) {
		return set(AccountContext.NAME, actx);
	}

	public boolean isUnknown() {
		AccountContext actx = accountContext();
		return actx == null || actx.isUnknown();
	}

	public String getId() {
		Account account = isUnknown() ? null : accountContext().account();
		return account == null ? null : account.getId();
	}

	public String getName() {
		Account account = isUnknown() ? null : accountContext().account();
		return account == null ? null : account.getAlias();
	}

	private static final String PROFILE = "profile";

	public String getProfile() {
		return ifEmpty(string(PROFILE), () -> "crescendo/default/client-profile.xml");
	}

	public Profile profile() {
		return Feature.load(getProfile());
	}

	public Client setProfile(String path) {
		return set(PROFILE, path);
	}

	private static final String AGENT = "agent";

	public String agent() {
		return string(AGENT);
	}

	public Client setAgent(String agent) {
		return set(AGENT, agent);
	}

	private TimeZone timezone() {
		return ifEmpty(TimeZone.class.cast(get(AccountContext.TZ)), TimeZone::getDefault);
	}

	public TimeZone getTimeZone() {
		return isUnknown() ? timezone() : accountContext().getTimeZone();
	}

	public Client setTimeZone(TimeZone zone) {
		if (isUnknown())
			return set(AccountContext.TZ, zone);
		else {
			accountContext().setTimeZone(zone);
			return set(AccountContext.TZ, null);
		}
	}

	private static final String IP_ADDR = "ip-address";

	public String getIpAddress() {
		return string(IP_ADDR);
	}

	public Client setIpAddress(String ipAddress) {
		return set(IP_ADDR, ipAddress);
	}

	public Client clear() {
		if (!isEmpty(values))
			values.clear();
		return this;
	}
	@Override
	public String toString() {
		return getClass().getName() + "(" + (isUnknown() ? Account.UNKNOWN : "\"" + accountContext().account().getId() + "\"") + ")";
	}

	public static class Profile extends Feature {
		private static final long serialVersionUID = 1L;

		public static final Profile get() {
			Client client = Client.get(false);
			return client == null ?  null : client.profile();
		}

		private List<Class<? extends AccountContextEvent>> accountContextEventClasses() {
			return getObjects("account-context-event-class");
		}

		public Profile add(Class<? extends AccountContextEvent> klass) {
			if (klass != null) {
				List<Class<? extends AccountContextEvent>> classes = accountContextEventClasses();
				if (!classes.contains(klass))
					classes.add(klass);
			}
			return this;
		}

		public Profile remove(Class<? extends AccountContextEvent> klass) {
			accountContextEventClasses().remove(klass);
			return this;
		}
	}
}