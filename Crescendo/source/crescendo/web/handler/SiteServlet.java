package crescendo.web.handler;

import crescendo.web.CrescendoHandler;
import crescendo.web.CrescendoServlet;

public class SiteServlet extends CrescendoServlet {
	private static final long serialVersionUID = 1L;
	@Override
	protected CrescendoHandler getHandler(String name) {
		if (AccountHandler.name().equals(name))
			return new AccountHandler();
		if (SiteHandler.name().equals(name))
			return new SiteHandler();
		if (UserGroupHandler.name().equals(name))
			return new UserGroupHandler();
		if (GroupHandler.name().equals(name))
			return new GroupHandler();
		return super.getHandler(name);
	}
}