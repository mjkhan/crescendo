package crescendo.system.file;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import crescendo.system.Account;
import crescendo.system.AccountContext;
import crescendo.system.Feature;
import crescendo.system.NamedObjects;
import crescendo.system.PermissionInspector;
import crescendo.system.Request;
import crescendo.system.Servant;
import crescendo.system.SiteContext;
import horizon.data.Dataset;
import horizon.util.FileSupport;

public class FileServant extends Servant.Generic {
	public static FileServant create(SiteContext sctx, String featureID) {
		return create(sctx, Feature.get(sctx, featureID), "file-servant");
	}

	public static FileServant create(SiteContext sctx, Request req) {
		String featureID = req.notEmpty(Feature.ID);
		FileServant servant = create(sctx, featureID);
		if (!isEmpty(servant.feature.string("owner-type")))
			servant.setOwnerID(req.string("owner-id"));
		return servant;
	}

	private String ownerID;

	protected String fileType() {
		return feature.string(File.TYPE);
	}

	public FileServant setOwnerID(String ownerID) {
		this.ownerID = notEmpty(ownerID, "ownerID");
		return this;
	}

	protected File.Config config() {
		return File.Config.get(feature, File.OBJ);
	}

	protected Filebase filebase() {
		return Filebase.get(feature);
	}
	@Override
	protected PermissionInspector permission() {
		return permission("file-permission");
	}
	@Override
	public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
		String precondition = "site_id = ? and file_type = ?";
		List<Object> argList = qsupport.toList(args);
		argList.add(0, siteID());
		argList.add(1, fileType());
		if (!isEmpty(ownerID)) {
			precondition += " and ins_id = ?";
			argList.add(2, ownerID);
		}
		Filebase filebase = filebase();
		String table = config().table(),
			   columns = "a.*,"
			   + "\n    case when status not in ('" + File.Status.INACTIVE.code() + "', '" + File.Status.REMOVED.code() + "') then\n        case {dirs}\n    else case {stash}\n    end dir,"
			   + "\n    case when status not in ('" + File.Status.INACTIVE.code() + "', '" + File.Status.REMOVED.code() + "') then\n        case {url-pfx}\n    else case {stash-pfx}\n    end url_prefix";
	   	columns = columns.replace("{dirs}", filebase.dirs()).replace("{stash}", filebase.stashes())
	   			.replace("{url-pfx}", filebase.urlPrefixes()).replace("{stash-pfx}", filebase.stashPrefixes())
	   			.replace("{site-id}", siteID());
		String from = table + " a";
		if (!isEmpty(ownerID))
			columns = columns.replace("{user-id}", ownerID);
		condition = precondition + "\nand " + condition;
		return sqlDialect("file-dialect")
			  .select("search-" + table, columns, from, condition, orderBy, argList, start, fetch);
	}

	public NamedObjects search(Request req) {
		return permission().check(req.defaultAction(File.SEARCH), () -> {
			String condition = ifEmpty(req.string(File.CONDITION), ""),
				   terms = qsupport.terms().condition(feature.string("file-search"), req.string(File.TERMS)),
				   orderBy = ifEmpty(req.string(File.ORDER), "file_id desc");

			if (!isEmpty(terms))
				condition = "".equals(condition) ? terms : condition + "\nand " + terms;
			String statusCondition = File.Status.condition(req.string(File.STATUS));
			if (!isEmpty(statusCondition)) {
				statusCondition = "status " + statusCondition;
				condition = isEmpty(condition) ? statusCondition : condition + "\nand " + statusCondition;
			}

			Object[] args = req.objects(File.ARGS);
			int fetch = req.number(File.FETCH).intValue(),
				start = req.number(File.START).intValue();
			return new NamedObjects().set(File.LIST, search(condition, orderBy, start, fetch, args));
			}, null, null
		);
	}

	public Dataset getInfo(String fieldName, Object[] values) {
		String condition = qsupport.fieldCondition(fieldName, values) + " and status " + File.Status.condition("none");
		return search(condition, qsupport.parameterize(values) ? values : null);
	}

	public Dataset getInfo(String... fileIDs) {
		return getInfo("file_id", fileIDs);
	}

	public NamedObjects getInfo(Request req) {
		return dbAccess().open(dbaccess -> {
			String fileID = req.notEmpty(File.ID);
			Dataset dataset = getInfo(fileID);
			NamedObjects result = new NamedObjects().set(File.INFO, dataset);
			if (dataset.isEmpty()) return result;

			return permission().check(req.defaultAction(File.GET).set(File.INFO, dataset),
				() -> result, null, () -> req.remove(File.INFO)
			);
		}, null);
	}

	public <T extends File> T getFile(String fileID) {
		Dataset fileset = getInfo(fileID);
		return !fileset.isEmpty() ? File.Factory.create(config(), fileset.get(0)) : null;
	}

	public NamedObjects viewInfo(Request req) {
		return permission().check(req.defaultAction(File.VIEW), () -> {
			String fileID = req.notEmpty(File.ID);
			Dataset dataset = getInfo(fileID);
				return new NamedObjects().set(File.INFO, dataset);
			}, null, null
		);
	}

	protected FileEvent event() {
		FileEvent evt = FileEvent.create(feature, "file-event");
		evt.set(sctx);
		return evt;
	}

	public int create(File file) {
		file.setSiteID(siteID());
		file.setType(fileType());
		//TODO: set creator info
		int saved = persistence().create(file);
		file.save();
		return saved;
	}

	public int update(File file) {
		int saved = persistence().update(file);
		file.save();
		return saved;
	}

	public int save(File file) {
		if (file == null) return 0;
		switch (file.state()) {
		case CREATED: return create(file);
		case MODIFIED: return update(file);
		default: return 0;
		}
	}

	protected FileEvent save(Collection<File> files, FileEvent evt) {
		if (isEmpty(files)) return evt;

		boolean event = evt != null;
		Account account = event ? AccountContext.get().account() : null;
		String creatorID = event ? account.getId() : null,
			   creatorName = event ? account.getAlias() : null;
		boolean proceed = !event || (event && evt.set(File.OBJS, files).before());
		if (proceed)
			dbAccess().transact(dbaccess -> {
				int affected = 0;

				for (File file: files) {
					file.setCreatorID(creatorID);
					file.setCreatorName(creatorName);
					affected += save(file);
				}
				if (event)
					evt.setFired(affected > 0).on();
			}, null);
		return evt;
	}

	protected FileEvent create(Collection<FileSupport.Upload> uploads, String dirID, FileEvent evt) {
		evt.setType(FileEvent.CREATE);
		return save(File.Factory.create(config(), uploads, dirID), evt);
	}

	public FileEvent create(Collection<FileSupport.Upload> uploads, String dirID) {
		return create(uploads, dirID, event());
	}

	public FileEvent save(Request req) {
		FileEvent evt = event();
		evt.setType(FileEvent.CREATE);
		Collection<FileSupport.Upload> uploads = (Collection<FileSupport.Upload>)req.get("uploads");
		if (isEmpty(uploads)) return evt;

		String dirID = req.string(File.DIR),
			   ipAddress = notEmpty(client(req), "client").getIpAddress();
		Collection<File> files = File.Factory.create(config(), uploads, dirID);
		files.forEach(file -> file.setIpAddress(ipAddress));

		return permission().check(req.defaultAction(File.CREATE), () -> save(files, evt), null, null);
	}

	public FileEvent move(String dirID, String... fileIDs) {
		FileEvent evt = event();
		evt.setType(FileEvent.MOVE);
		String cmd = "update " + config().table() + " set dir_id = ?"
				   + "\nwhere site_id = ? and file_id " + qsupport.asIn(fileIDs) + " and dir_id <> ?";
		dbAccess().transact(dbaccess -> {evt.setFired(dbaccess.update().execute(cmd, dirID, siteID(), dirID) > 0).on();}, null);
		return evt;
	}

	public FileEvent move(Request req) {
		return permission().check(req.defaultAction(File.MOVE), () -> move(req.string(File.DIR), req.objects(File.ID)), null, null);
	}

	protected Dataset search(String[] dirIDs, String[] fileIDs, File.Status status) {
		String condition = "status not in (?, '" + File.Status.DELETE.code() + "')";
		List<String> args = qsupport.toList(status.code());
		if (!isEmpty(dirIDs)) {
			boolean parameterize = qsupport.parameterize(dirIDs);
			condition += " and dir_id " + (parameterize ? "= ?" : qsupport.asIn(dirIDs));
			if (parameterize)
				args.add(dirIDs[0]);
		}
		if (!isEmpty(fileIDs)) {
			boolean parameterize = qsupport.parameterize(fileIDs);
			condition += " and file_id " + (!parameterize ? qsupport.asIn(fileIDs) : " = ?");
			if (parameterize)
				args.add(fileIDs[0]);
		}
		return search(condition, "file_id desc", 0, 50, args.toArray());
	}

	protected List<File> setStatus(Dataset fileSet, File.Status status) {
		List<File> files = File.Factory.create(config(), fileSet);
		return files.stream().filter(file -> file.changeStatus(status)).collect(Collectors.toList());
	}

	protected int setStatus(List<File> files, File.Status status) {
		if (isEmpty(files)) return 0;

		String cmd = "update " + config().table() + " set status = ? where site_id = ? and file_id {file-ids} and status not in (?, '" + File.Status.DELETE.code() + "')";
		List<Object> args = qsupport.toList(status.code(), siteID());
		boolean parameterized = qsupport.parameterize(files);

		cmd = cmd.replace("{file-ids}", parameterized ? "= ?" : qsupport.asIn(files.stream().map(file -> file.getId()).collect(Collectors.toList())));
		if (parameterized)
			args.add(files.get(0).getId());
		args.add(status.code());

		return dbAccess().update().execute(cmd, args.toArray());
	}

	protected boolean setStatus(String[] dirIDs, String[] fileIDs, File.Status status, FileEvent evt) {
		Dataset fileSet = search(dirIDs, fileIDs, status);
		List<File> files = setStatus(fileSet, status);
		int result = setStatus(files, status);
		if (!evt.isFired())
			evt.setFired(result > 0);
		return fileSet.hasMore();
	}

	public FileEvent setStatus(String[] dirIDs, String[] fileIDs, File.Status status) {
		FileEvent evt = event();
		evt.setType(FileEvent.CHANGE_STATUS).set(File.DIR, dirIDs).set(File.ID, fileIDs).set(File.STATUS, status);
		if (!isEmpty(fileIDs) && evt.before()) {
			dbAccess().transact(dbaccess -> {
				while (true) {
					if (!setStatus(dirIDs, fileIDs, status, evt))
						break;
				}
				evt.on();
			}, null);
		}
		return evt;
	}

	public FileEvent setStatus(Request req) {
		return permission().check(req.defaultAction(File.CHANGE_STATUS), () -> setStatus(req.objects(File.DIR), req.objects(File.ID), (File.Status)req.get(File.STATUS)), null, null);
	}
}