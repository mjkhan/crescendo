package crescendo.system;

import horizon.system.AbstractObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public interface Effect extends Serializable {
	public Request request();

	public void set(Request req);

	public String destination();

	public static class Implementation extends NamedObjects implements Effect {
		private static final long serialVersionUID = 1L;
		@Override
		public Request request() {
			return Request.get(this);
		}
		@Override
		public void set(Request req) {
			if (req != null)
				req.setTo(this);
		}

		private static final String DEST = "__dest-config__";
		@Override
		public String destination() {
			return string(DEST);
		}

		public void setDestination(String destination) {
			set(DEST, destination);
		}
	}

	public static class Support extends AbstractObject {
		protected static <T extends Effect> HashMap<String, ArrayList<T>> splitByConfig(Iterable<T> objs) {
			HashMap<String, ArrayList<T>> result = new HashMap<>();
			for (T obj: objs) {
				if (obj == null) continue;
				String configName = ifEmpty(obj.destination(), "");
				ArrayList<T> list = result.get(configName);
				if (list == null)
					result.put(configName, list = new ArrayList<T>());
				if (!list.contains(obj))
					list.add(obj);
			}
			return result;
		}
	}
}