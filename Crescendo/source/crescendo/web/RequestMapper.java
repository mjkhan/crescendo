package crescendo.web;

import horizon.system.AbstractObject;
import horizon.web.Kookie;
import horizon.web.RequestParameter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import crescendo.system.Client;
import crescendo.system.Feature;
import crescendo.system.Request;
import crescendo.system.Session;
import crescendo.util.StringEncryptor;

public class RequestMapper extends AbstractObject {
	private static final String CFG = "request-mapper";
	private static final String NAME = "__" + CFG + "__";

	public static final RequestMapper get(HttpServletRequest hreq) {
		return RequestMapper.class.cast(hreq.getAttribute(NAME));
	}

	static final RequestMapper create(Function<String, String> initParam) {
		Class<? extends RequestMapper> klass = Feature.classOf(initParam.apply(CFG));
		RequestMapper mapper = ifEmpty(Feature.instance(klass), () -> new RequestMapper());
		mapper.sitespace = initParam.apply("default-sitespace");
		mapper.siteID = notEmpty(initParam.apply("default-site-id"), "default-site-id");
		mapper.handler = notEmpty(initParam.apply("default-handler"), "default-handler");
		mapper.initParam = initParam;
		return mapper;
	}

	RequestMapper setTo(HttpServletRequest hreq) {
		hreq.setAttribute(NAME, this);
		return this;
	}

	protected RequestParameter param;
	protected String
		url,
		sitespace,
		siteID,
		handler;
	protected List<String> segments;	/* host/context/servlet/siteID/handler */
	protected Function<String, String> initParam;
	private StringEncryptor encryptor;

	protected StringEncryptor encryptor() {
		return encryptor != null ? encryptor : (encryptor = new StringEncryptor());
	}

	public RequestMapper set(RequestParameter param) {
		if (!equals(this.param, param)) {
			this.param = param;
			setUrl(param.request().getRequestURL().toString());
			parse();
		}
		return this;
	}

	private void setUrl(String s) {
		int begin = s.indexOf("://");
		if (begin > -1)
			begin += 3;
		url = begin > 0 ? s.substring(begin) : s;
	}

	public boolean isUrlValid() {
		return true;
	}

	protected void parse() {
		if (!isUrlValid()) return;
		segments = Arrays.asList(url.split("/"));
	};

	public String sitespace() {
		return sitespace;
	}

	public String siteID() {
		return segments.size() > 3 ? segments.get(3) : siteID;
	}

	public String handler() {
		return segments.size() > 4 ? segments.get(4) : handler;
	}

	protected Kookie kookie() {
		return Kookie.get(param.request(), null);
	}

	protected Request createRequest() {
		return new Request().setPath(url).setSitespace(sitespace()).setCurrentSite(siteID());
	}

	private static final String SESSION_TOKEN = "session-token-cookey";

	private String idKey() {
		return ifEmpty(initParam.apply(SESSION_TOKEN), Session.OBJ);
	}

	protected Session getSession() {
		String sessionToken = kookie().getValue(idKey());
		if (!isEmpty(sessionToken))
			return Session.parse(encryptor().decrypt(sessionToken));
		return null;
	}

	public Client getClient() {
		Client client = new Client()
		  .setAgent(param.request().getHeader("User-Agent"))
		  .setProfile(initParam.apply("client-profile"))
		  .setIpAddress(param.request().getRemoteAddr());
		Session session = getSession();
		if (session != null)
			session.setTo(client);
		return client;
/*
	see: https://developer.mozilla.org/en-US/docs/Browser_detection_using_the_user_agent
	see also: https://code.google.com/p/mobileesp/source/browse/Java/UAgentInfo.java
*/
	}

	public void setFootprint(Session session) {
		if (session != null && !session.isExpired()) {
			boolean remember = session.isPermanent();
			String key = idKey(),
				   sessionToken = encryptor().encrypt(session.string());
			if (remember) {
				kookie().longSave(key, sessionToken)
						.longSave(key + "-remembered", "true");
			} else {
				kookie().shortSave(key, sessionToken);
			}
		} else
			clearFootprint();
	}

	public void clearFootprint() {
		String key = idKey();
		kookie().remove(key)
				.remove(key + "-remembered");
		HttpSession httpSession = param.request().getSession(false);
		if (httpSession != null)
			httpSession.invalidate();
	}
}