package crescendo.bean;

import crescendo.system.Crescendo;
import crescendo.system.Request;
import crescendo.system.Response;
import horizon.jndi.ObjectLocator;

public interface CrescendoManager {
	public Response getSite(Request req);

	public Response execute(Request req);

	public boolean log(Request req);

	public static class Home {
		private static <T> T lookup(String contextName, Class<?> klass, boolean local) {
			return ObjectLocator.get(contextName)
				  .lookup(Crescendo.ejbJndiNames(local).replace("{class.simple.name}", klass.getSimpleName()).replace("{class.full.name}", klass.getName()));
		}

		public static <T> T local(Class<?> klass) {
			return lookup(null, klass, true);
		}

		public static <T> T remote(String contextName, Class<?> klass) {
			return lookup(contextName, klass, false);
		}
	}
}