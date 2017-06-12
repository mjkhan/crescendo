package crescendo.system.group;

import java.util.List;
import java.util.function.Supplier;

import crescendo.system.PermissionInspector;
import crescendo.system.Request;
import horizon.data.DataRecord;
import horizon.data.Dataset;

public class GroupPermission extends PermissionInspector {
	@Override
	protected boolean permits(Request req) {
		switch (req.action()) {
		case Group.SEARCH: return search(req);
		case Group.GET: return getInfo(req);
		case Group.VIEW: return viewInfo(req);
		case Group.CREATE: return create(req);
		case Group.UPDATE: return update(req);
		case Group.MOVE: return move(req);
		case Group.REORDER: return reorder(req);
		case Group.REMOVE: return remove(req);
		case Group.CHANGE_STATUS: return setStatus(req);
		default: return false;
		}
	}

	protected boolean ownerAware(Request req) {
		return !isEmpty(req.get(Group.OWNER_TYPE))
			&& !isEmpty(req.get("owner-id"));
	}

	private boolean isOwner(Request req, Supplier<String> ownerID) {
		return isEmpty(ownerID) ? true : isSelf(req, ownerID);
	}

	protected boolean isOwner(Request req) {
		Dataset dataset = ifEmpty(req.dataset(cref("info")), () -> req.dataset(cref("list")));
		if (dataset != null) {
			if (dataset.isEmpty()) return false;

			for (DataRecord record: dataset)
				if (!isOwner(req, () -> record.string("owner_id")))
					return false;
		}

		Group group = (Group)req.get(cref("obj"));
		if (group != null)
			return isOwner(req, group::getOwnerID);

		List<Group> categories = (List<Group>)req.get(cref("objs"));
		if (categories != null)
			for (Group obj: categories)
				if (!isOwner(req, obj::getOwnerID)) return false;

		return true;
	}

	protected boolean search(Request req) {
		return true;
	}

	protected boolean getInfo(Request req) {
		return isOwner(req);
	}

	protected boolean viewInfo(Request req) {
		return !search(req, req.notEmpty(cref("id"))).isEmpty();
	}

	protected boolean create(Request req) {
		return !search(req, null).isEmpty();
	}

	protected boolean update(Request req) {
		return isOwner(req);
	}

	protected boolean move(Request req) {
		return isOwner(req);
	}

	protected boolean reorder(Request req) {
		return isOwner(req);
	}

	protected boolean remove(Request req) {
		return isOwner(req);
	}

	protected boolean setStatus(Request req) {
		return isOwner(req);
	}
}