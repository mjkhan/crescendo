package crescendo.web;

import horizon.system.AbstractObject;
import horizon.util.Xmlement;

import java.util.HashMap;

import org.w3c.dom.Element;

import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.SiteContext;

public class ResponseMapper extends AbstractObject {
	private static final ResponseMapper EMPTY = new ResponseMapper();
	private static final HashMap<String, ResponseMapper> mappers = new HashMap<String, ResponseMapper>();

	public static final ResponseMapper get(String path) {
		return isEmpty(path) ? null : mappers.computeIfAbsent(path, Loader::get);
	}

	public static final ResponseMapper get(Response resp) {
		Request req = Request.get(resp);
		SiteContext sctx = SiteContext.class.cast(resp.get(req.currentSite()));
		String path = sctx.site().uiContext();
		return ifEmpty(get(path), EMPTY);
	}

	private String
		configPath,
		context,
		pathParam,
		keyParam;
	private HashMap<String, Entry> entries;

	public String context() {
		return context;
	}

	public String pathParam() {
		return pathParam;
	}

	public String keyParam() {
		return keyParam;
	}

	public CrescendoHandler.Result map(String handler, String action, CrescendoHandler.Result result) {
		if (!isEmpty(entries)) {
			Entry entry = ifEmpty(entries.get(Entry.key(handler, action)), () -> entries.get(Entry.key(handler,  "*")));
			if (entry == null)
				throw new RuntimeException(String.format("Response entry not found for \"%s\" handler's \"%s\" action in %s", handler, action, configPath));
			entry.set(result);
		}
		return result;
	}

	private static class Entry {
		private static String key(String handler, String action) {
			return String.format("%s.%s", handler, action);
		}

		CrescendoHandler.Result.Method method;
		String path;

		void set(CrescendoHandler.Result result) {
			result.setPath(path).setMethod(method);
		}
	}

	private static class Loader {
		private static final Xmlement xml = new Xmlement();

		static ResponseMapper get(String path) {
			try {
				Element doc = xml.getDocument(path);
				ResponseMapper mapper = new ResponseMapper();
				mapper.configPath = path;
				mapper.context = xml.attribute(doc, "context");
				mapper.pathParam = xml.attribute(doc, "path-param");
				mapper.keyParam = xml.attribute(doc, "key-param");

				for (Element request: xml.getChildren(doc, "request")) {
					String handler = notEmpty(xml.attribute(request, "handler"), "handler");
					for (Element response: xml.getChildren(request, "response")) {
						String method = ifEmpty(xml.attribute(response, "method"), "forward").toUpperCase(),
							   respath = ifEmpty(response.getTextContent(), "").trim();
						String[] actions = notEmpty(xml.attribute(response, "key"), "key").split(",");
						for (String action: actions) {
							if (mapper.entries == null)
								mapper.entries = new HashMap<String, ResponseMapper.Entry>();
							String key = Entry.key(handler, action.trim());
							Entry entry = new Entry();
							entry.method = CrescendoHandler.Result.Method.valueOf(method);
							entry.path = respath;
							Entry prev = mapper.entries.put(key, entry);
							if (prev != null)
								throw new RuntimeException(String.format("Duplicate response entry for \"%s\" handler's \"%s\" action in %s", handler, action, path));
						}
					}
				}
				return mapper;
			} catch (Exception e) {
				throw runtimeException(e);
			}
		}
	}
}