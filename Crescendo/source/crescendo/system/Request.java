package crescendo.system;

import java.util.HashMap;
import java.util.Map;

public class Request extends NamedObjects {
	private static final long serialVersionUID = 1L;
	private static final ThreadLocal<Request> current = new ThreadLocal<Request>();

	public static final Request get(boolean create) {
		Request req = current.get();
		if (req == null && create)
			req = new Request().setCurrent();
		return req;
	}

	public static final Request get() {
		return get(true);
	}

	public boolean isCurrent() {
		return equals(get(false));
	}

	public Request setCurrent() {
		if (!isCurrent()) {
			current.set(this);
			Client client = Client.get(this);
			if (client != null)
				client.setCurrent();
		}
		return this;
	}

	public Request release() {
		if (isCurrent()) {
			current.remove();
			Client client = Client.get(this);
			if (client != null)
				client.release();
		}
		return this;
	}

	private static final String REQ = "request";

	public static final Request get(NamedObjects objs) {
		return objs == null ? null :
			   objs instanceof Request ? Request.class.cast(objs) :
			   Request.class.cast(objs.get(REQ));
	}

	public Request setTo(NamedObjects objs) {
		if (objs != null
		 && objs != this)
			objs.set(REQ, this);
		return this;
	}
	@Override
	public Request set(String name, Object obj) {
		super.set(name, obj);
		return this;
	}
	@Override
	public Request setAll(Map<String, ? extends Object> objs) {
		super.setAll(objs);
		return this;
	}

	private static final String PATH = "path";

	public String path() {
		return string(PATH);
	}

	public Request setPath(String path) {
		return set(PATH, path);
	}

	private static final String ACTION = "action";

	public String action() {
		return ifEmpty(string(ACTION), () -> "");
	}

	public Request setAction(String action) {
		return set(ACTION, action);
	}

	public Request defaultAction(String action) {
		if (isEmpty(action()))
			setAction(action);
		return this;
	}

	private static final String READWRITE = "readWrite";

	public boolean isReadWrite() {
		return Boolean.TRUE.equals(get(READWRITE));
	}

	public Request setReadWrite(boolean readWrite) {
		return set(READWRITE, Boolean.valueOf(readWrite));
	}

	public Request ensureReadWrite() {
		if (!isReadWrite())
			throw new RuntimeException("The Request is not for a read-write action.");
		return this;
	}

	public String sitespace() {
		return string(Site.SPACE);
	}

	public Request setSitespace(String sitespace) {
		return set(Site.SPACE, sitespace);
	}

	public String currentSite() {
		return string(Site.CURRENT);
	}

	public Request setCurrentSite(String siteID) {
		return set(Site.CURRENT, siteID);
	}

	public Request export(Response resp) {
		setTo(resp);
		//TODO:Is this necessary?
		resp.set(ACTION, action());
		Client client = Client.get(this);
		if (client != null) {
			client.setTo(resp);
//			if (!client.isUnknown()) {
				AccountContext actx = client.accountContext();
				if (actx != null)
					actx.export(resp);
//			}
		}
		return this;
	}
	@FunctionalInterface
	public static interface Action {
		public static final String READ_ADMIN = "read-admin";
		public static final String WRITE_ADMIN = "write-admin";
		public static final String READ_SITE = "read-site";
		public static final String WRITE_SITE= "write-site";
		public static final String BEFORE_RESPONSE = "before-response";

		public void perform(Request req, SiteContext sctx, Response resp) throws Exception;

		public static class Map extends HashMap<String, Action> {
			private static final long serialVersionUID = 1L;

			public Map set(String key, Action action) {
				put(key, action);
				return this;
			}
		}

		public static Map newMap(String key, Action action) {
			return new Map().set(key, action);
		}
	}
}