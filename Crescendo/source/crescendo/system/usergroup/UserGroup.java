package crescendo.system.usergroup;

import crescendo.system.Principal;
import crescendo.system.group.Group;

public class UserGroup extends Group implements Principal {
	private static final long serialVersionUID = 1L;

	public static final String OBJ = "usergroup";
	public static final String OBJS = OBJ + "s";
	public static final String INFO = OBJ + "-info";
	public static final String LIST = OBJ + "-list";
	public static final String ID = OBJ + "-id";

	public static final String NAME = OBJ + "-name";
	@Override
	public Type getPrincipalType() {
		return Type.USER_GROUP;
	}
}