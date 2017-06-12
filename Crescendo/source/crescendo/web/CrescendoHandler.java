package crescendo.web;

import horizon.web.Kookie;
import horizon.web.RequestHandler;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import crescendo.bean.CrescendoManager;
import crescendo.system.Client;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.Session;
import crescendo.system.Site;

public abstract class CrescendoHandler extends RequestHandler {
	@FunctionalInterface
	public static interface ResultProducer {
		public Result get(Request req) throws Exception;
	}

	protected Request req;
	protected Client client;
	protected HashMap<String, ResultProducer> defaultActions;

	protected CrescendoHandler() {}

	protected CrescendoHandler(CrescendoHandler other) {
		super(other);
	}

	public abstract String getName();
	@Override
	public CrescendoHandler init(RequestHandler other) {
		super.init(other);
		if (other instanceof CrescendoHandler) {
			CrescendoHandler handler = CrescendoHandler.class.cast(other);
			handler.req = req;
			handler.client = client;
		}
		return this;
	}

	public CrescendoHandler set(Client client) {
		this.client = client;
		return this;
	}

	protected abstract CrescendoManager manager() throws Exception;

	public Result getSite(Request req) throws Exception {
		return respond(
			manager().getSite(ifEmpty(req, () -> request()).set(Site.ID, requestMapper().siteID()))
		);
	}

	private static final String HANDLER = "handler";

	protected Request newRequest() {
		req = requestMapper().createRequest()
			 .set(HANDLER, getName());
    	if (client != null)
    		client.setTo(req);
    	return req;
    }

	protected Request request() {
		return ifEmpty(req, this::newRequest);
    }

	private RequestMapper requestMapper() {
		return RequestMapper.get(param.request());
	}

	protected String sitespace() {
		return requestMapper().sitespace();
	}

	protected String thisSite() {
		return requestMapper().siteID();
	}

	protected Kookie kookie() {
		return Kookie.get(param.request(), hresp);
	}

	public ResultProducer defaultAction(String method) {
		return isEmpty(defaultActions) ? null : defaultActions.get(method);
	}

	public CrescendoHandler setDefaultAction(String method, ResultProducer action) {
		if (defaultActions == null)
			defaultActions = new HashMap<String, ResultProducer>();
		defaultActions.put(method, action);
		return this;
	}

	public Result doDefault(String method, ResultProducer def) throws Exception {
		ResultProducer action = defaultAction(method);
		if (action == null)
			action = def;
		if (action != null)
			return action.get(null);
		throw unknownAction();
	}

	public Result doDefault(String method) throws Exception {
		return doDefault(method, null);
	}

	private static final String
		ACTION = "action",
		FAILED = "actionFailed";

	protected void set(Response resp) {
		boolean failed = resp.isFailed();

		param.request().setAttribute(HANDLER, getName());
		param.request().setAttribute(ACTION, req.action());
		param.request().setAttribute(FAILED, Boolean.valueOf(failed));

		resp.forEach((key, value) -> param.request().setAttribute(key, value));
		requestMapper().setFootprint(Session.get(Client.get(Request.get(resp))));
	}

	private static final String RESP_MAP = "response-map";

	protected Result send(Response resp) {
		ResponseMapper mapper = ifEmpty(ResponseMapper.get(initParam(RESP_MAP)), () -> ResponseMapper.get(resp));
		result().setContextName(mapper.context());
		String dest = param.getValue(ifEmpty(mapper.pathParam(), ""));
		if (!isEmpty(dest))
			return forward(dest);

		String key = param.getValue(ifEmpty(mapper.keyParam(), ""), () -> req.action());
		return mapper.map(getName(), key, result);
	}

	protected Result respond(Response resp) {
		set(resp);
		return send(resp);
	}

	protected static String encode(String str) {
		try {
			return !isEmpty(str) ? URLEncoder.encode(str, "UTF-8") : str;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	protected static String decode(String str) {
		try {
			return !isEmpty(str) ? URLDecoder.decode(str, "UTF-8") : str;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	protected static final RuntimeException unknownAction() {
		return new RuntimeException("Unable to determine the requested action");
	}
}