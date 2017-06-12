package crescendo.system;

import java.io.Serializable;

public abstract class AccountContextEvent extends Event {
	private static final long serialVersionUID = 1L;
	public static final String LOAD_ACTX = "load-account-context";
	public static final String UNLOAD_ACTX = "unload-account-context";

	private transient AccountContext actx;

	public String accountID() {
		Account account = Account.class.cast(get(Account.OBJ));
		if (account == null)
			account = actx == null ? null : actx.isUnknown() ? null : actx.account();
		return account != null ? account.getId() : null;
	}

	public AccountContext accountContext() {
		if (actx == null) {
			Event parent = parent();
			if (parent instanceof AccountContextEvent)
				return AccountContextEvent.class.cast(parent).accountContext();
		}
		return actx;
	}

	public AccountContextEvent set(AccountContext actx) {
		this.actx = actx;
		return this;
	}

	private boolean isInherent() {
		switch (type()) {
		case LOAD_ACTX:
		case UNLOAD_ACTX: return true;
		default: return false;
		}
	}
	@Override
	public AccountContextEvent setLoad(String name, Serializable obj) {
		if (isInherent())
			accountContext().set(name, obj);
		else
			super.setLoad(name, obj);
		return this;
	}
	@Override
	public AccountContextEvent setUnload(String name) {
		if (isInherent())
			accountContext().remove(name);
		else
			super.setUnload(name);
		return this;
	}

	public void onLoad(AccountContext actx) {
		set(actx).setType(LOAD_ACTX).setFired(true).on();
	}
	@Override
	public void after() {
		AccountContext ctx = accountContext();
		unloads().forEach(ctx::remove);
		loads().forEach(ctx::set);
		children().forEach(child -> child.after());
	}

	public void update(AccountContext actx) {
		if (actx == null) return;
		set(actx).after();
	}

	public void onUnload(AccountContext actx) {
		set(actx).setType(UNLOAD_ACTX).setFired(true).on();
	}
}