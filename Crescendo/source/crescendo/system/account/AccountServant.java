package crescendo.system.account;

import java.util.Collection;
import java.util.Map;

import crescendo.system.Account;
import crescendo.system.CrescendoException;
import crescendo.system.Feature;
import crescendo.system.NamedObjects;
import crescendo.system.PermissionInspector;
import crescendo.system.Request;
import crescendo.system.Servant;
import horizon.data.Dataset;
import horizon.data.FieldValues;
import horizon.persistence.Persistent.Info;

public class AccountServant extends Servant.Generic implements Account.Provider {
	private static final String NAME = Account.OBJ + "-servant";
	protected static final String SEARCH = Account.OBJ + "-search";

	public static final <T extends AccountServant> T create(Feature feature) {
		return create(null, !isEmpty(feature) ? feature : Account.feature(null), NAME);
	}

	protected Account.Config config() {
		return Account.config(feature);
	}
	@Override
	protected PermissionInspector permission() {
		return permission("account-permission");
	}
	@Override
	public Dataset search(String condition, String orderBy, int start, int fetch, Object... args) {
		String table = config().table();
		return sqlDialect(Account.OBJ + "-dialect")
			  .select("search-" + table, null, table, condition, orderBy, !isEmpty(args) ? qsupport.toList(args) : null, start, fetch);
	}

	public NamedObjects search(Request req) {
		return permission().check(req.defaultAction(Account.SEARCH), () -> {
				String terms = qsupport.terms().condition(feature.string(SEARCH), req.string(Account.TERMS)),
					   condition = ifEmpty(req.string(Account.CONDITION), ""),
					   orderBy = ifEmpty(req.string(Account.ORDER), "user_id");

				if (!isEmpty(terms))
					condition = "".equals(condition) ? terms : condition + "\nand " + terms;
				String statusCondition = Account.Status.condition(req.string(Account.STATUS));
				if (!isEmpty(statusCondition)) {
					statusCondition = "status " + statusCondition;
					condition = isEmpty(condition) ? statusCondition : condition + "\nand " + statusCondition;
				}

				Object[] args = req.objects(Account.ARGS);
				int fetch = req.number(Account.FETCH).intValue(),
					start = req.number(Account.START).intValue();
				return new NamedObjects().set(Account.LIST, search(condition, orderBy, start, fetch, args));
			},
			() -> NamedObjects.EMPTY,
			null
		);
	}

	public Dataset getInfo(String fieldName, Object[] values) {
		return search(
				qsupport.fieldCondition(fieldName, values) + " and status " + Account.Status.condition("none"),
				qsupport.parameterize(values) ? values : null
			);
	}

	public Dataset getInfo(String... accountIDs) {
		return getInfo("user_id", accountIDs);
	}

	private static final String ATTRS = "select * from " + Info.table(Account.Attribute.class) + " where user_id = ?";

	public Dataset getAttributeInfo(String accountID) {
		return adminAccess().query(Account.ATTR).getRecords(ATTRS, new Object[]{accountID});
	}

	public NamedObjects getInfo(Request req) {
		return permission().check(req.defaultAction(Account.GET), () -> {
				String accountID = req.string(Account.ID);
				return adminAccess().open(dbaccess -> {
					Dataset dataset = getInfo(accountID);
					return new NamedObjects()
					  .set(Account.INFO, dataset)
					  .set(Account.ATTR, dataset.isEmpty() ? null : getAttributeInfo(accountID));
				}, null);
			}, null, null
		);
	}

	public NamedObjects viewInfo(Request req) {
		return permission().check(req.defaultAction(Account.VIEW), () -> {
				String accountID = req.string(Account.ID);
				return adminAccess().open(dbaccess -> {
					Dataset dataset = getInfo(accountID);
					return new NamedObjects()
					  .set(Account.INFO, dataset)
					  .set(Account.ATTR, dataset.isEmpty() ? null : getAttributeInfo(accountID));
				}, null);
			}, null, null
		);
	}

	public Dataset newInfo(int count) {
		if (count < 1)
			throw new CrescendoException().setId("invalid-account-count");
		Dataset accountSet = search("user_id is null");
		for (int i = 0; i < count; ++i)
			config().setDefaults(accountSet.append());
		return accountSet;
	}

	public NamedObjects newInfo(Request req) {
		return new NamedObjects().set(Account.INFO, newInfo(Math.max(1, req.defaultAction(Account.NEW).number(Account.COUNT).intValue())));
	}
	@Override
	public Account getAccount(String accountID) {
		return adminAccess().open(dbaccess -> {
			Dataset accountSet = getInfo(accountID);
			if (!accountSet.isEmpty()
			 && !Account.Status.REMOVED.code().equals(accountSet.getValue("status"))) {
				Account account = Account.Factory.create(config(), accountSet.get(0));
				setAttributes(account);
				return account;
			} else
				return null;
		}, null);
	}

	protected void setAttributes(Account account) {
		if (account == null) return;
		account.attributes().read(getAttributeInfo(account.getId()));
	}

	public Collection<Account> getAccounts(String... accountIDs) {
		return adminAccess().open(dbaccess -> {
			Dataset accountSet = getInfo(accountIDs);
			Collection<Account> accounts = Account.Factory.create(config(), accountSet);
			accounts.forEach(this::setAttributes);
			return accounts;
		}, null);
	}

	public Account.Validator.Result validate(String name, Map<String, Object> args) {
		return config().validator()
			  .set(adminAccess())
			  .validate(name, args);
	}

	public Account.Validator.Result validate(Request req) {
		return validate(
				  req.defaultAction(Account.VALIDATE).string(Account.Validator.NAME),
				  req
			   );
	}

	protected int create(Account account) {
		return adminPersistence().create(account);
	}

	protected int update(Account account) {
		return adminPersistence().update(account);
	}

	protected int save(Account account, AccountEvent evt) {
		if (account == null || !account.state().isDirty()) return 0;
		boolean event = evt != null;

		return adminAccess().transact(dbaccess -> {
			int result = 0;
			switch (account.state()) {
			case CREATED: result = create(account); break;
			case MODIFIED: result = update(account); break;
			default:
				if (account.attributes().isDirty())
					result = update(account);
				break;
			}
			if (event && evt.setFired(result > 0).isFired()) {
				if (AccountEvent.UPDATE.equals(evt.type()))
					evt.set("changed-" + Account.FIELD_NAME, account.clearChangedFields());
				evt.on();
			}
			if (event && evt.isFired())
				switch (evt.type()) {
				case AccountEvent.UPDATE:
				case AccountEvent.CHANGE_STATUS: evt.setLoad(Account.OBJ, account); break;
				case AccountEvent.REMOVE: evt.setHasUpdates(true); break;
				}
			return result;
		}, null);
	}

	public AccountEvent save(Account account) {
		AccountEvent evt = AccountEvent.create(feature);
		if (account != null
		 && evt.setType(Account.State.CREATED.equals(account.state()) ? AccountEvent.CREATE : AccountEvent.UPDATE)
		 	   .set(Account.OBJ, account)
		 	   .before()
			)
			save(account, evt);
		return evt;
	}

	public AccountEvent save(FieldValues accountInfo, FieldValues attributes) {
		Account account = null;
		switch (accountInfo.state()) {
		case CREATED: account = Account.Factory.create(config()); break;
		default: account = getAccount(accountInfo.string("user_id")); break;
		}
		if (account != null) {
			account.read(adminPersistence().getRecord(config().entityAs(), accountInfo));
			if (attributes != null)
				account.attributes().set(attributes);
		}
		return save(account);
	}

	public AccountEvent save(Request req) {
		FieldValues accountInfo = FieldValues.class.cast(req.get(Account.INFO));
		return permission().check(req.defaultAction(Account.State.CREATED.equals(accountInfo.state()) ? Account.CREATE : Account.UPDATE), () -> {
				FieldValues attributes = FieldValues.class.cast(req.get(Account.ATTR));
				return save(accountInfo, attributes);
			}, null, null
		);
	}

	public AccountEvent setStatus(String accountID, Account.Status status) {
		AccountEvent evt = AccountEvent.create(feature);
		Account account = !isEmpty(accountID) && !isEmpty(status) ? getAccount(accountID) : null;
		if (!isEmpty(account)
		 && evt.setType(Account.Status.remove(status) ? AccountEvent.REMOVE : AccountEvent.CHANGE_STATUS)
			   .set(Account.OBJ, account).set(Account.STATUS, status)
		 	   .before()) {
			account.setStatus(status);
			save(account, evt);
		}
		return evt;
	}

	public AccountEvent setStatus(Request req) {
		Account.Status status = Account.Status.class.cast(req.remove(Account.STATUS));
		return permission().check(req.defaultAction(Account.Status.remove(status) ? Account.REMOVE : Account.CHANGE_STATUS), () -> {
				String accountID = req.string(Account.ID);
				return setStatus(accountID, status);
			}, null, null
		);
	}
}