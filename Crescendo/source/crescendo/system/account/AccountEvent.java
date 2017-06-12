package crescendo.system.account;

import crescendo.system.Account;
import crescendo.system.AccountContextEvent;
import crescendo.system.Feature;

public class AccountEvent extends AccountContextEvent {
	private static final long serialVersionUID = 1L;
	private static final String NAME = Account.OBJ + "-event";

	public static final String LOGIN = Account.LOGIN;
	public static final String LOGIN_FAILED = "login-failed";
	public static final String LOGOUT = Account.LOGOUT;
	public static final String CHANGE_STATUS = Account.CHANGE_STATUS;

	public Account account() {
		return Account.class.cast(get(Account.OBJ));
	}

	public static final AccountEvent create(Feature feature) {
		if (feature == null)
			feature = Account.feature(null);
		return create(feature, NAME);
	}
	@Override
	public AccountEvent on() {
		if (LOGIN.equals(type())) {
			boolean loggedIn = bool(Account.LOGGED_IN);
			if (!loggedIn)
				setType(LOGIN_FAILED);
		}
		super.on();
		return this;
	}
}