package crescendo.bean.group;

import crescendo.bean.CrescendoManager;
import crescendo.system.Request;
import crescendo.system.Response;

public interface GroupManager extends CrescendoManager {
	public static GroupManager bean() {
		return new GroupManagerBean();
	}

	public static GroupManager local() {
		return Home.local(GroupManager.class);
	}

	public static GroupManager remote(String contextName) {
		return Home.remote(contextName, GroupManager.class);
	}

	public Response search(Request req);

	public Response getInfo(Request req);

	public Response viewInfo(Request req);

	public Response newInfo(Request req);

	public Response validate(Request req);

	public Response save(Request req);

	public Response move(Request req);

	public Response reorder(Request req);

	public Response setStatus(Request req);

	public Response remove(Request req);

	public static interface Remote extends GroupManager {}
}