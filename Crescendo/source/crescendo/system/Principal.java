package crescendo.system;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import crescendo.util.StringResource;
import horizon.data.Dataset;

public interface Principal {
	public static final String ID = "principal-id";
	public static final String OBJS = "principals";
	public static final CrescendoException unknown = CrescendoException.create("unknown principal").setId("unknown-principal");

	public static Principal find(Iterable<Principal> principals, String principalID) {
		if (principals != null)
			for (Principal principal: principals)
				if (principalID.equals(principal.getId()))
					return principal;
		return null;
	}

	public static boolean contained(Iterable<Principal> principals, String principalID) {
		return find(principals, principalID) != null;
	}

	public static boolean contained(Collection<Principal> principals, Principal... prins) {
		if (principals != null && prins != null)
			for (Principal p: prins)
				if (principals.contains(p))
					return true;
		return false;
	}

	public static Map<Type, List<Principal>> byType(Collection<Principal> principals) {
		return principals.stream().collect(Collectors.groupingBy(Principal::getPrincipalType));
	}

	public static boolean contained(Collection<Principal> principals, Dataset dataset, boolean whenEmpty) {
		if (principals == null || principals.isEmpty() || dataset == null || dataset.isEmpty())
			return whenEmpty;

		for (Principal p: principals)
			if (p.contained(dataset, false)) return true;
		return false;
	}

	public default boolean contained(Dataset dataset, boolean whenEmpty) {
		if (dataset == null || dataset.isEmpty())
			return whenEmpty;
		String type = getPrincipalType().code();

		for (int i = 0, count = dataset.size(); i < count; ++i) {
			String userType = dataset.string(i, "user_type");
			if (Type.ALL.code().equals(userType)) return true;

			if (!type.equals(userType)) continue;

			String userID = dataset.string(i, "user_id");
			if (Generic.EVERYONE.getId().equals(userID) || getId().equals(userID)) return true;
		}
		return false;
	}

	public static enum Type implements Codified {
		ALL("*"),
		ACCOUNT("000"),
		USER_GROUP("001"),
		IP_ADDRESS("002"),
		DEVICE_ID("003");

		private final String code;

		private Type(String code) {
			this.code = code;
		}

		public static Type codeOf(String code) {
			return Codified.codeOf(values(), code);
		}

		public static String displayName(String code, StringResource res) {
			Type type = codeOf(code);
			return type != null ? type.displayName(res) : null;
		}
		@Override
		public String code() {
			return code;
		}
	}

	public Type getPrincipalType();

	public String getId();

	public String getName();

	public static enum Generic implements Principal {
		/**Everyone, including those not logged into the system*/
		EVERYONE(Type.ALL, "*", "everyone"),
		/**Users logged into the system*/
		LOGGED_IN(Type.USER_GROUP, "000", "logged-in"),
		/**Logged-in users but not assigned to any of the current site's user groups*/
		GUEST(Type.USER_GROUP, "001", "guest"),
		/**Logged-in user that owns the current site*/
		SITE_OWNER(Type.ACCOUNT, "002", "site owner"),
		/**Logged-in users assigned to one or more user groups of the current site*/
		SITE_USER(Type.USER_GROUP, "003", "site user"),
		SYSTEM(Type.USER_GROUP, "997", "system"),
		/**System administrator or super user*/
		SYS_ADMIN(Type.USER_GROUP, "998", "system administrator"),
		/**Logged-in users that are blocked from the current site*/
		BLOCKED(Type.USER_GROUP, "999", "blocked");

		public static final Collection<Principal> reserved = Arrays.asList(EVERYONE, LOGGED_IN, GUEST, SITE_OWNER, SITE_USER, SYSTEM, SYS_ADMIN, BLOCKED);

		public static final String IDs() {
			return reserved.stream().map(group -> "'" + group.getId() + "'").collect(Collectors.joining(", "));
		}

		public static final boolean contains(String id) {
			for (Principal principal: reserved)
				if (principal.getId().equals(id)) return true;
			return false;
		}

		public static final boolean isBlocked(String groupID) {
			return BLOCKED.getId().equals(groupID);
		}

		public static final Generic get(AccountContext actx) {
			return actx.isUnknown() ? EVERYONE :
				   actx.account().isSystemAdministrator() ? SYS_ADMIN : LOGGED_IN;
		}

		private final Type type;
		private final String
			id,
			name;

		private Generic(Type type, String id, String name) {
			this.type = type;
			this.id = id;
			this.name = name;
		}
		@Override
		public Type getPrincipalType() {
			return type;
		}
		@Override
		public String getId() {
			return id;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public String toString() {
			return getClass().getName() + "." + super.toString();
		}
	}
}