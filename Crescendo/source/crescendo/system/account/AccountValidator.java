package crescendo.system.account;

import java.util.Map;

import crescendo.system.Account;
import crescendo.system.Entity;
import crescendo.system.code.CodeServant;

public class AccountValidator extends Account.Validator {
	@Override
	public Result validate(String name, Map<String, Object> args) {
		if ("id".equals(name))
			return validID((String)args.get("value"));
		return Result.VALID;
	}

	protected Result validID(String id) {
		if (isEmpty(id))
			return new Result().setMessage("empty-" + Account.ID);

		String pattern = cfg.string(Account.ID + "-pattern");
		if (!isEmpty(pattern) && !id.matches(pattern))
			return new Result().setMessage("invalid-" + Account.ID);
		if (getCount(cfg.table(), "user_id = '" + id + "'") > 0)
			return new Result().setMessage(Account.ID + "-in-use");
		if (CodeServant.create().exists("account", "reserved-id", id))
			return new Result().setMessage("reserved-" + Account.ID);

		return Result.VALID;
	}
	@Override
	protected void doValidate(Entity entity) {
		Account account = Account.class.cast(entity);
		switch (account.state()) {
		case CREATED:
			Result result = validate("id", account.getId());
			if (!result.isValid())
				throw result.invalid();
			if (!result.isValid())
				throw result.invalid();
			break;
		default: break;
		}
	}
}