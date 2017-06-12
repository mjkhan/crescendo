package crescendo.system;

public class Crescendo extends Feature {
	private static final long serialVersionUID = 1L;

	private static final Crescendo cfg = new Crescendo();

	private String
		sysAdminAttribute,
		adminConnection,
		readonlyAdminConnection,
		localEjbJndiNames,
		remoteEjbJndiNames;

	private Crescendo() {
		setup();
	}

	public static final Crescendo get() {
		return cfg;
	}

	public static final String systemAdministratorAttribute() {
		return cfg.sysAdminAttribute;
	}

	public static final int accountPoolSize() {
		return cfg.number("account-pool-size").intValue();
	}

	public static final int sitePoolSize() {
		return cfg.number("site-pool-size").intValue();
	}

	public static final String adminConnection(boolean readWrite) {
		if (readWrite)
			return cfg.adminConnection;
		return ifEmpty(cfg.readonlyAdminConnection, () -> cfg.adminConnection);
	}

	public static final String ejbJndiNames(boolean local) {
		return local ? cfg.localEjbJndiNames : cfg.remoteEjbJndiNames;
	}

	public static final int siteObjectsInAccountContext() {
		return cfg.number("site-objects-in-account-context").intValue();
	}

	private void setup() {
		String path = "crescendo/crescendo.xml";
		Feature feature = Feature.load(path);
		sysAdminAttribute = String.class.cast(feature.remove("sys-admin-attr"));

		adminConnection = String.class.cast(feature.remove("admin-connection"));
		readonlyAdminConnection = String.class.cast(feature.remove("admin-connection-readonly"));

		AccountContextPool.configure(feature);
		SiteContextPool.configure(feature);

		localEjbJndiNames = String.class.cast(feature.remove("local-ejb-jndi-names"));
		remoteEjbJndiNames = String.class.cast(feature.remove("remote-ejb-jndi-names"));

		putAll(feature);
		Feature.unload(path);
	}
}