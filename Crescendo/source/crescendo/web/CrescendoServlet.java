package crescendo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import crescendo.system.Client;
import horizon.system.Log;
import horizon.web.HorizonServlet;
import horizon.web.Kookie;
import horizon.web.RequestParameter;

public abstract class CrescendoServlet extends HorizonServlet {
	private static final long serialVersionUID = 1L;
	@Override
	protected void service(RequestParameter param, HttpServletResponse hresp) throws ServletException, IOException {
		RequestMapper rmapper = getMapper(param);
		if (!rmapper.isUrlValid()) return;

		param.logParams();
		Kookie kookie = Kookie.get(param.request(), hresp);
		Client client = rmapper.getClient();
		CrescendoHandler handler = null;
		CrescendoHandler.Result result = null;

		try {
			beforeService(param, hresp);
			handler = (CrescendoHandler)getHandler(rmapper.handler())
					 .set(client)
					 .setConfig(getServletConfig())
					 .setParam(param)
					 .setResponse(hresp);
			String method = param.request().getMethod();
			param.request().setAttribute("method", method);
			switch (method) {
			case "GET": result = handler.doGet(); break;
			case "PUT": result = handler.doPut(); break;
			case "POST": result = handler.doPost(); break;
			case "DELETE": result = handler.doDelete(); break;
			default: break;
			}
		} catch (Exception e) {
			result = onException(param, e);
			if (result == null)
				throw servletException(e);
		}

		switch (result.getMethod()) {
		case INCLUDE : include(param, result, hresp); break;
		case FORWARD : forward(param, result, hresp); break;
		case REDIRECT: redirect(result.getPath(), hresp); break;
		default: break;
		}
		if (handler != null)
			handler.clear();
		if (client != null)
			client.clear();
		kookie.clear();
		param.clear();
	}

	protected RequestMapper getMapper(RequestParameter param) {
		RequestMapper mapper = RequestMapper.get(param.request());
		if (mapper == null)
			mapper = RequestMapper.create(this::initParam)
					.setTo(param.request());
		return mapper.set(param);
	}

	protected void beforeService(RequestParameter param, HttpServletResponse hresp) {}

	protected CrescendoHandler getHandler(String name) {
		throw CrescendoHandler.NotFound.get();
	}

	protected String forwardContext(RequestParameter param, CrescendoHandler.Result result) {
		String fctx = result.contextName();
		if (isEmpty(fctx)
		 || param.request().getContextPath().equals(fctx)) return null;
		return fctx;
	}

	protected void beforeForward(RequestParameter param) {}

	protected void include(RequestParameter param, CrescendoHandler.Result result, HttpServletResponse hresp) throws ServletException, IOException {
		param.request().getRequestDispatcher(result.getPath()).include(param.request(), hresp);
		log().debug(() -> "INCLUDEs " + result.getPath());
	}

	protected void forward(RequestParameter param, CrescendoHandler.Result result, HttpServletResponse hresp) throws ServletException, IOException {
		if (hresp.isCommitted()) return;

		String fctx = forwardContext(param, result),
			   path = result.getPath();
		beforeForward(param);
		HttpServletRequest hreq = param.request();
		if (isEmpty(fctx))
			hreq.getRequestDispatcher(path).forward(hreq, hresp);
		else getServletContext()
			.getContext("/" + fctx)
			.getRequestDispatcher(path)
			.forward(hreq, hresp);
		log().debug(() -> "FORWARDed to " + ifEmpty(fctx, "") + path);
	}

	protected void redirect(String path, HttpServletResponse hresp) throws IOException {
		if (hresp.isCommitted()) return;
		hresp.sendRedirect(path);
		log().debug(() -> "REDIRECTed to " + path);
	}

	protected CrescendoHandler.Result onException(RequestParameter param, Exception e) {
		param.print();
		e.printStackTrace();
		return null;
	}

	protected Log log() {
		return Log.get(getClass());
	}
}