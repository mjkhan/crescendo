package crescendo.bean;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import crescendo.system.AccountContext;
import crescendo.system.AccountContextEvent;
import crescendo.system.AccountContextPool;
import crescendo.system.Client;
import crescendo.system.Event;
import crescendo.system.Job;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.SiteContext;
import crescendo.system.SiteContextEvent;
import crescendo.system.SiteContextPool;
import horizon.database.DBAccess;

public abstract class CrescendoManagerBean extends CrescendoBean implements CrescendoManager {
	protected Client client() {
		return Client.get(false);
	}

	protected AccountContext acontext(Request req) {
		return accountContexts().context(req);
	}

	protected SiteContext scontext(Request req) {
		SiteContext sctx = siteContexts().context(req);
		if (sctx == null || sctx.isUnknown())
			throw SiteContext.NOT_FOUND;
		return sctx;
	}

	protected RuntimeException unsupportedOperation(String method) {
		return new UnsupportedOperationException(String.format("Implement the method %s.%s.", getClass().getName(), method));
	}

	protected CrescendoManager getSibling(String sitespace) throws Exception {
		throw unsupportedOperation("getSibling(String)");
	}

	protected DBAccess swap(DBAccess current, DBAccess next) {
		if (equals(current, next)) return current;

		if (current != null)
			current.close();
		if (next != null)
			next.open();

		return next;
	}

	protected Response readSystem(Request req, Request.Action.Map actions) {
		req.setCurrent();
		Response resp = new Response();
		try {
			boolean noActions = isEmpty(actions);
			DBAccess dbaccess = adminAccess();
			dbaccess.open();

			AccountContext actx = acontext(req);
			SiteContext sctx = scontext(req);
			Request.Action action = noActions ? null : actions.get(Request.Action.READ_ADMIN);
			if (action != null)
				action.perform(req, sctx, resp);

			dbaccess = swap(dbaccess, sctx.dbAccess());
			sctx.export(resp);
			actx.enter(sctx);

			action = noActions ? null : actions.get(Request.Action.READ_SITE);
			if (action != null)
				action.perform(req, sctx, resp);

			dbaccess.close();

			action = noActions ? null : actions.get(Request.Action.BEFORE_RESPONSE);
			if (action != null)
				action.perform(req, sctx, resp);
		} catch (Exception e) {
			resp.set(exception(e));
		}
		req.export(resp).release();
		return resp;
	}

	protected Response writeAdmin(Request req, Request.Action action) {
		req.setCurrent().setReadWrite(true);
		Response resp = new Response();
		try {
			DBAccess dbaccess = adminAccess();
			dbaccess.open();

			AccountContext actx = acontext(req);
			SiteContext sctx = scontext(req);
			if (action != null)
				action.perform(req, sctx, resp);
			dbaccess = swap(dbaccess, sctx.dbAccess());
			sctx.export(resp);
			actx.enter(sctx);

			dbaccess.close();

			propagate(Event.get(resp));
		} catch (Exception e) {
			resp.set(exception(e));
		}
		req.export(resp).release();
		return resp;
	}

	protected Response writeSite(Request req, Request.Action.Map actions) {
		req.setCurrent().setReadWrite(true);
		Response resp = new Response();
		try {
			boolean noActions = isEmpty(actions);
			DBAccess dbaccess = adminAccess();
			dbaccess.open();

			AccountContext actx = acontext(req);
			SiteContext sctx = scontext(req);
			Request.Action action = noActions ? null : actions.get(Request.Action.READ_ADMIN);
			if (action != null)
				action.perform(req, sctx, resp);

			dbaccess = swap(dbaccess, sctx.dbAccess());
			sctx.export(resp);
			actx.enter(sctx);

			action = noActions ? null : actions.get(Request.Action.WRITE_SITE);
			if (action != null)
				action.perform(req, sctx, resp);

			dbaccess.close();

			action = noActions ? null : actions.get(Request.Action.BEFORE_RESPONSE);
			if (action != null)
				action.perform(req, sctx, resp);

			propagate(Event.get(resp));
		} catch (Exception e) {
			resp.set(exception(e));
		}
		req.export(resp).release();
		return resp;
	}

	protected void propagate(Event evt) {
		if (evt == null || !evt.isFired()) return;

		Function<Event, Class<? extends Event>> eventContext = e -> e instanceof AccountContextEvent ? AccountContextEvent.class : SiteContextEvent.class;
		Map<Class<? extends Event>, List<Event>> evtsByContext = evt.expand().stream().collect(Collectors.groupingBy(eventContext));

		AccountContextPool.get().propagate(evtsByContext.get(AccountContextEvent.class));
		SiteContextPool.get().propagate(evtsByContext.get(SiteContextEvent.class));
		Job.send(null, evt.removeJobs());
	}
	@Override
	public Response getSite(Request req) {
		return readSystem(req, null);
	}
	@Override
	public Response execute(Request req) {
		throw unsupportedOperation("execute(" + Request.class.getName() + ")");
	}
	@Override
	public boolean log(Request req) {
		throw unsupportedOperation("log(" + Request.class.getName() + ")");
	}
}