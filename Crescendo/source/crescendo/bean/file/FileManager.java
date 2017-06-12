package crescendo.bean.file;

import crescendo.bean.CrescendoManager;
import crescendo.system.Request;
import crescendo.system.Response;

public interface FileManager extends CrescendoManager {
	public static FileManager bean() {
		return new FileManagerBean();
	}

	public static FileManager local() {
		return Home.local(FileManager.class);
	}

	public static FileManager remote(String contextName) {
		return Home.remote(contextName, FileManager.class);
	}

	public Response search(Request req);

	public Response getInfo(Request req);

	public Response viewInfo(Request req);

	public Response save(Request req);

	public Response move(Request req);

	public Response download(Request req);

	public Response setStatus(Request req);

	public Response remove(Request req);

	public static interface Remote extends FileManager {}
}