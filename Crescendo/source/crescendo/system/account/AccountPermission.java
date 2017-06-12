package crescendo.system.account;

import horizon.data.FieldValues;
import crescendo.system.Account;
import crescendo.system.AccountContext;
import crescendo.system.PermissionInspector;
import crescendo.system.Request;

public class AccountPermission extends PermissionInspector {
	@Override
	public boolean grants(Request req) {
		return adminPermits(req);
	}
	@Override
	protected boolean permits(Request req) {
		switch (req.action()) {
		case Account.SEARCH: return search(req);
		case Account.GET: return getInfo(req);
		case Account.VIEW: return viewInfo(req);
		case Account.CREATE: return createAccount(req);
		case Account.UPDATE: return updateAccount(req);
		case Account.REMOVE: return removeAccount(req);
		case Account.CHANGE_STATUS: return setStatus(req);
		default: return false;
		}
	}

	protected boolean search(Request req) {
		return true;
	}

	protected boolean isSelf(Request req) {
		return isSelf(req, () -> req.notEmpty(Account.ID));
	}

	protected boolean getInfo(Request req) {
		return isSelf(req);
	}

	protected boolean viewInfo(Request req) {
		return true;
	}

	protected boolean createAccount(Request req) {
		return true;
	}

	protected boolean updateAccount(Request req) {
		FieldValues accountInfo = FieldValues.class.cast(req.get(Account.INFO));
		AccountContext actx = accountContext(req);
		return accountInfo.string("user_id").equals(actx.isUnknown() ? null : actx.account().getId());
	}

	protected boolean removeAccount(Request req) {
		return isSelf(req);
	}

	protected boolean setStatus(Request req) {
		return isSelf(req);
	}
}