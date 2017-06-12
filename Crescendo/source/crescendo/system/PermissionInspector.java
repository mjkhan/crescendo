package crescendo.system;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import horizon.data.Dataset;

public class PermissionInspector extends Servant.Generic {
	public static final String SITE_ACCESS = "site-access";
	public static final String DENIED = "permission-denied";
	public static final CrescendoException deny = exception(null).setId(DENIED);

	public static final PermissionInspector create(Feature feature, String entryID) {
		if (isEmpty(entryID)) return null;
		Config cfg = Config.class.cast(feature.get(entryID));
		PermissionInspector inspector = cfg.inspector();
		inspector.feature = feature;
		return inspector;
	}

	protected Config cfg;

	public <T> T check(Request req, Supplier<T> granted, Supplier<T> denied, Runnable teardown) {
		boolean permitted = grants(req);
		if (teardown != null)
			teardown.run();

		if (permitted && granted != null)
			return granted.get();
		if (!permitted) {
			if (denied != null) {
				log().debug("Permission denied on " + cfg.target + (isEmpty(req.action()) ? "" : " to " + req.action()));
				return denied.get();
			}
			throw deny;
		}
		return null;
	}

	public boolean grants(Request req) {
		return sitePermits(req);
	}

	protected Principal.Generic getPrincipal(Request req) {
		return Principal.Generic.get(accountContext(req));
	}

	protected boolean adminPermits(Request req) {
		if (Principal.Generic.SYS_ADMIN.equals(getPrincipal(req)))
			return true;
		return permits(req);
	}

	protected Collection<Principal> getPrincipals(Request req) {
		return (Collection<Principal>)req.computeIfAbsent("client-principals", (key) -> accountContext(req).getPrincipals(sctx));
	}

	protected boolean sitePermits(Request req) {
		Collection<Principal> principals = getPrincipals(req);
		if (Principal.contained(principals, Principal.Generic.BLOCKED))
			return false;
		if (Principal.contained(principals, Principal.Generic.SYS_ADMIN, Principal.Generic.SITE_OWNER))
			return true;
		if (!isAccessible(principals))
			return false;

		return permits(req);
	}

	public boolean isAccessible(Collection<Principal> principals) {
		Dataset dataset = sctx.dataset(SITE_ACCESS);
		if (dataset == null)
			dataset = search(principals, SITE_ACCESS, "site", siteID(), null, null);
		return Principal.contained(principals, dataset, false);
	}

	protected boolean permits(Request req) {
		return true;
	}

	protected Dataset search(Collection<Principal> principals, String action, String actionTarget, String targetID, String pctxID, String pctxType) {
		Map<Principal.Type, List<Principal>> byType = Principal.byType(principals);
		StringBuilder buff = new StringBuilder();
		byType.forEach((type, list) -> {
			if (list.isEmpty()) return;

			String userIDs = list.stream().filter(principal -> !Principal.Generic.BLOCKED.equals(principal))
				  .map(principal -> "'" + principal.getId() + "'")
				  .collect(Collectors.joining(", "));
			if (buff.length() > 0)
				buff.append(" or ");
			buff.append("(user_type = '{type}' and user_id in ({user_id}))".replace("{type}", type.code()).replace("{user_id}", userIDs));
		});

		String select =
			"select * from {table}"
		+ "\nwhere site_id = ? and action_id = ? and action_target = ?"
		+ "\nand target_id = ? and pctx_id = ? and pctx_type = ?"
		+ "\nand ({users})"
		+ "\norder by user_type, user_id";
		log().debug(String.format("Looking up permission({action:%s, target:%s(\"%s\")})...", action, actionTarget, targetID));
		return dbAccess().query("crsnd-permission").getRecords(select.replace("{table}", cfg.table()).replace("{users}", buff.toString()), new Object[]{
			siteID(), action, actionTarget, ifEmpty(targetID, "*"), ifEmpty(pctxID, "*"), ifEmpty(pctxType, "*")
		});
	}

	protected Dataset search(Collection<Principal> principals, String action, String targetID, String pctxID, String pctxType) {
		return search(principals, action, cfg.target(), targetID, pctxID, pctxType);
	}

	protected Dataset search(Request req, String targetID, String pctxID, String pctxType) {
		return search(getPrincipals(req), req.action(), targetID, pctxID, pctxType);
	}

	protected Dataset search(Request req, String targetID) {
		return search(req, targetID, null, null);
	}

	protected boolean isSelf(Request req, Supplier<String> idSupplier) {
		if (idSupplier == null) return false;

		AccountContext actx = accountContext(req);
		return actx.isUnknown() ? false : actx.account().getId().equals(idSupplier.get());
	}

	protected static class Config extends Feature.Entry {
		private static final long serialVersionUID = 1L;
		private String
			table,
			target;
		private Class<PermissionInspector> klass;

		public String table() {
			String result = find(feature(), entry -> {
				Config cfg = (Config)entry;
				return cfg.table;
			}, () -> null);
			return ifEmpty(result, () -> "crsnd_permission");
		}

		public String target() {
			String result = find(feature(), entry -> {
				Config cfg = (Config)entry;
				return cfg.target;
			}, () -> null);
			if (isEmpty(result))
				throw new RuntimeException("empty target");
			return result;
		}

		public PermissionInspector inspector() {
			Class<PermissionInspector> klass = find(feature(), entry -> {
				Config cfg = (Config)entry;
				return cfg.klass;
			}, () -> null);
			PermissionInspector inspector = Config.instance(ifEmpty(klass, () -> PermissionInspector.class));
			inspector.cfg = this;
			return inspector;
		}

		protected static class Reader extends ComplexReader {
			@Override
			public Config value() {
				Config cfg = new Config();
				cfg.setID(id());
				cfg.table = xml.attribute(element, "table");
				cfg.target = xml.attribute(element, "target");
				cfg.klass = new Configuration.ClassReader().setElement(element).as("inspector");
				return cfg;
			}
		}
	}
}