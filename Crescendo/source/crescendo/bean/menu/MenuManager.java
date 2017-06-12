package crescendo.bean.menu;

import crescendo.bean.CrescendoManager;
import crescendo.system.Request;
import crescendo.system.Response;

public interface MenuManager extends CrescendoManager {
	public static MenuManager bean() {
		return new MenuManagerBean();
	}

	public static MenuManager local() {
		return Home.local(MenuManager.class);
	}

	public static MenuManager remote(String contextName) {
		return Home.remote(contextName, MenuManager.class);
	}

	public Response search(Request req);

	public Response getInfo(Request req);

	public Response newInfo(Request req);

	public Response validate(Request req);

	public Response save(Request req);

	public Response move(Request req);

	public Response reorder(Request req);

	public Response setStatus(Request req);

	public Response remove(Request req);

	public static interface Remote extends MenuManager {}
}