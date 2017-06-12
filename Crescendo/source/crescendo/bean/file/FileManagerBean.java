package crescendo.bean.file;

import crescendo.bean.CrescendoManagerBean;
import crescendo.system.Request;
import crescendo.system.Response;
import crescendo.system.file.File;
import crescendo.system.file.FileServant;

public class FileManagerBean extends CrescendoManagerBean implements FileManager {
	@Override
	protected FileManager getSibling(String sitespace) throws Exception {
		return FileManager.remote(sitespace);
	}

	private final Request.Action search = (req, sctx, resp) -> resp.setAll(FileServant.create(sctx, req).search(req));
	@Override
	public Response search(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, search));
	}

	private final Request.Action getInfo = (req, sctx, resp) -> resp.setAll(FileServant.create(sctx, req).getInfo(req));
	@Override
	public Response getInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, getInfo));
	}

	private final Request.Action viewInfo = (req, sctx, resp) -> resp.setAll(FileServant.create(sctx, req).viewInfo(req));
	@Override
	public Response viewInfo(Request req) {
		return readSystem(req, Request.Action.newMap(Request.Action.READ_SITE, viewInfo));
	}

	private final Request.Action save = (req, sctx, resp) -> resp.set(FileServant.create(sctx, req).save(req));
	@Override
	public Response save(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, save));
	}

	private final Request.Action move = (req, sctx, resp) -> resp.set(FileServant.create(sctx, req).move(req));
	@Override
	public Response move(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, move));
	}
	@Override
	public Response download(Request req) {
		return null;
	}

	private final Request.Action setStatus = (req, sctx, resp) -> resp.set(FileServant.create(sctx, req).setStatus(req));
	@Override
	public Response setStatus(Request req) {
		return writeSite(req, Request.Action.newMap(Request.Action.WRITE_SITE, setStatus));
	}
	@Override
	public Response remove(Request req) {
		return setStatus(req.set(File.STATUS, File.Status.REMOVED));
	}

}