package crescendo.system.usergroup;

import crescendo.system.Entity;

public final class SiteUser extends Entity {
	private static final long serialVersionUID = 1L;

	public static final String OBJ = "siteuser";
	public static final String LIST = OBJ + "-list";

	public static final String ID = "user-id";
	public static final String TYPE = "user-type";

	public static final String SEARCH = "search-user";
	public static final String ADD = "add-user";
	public static final String REMOVE = "remove-user";
	public static final String SET = "set-user";
	public static final String BLOCK = "block-user";

	private SiteUser() {}
}