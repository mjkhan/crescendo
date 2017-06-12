package crescendo.system;

import java.sql.Timestamp;
import java.util.List;

import crescendo.util.TimeSupport;
import horizon.data.DataRecord;
import horizon.persistence.Column;
import horizon.persistence.Persistent;
import horizon.persistence.Selector;
@Selector("select * from ${crsnd_account} where user_id = ?")
public class Account extends Entity implements Principal, AttributeOwner {
	private static final long serialVersionUID = 1L;

	public static final String LOGIN = "login";
	public static final String LOGGED_IN = "loggedin";
	public static final String LOGOUT = "logout";
	public static final String RELOAD = "reload";

	public static final String OBJ = "account";
	public static final String OBJS = OBJ + "s";
	public static final String INFO = OBJ + "-info";
	public static final String ATTR = OBJ + "-attr";
	public static final String LIST = OBJ + "-list";
	public static final String ID = OBJ + "-id";

	public static final String UNKNOWN = "unknown";

	private static final String DEF_CFG = "crescendo/default/account.xml";

	public static final Feature feature(String path) {
		Feature f = !isEmpty(path) ?
				Feature.load(path) :
				Client.get().profile().feature(OBJ);
		if (f == null)
			f = Feature.load(DEF_CFG);
		return f;
	}

	public static final Config config(Feature feature) {
		if (feature == null)
			feature = feature(null);
		Config cfg = Config.get(feature, OBJ);
		return cfg;
	}

	private String
		id,
		type,
		alias,
		imgUrl;
	private transient String password;
	private Attributes attributes;
	private Timestamp
		createdAt,
		lastModified;
	private Status status;
	@Override
	@Column(name="user_id", callon="write")
	public String getId() {
		return ifEmpty(id, UNKNOWN);
	}
	@Column(name="user_id")
	private void setId(String id) {
		if (equals(this.id, id)) return;
		this.id = notEmpty(id, "id");
		setModified();
	}

	public boolean isUnknown() {
		return UNKNOWN.equals(getId());
	}
	@Column(name="user_type", callon="write")
	public String getType() {
		return type;
	}
	@Column(name="user_type")
	public void setType(String type) {
		if (equals(this.type, type)) return;
		this.type = type;
		setModified("type");
	}
	@Override
	public Type getPrincipalType() {
		return Type.ACCOUNT;
	}
	@Column(name="alias", callon="write")
	public String getAlias() {
		return ifEmpty(alias, id);
	}
	@Column(name="alias")
	public void setAlias(String alias) {
		if (equals(this.alias, alias)) return;
		this.alias = alias;
		setModified("alias");
	}
	@Override
	public String getName() {
		return getAlias();
	}
	@Column(name="passwd", callon="write")
	public String getPassword() {
		return password;
	}
	@Column(name="passwd")
	public void setPassword(String password) {
		if (equals(this.password, password)) return;
		this.password = notEmpty(password, "password");
		setModified("password");
	}
	@Column(name="img_url", callon="write")
	public String getImgUrl() {
		return imgUrl;
	}
	@Column(name="img_url")
	public void setImgUrl(String imgUrl) {
		if (equals(this.imgUrl, imgUrl)) return;
		this.imgUrl = imgUrl;
		setModified("imgUrl");
	}
	@Column(name="ins_time", callon="write")
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	@Column(name="ins_time")
	public void setCreatedAt(Timestamp createdAt) {
		if (equals(this.createdAt, createdAt)) return;
		this.createdAt = notEmpty(createdAt, "createdAt");
		setModified();
	}
	@Column(name="upd_time", callon="write")
	public Timestamp getLastModified() {
		return lastModified;
	}
	@Column(name="upd_time")
	public void setLastModified(Timestamp lastModified) {
		if (equals(this.lastModified, lastModified)) return;
		this.lastModified = notEmpty(lastModified, "lastModified");
		setModified();
	}

	public Status getStatus() {
		return ifEmpty(status, Status.CREATED);
	}

	public void setStatus(Status status) {
		if (equals(this.status, status)) return;

		Status.validate(this.status, status);
		this.status = notEmpty(status, "status");
		setModified("status");
	}

	public Attributes attributes() {
		return attributes != null ? attributes :
			  (attributes = new Attributes().setEntryType(Attribute.class));
	}
	@Override
	public Object getAttribute(String name) {
		return attributes != null ? attributes.get(name) : null;
	}

	public boolean isSystemAdministrator() {
		return "true".equals(getAttribute(Crescendo.systemAdministratorAttribute()));
	}
	@Override
	protected void read(String fieldName, Object value) {
		super.read(fieldName, value);
		if ("status".equals(fieldName))
			setStatus(Status.codeOf(asString(value)));
	}
	@Override
	public void store(List<Persistent> persistents, Action action) {
		Timestamp now = TimeSupport.now();
		if (State.CREATED.equals(state))
			setCreatedAt(now);
		if (state.isDirty() || attributes().isDirty())
			setLastModified(now);
		super.store(persistents, action);
		Iterable<Attribute> attrs = attributes().getDirties();
		for (Attribute attr: attrs) {
			persistents.add(attr.setAccountID(id));
			setChanged(attr.key());
		}
	}
	@Override
	public void write(DataRecord record) {
		if (isUnknown())
			throw new RuntimeException("Attempted to save " + this);
		super.write(record);
		record.set("status", getStatus().code());
	}
	@Override
	public String toString() {
		return getClass().getName() + "(" + (isUnknown() ? id : "\"" + id + "\"") + ")";
	}
	@Selector("select * from ${crsnd_account_attr} where user_id = ? and attr_key = ?")
	public static class Attribute extends Attributes.Entry {
		private static final long serialVersionUID = 1L;
		private String accountID;
		@Column(name="user_id", callon="write")
		public String accountID() {
			return accountID;
		}
		@Column(name="user_id")
		public Attribute setAccountID(String accountID) {
			if (!equals(this.accountID, accountID)) {
				this.accountID = accountID;
				setModified();
			}
			return this;
		}
	}

	public static interface Provider extends Servant {
		public Account getAccount(String accountID);

		public static class Factory {
			public static final Provider create(Feature feature) {
				Class<? extends Provider> klass = feature.klass("account-servant");
				return Feature.instance(klass).set(feature);
			}
		}
	}
}