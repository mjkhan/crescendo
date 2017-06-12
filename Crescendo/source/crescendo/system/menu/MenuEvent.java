package crescendo.system.menu;

import java.util.Map;

import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.SiteContextEvent;

public class MenuEvent extends SiteContextEvent {
	private static final long serialVersionUID = 1L;

	public static final String MOVE = Menu.MOVE;
	public static final String REORDER = Menu.REORDER;
	public static final String CHANGE_STATUS = Menu.CHANGE_STATUS;

	public static final <T extends MenuEvent> T create(Feature feature) {
		return create(feature, Menu.OBJ + "-event");
	}
	@Override
	public MenuEvent on() {
		switch (type()) {
		case LOAD_SCTX:
		case CREATE:
		case UPDATE:
		case MOVE:
		case REORDER:
		case REMOVE:
		case CHANGE_STATUS:
			loadMenus();
		}
		super.on();
		return this;
	}

	protected void loadMenus() {
		Request req = request();
		String menuType = req != null ? req.string(Menu.TYPE) : null;
		MenuServant servant = MenuServant.create(siteContext(), menuType);
		Map<String, Menu.Tree<Menu>> menus = servant.getMenuTree();
		menus.forEach((key, value) -> setLoad(key + "-menus", value));
	}
}