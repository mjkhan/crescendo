package crescendo.bean.usergroup;

import crescendo.bean.group.GroupManager;
import crescendo.system.Request;
import crescendo.system.Response;

public interface UserGroupManager extends GroupManager {
	public static UserGroupManager bean() {
		return new UserGroupManagerBean();
	}

	public static UserGroupManager local() {
		return Home.local(UserGroupManager.class);
	}

	public static UserGroupManager remote(String contextName) {
		return Home.remote(contextName, UserGroupManager.class);
	}

	public Response searchUser(Request req);

	public Response addUser(Request req);

	public Response removeUser(Request req);

	public Response setUser(Request req);

	public Response blockUser(Request req);

	public static interface Remote extends UserGroupManager {}
}