package crescendo.system;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;

import crescendo.util.TimeSupport;
import horizon.data.DataRecord;
import horizon.persistence.Column;
import horizon.persistence.Persistent;
import horizon.persistence.Selector;
@Selector("select * from ${crsnd_site} where site_id = :site-id")
public class Site extends Entity implements AttributeOwner {
	private static final long serialVersionUID = 1L;

	public static final String SETUP = "setup";
	public static final String RELOAD = "reload";

	public static final String OBJ = "site";
	public static final String OBJS = OBJ + "s";
	public static final String INFO = OBJ + "-info";
	public static final String ATTR = OBJ + "-attr";
	public static final String LIST = OBJ + "-list";
	public static final String CURRENT = "current-site";
	public static final String NOT_FOUND = OBJ + "-not-found";

	public static final String ID = OBJ + "-id";
	public static final String TYPE = OBJ + "-type";
	public static final String SPACE = "sitespace";

	private static final String DEF_CFG = "crescendo/default/site.xml";

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
		return Config.get(feature, OBJ);
	}

	private String
		id,
		type,
		name,
		space,
		dbConnection,
		rdConnection,
		filebase,
		profile,
		evtCfg,
		jobCfg,
		uiCtx,
		creatorID,
		creatorName,
		ownerID,
		ownerName;
	private Attributes attributes;
	private Timestamp
		createdAt,
		lastModified;
	private Status status;
	@Column(name="site_id", callon="write")
	public String getId() {
		return id;
	}
	@Column(name="site_id")
	private void setId(String id) {
		if (equals(this.id, id)) return;
		this.id = id;
		setModified();
	}
	@Column(name="site_type", callon="write")
	public String getType() {
		return type;
	}
	@Column(name="site_type")
	public void setType(String type) {
		if (equals(this.type, type)) return;
		this.type = type;
		setModified("type");
	}
	@Column(name="site_name", callon="write")
	public String getName() {
		return ifEmpty(name, id);
	}
	@Column(name="site_name")
	public void setName(String name) {
		if (equals(this.name, name)) return;
		this.name = name;
		setModified("name");
	}
	@Column(name="site_spc", callon="write")
	public String sitespace() {
		return space;
	}
	@Column(name="site_spc")
	public void setSpace(String space) {
		if (equals(this.space, space)) return;
		this.space = space;
		setModified("space");
	}
	@Column(name="db_conn", callon="write")
	public String dbConnection() {
		return dbConnection;
	}
	@Column(name="db_conn")
	public void setDbConnection(String dbConnection) {
		if (equals(this.dbConnection, dbConnection)) return;
		this.dbConnection = notEmpty(dbConnection, "dbConnection");
		setModified("dbConnection");
	}
	@Column(name="rd_conn", callon="write")
	public String rdConnection() {
		return ifEmpty(rdConnection, dbConnection);
	}
	@Column(name="rd_conn")
	public void setRdConnection(String rdConnection) {
		if (equals(this.rdConnection, rdConnection)) return;
		this.rdConnection = rdConnection;
		setModified("rdConnection");
	}

	public String dbConnection(boolean readWrite) {
		return readWrite ? dbConnection() : rdConnection();
	}
	@Column(name="filebase", callon="write")
	public String filebase() {
		return filebase;
	}
	@Column(name="filebase")
	public void setFilebase(String filebase) {
		if (equals(this.filebase, filebase)) return;
		this.filebase = filebase;
		setModified("filebase");
	}
	@Column(name="profile", callon="write")
	public String profile() {
		return profile;
	}
	@Column(name="profile")
	public void setProfile(String profile) {
		if (equals(this.profile, profile)) return;
		this.profile = profile;
		setModified("profile");
	}
	@Column(name="evt_cfg", callon="write")
	public String eventConfig() {
		return evtCfg;
	}
	@Column(name="evt_cfg")
	public void setEventConfig(String evtCfg) {
		if (equals(this.evtCfg, evtCfg)) return;
		this.evtCfg = evtCfg;
		setModified("eventConfig");
	}
	@Column(name="job_cfg", callon="write")
	public String jobConfig() {
		return jobCfg;
	}
	@Column(name="job_cfg")
	public void setJobConfig(String jobCfg) {
		if (equals(this.jobCfg, jobCfg)) return;
		this.jobCfg = jobCfg;
		setModified("jobConfig");
	}
	@Column(name="ui_ctx", callon="write")
	public String uiContext() {
		return uiCtx;
	}
	@Column(name="ui_ctx")
	public void setUIContext(String uiCtx) {
		if (equals(this.uiCtx, uiCtx)) return;
		this.uiCtx = uiCtx;
		setModified("uiContext");
	}
	@Column(name="ins_id", callon="write")
	public String getCreatorID() {
		return creatorID;
	}
	@Column(name="ins_id")
	public void setCreatorID(String creatorID) {
		if (equals(this.creatorID, creatorID)) return;
		this.creatorID = notEmpty(creatorID, "creatorID");
		setModified();
	}
	@Column(name="ins_name", callon="write")
	public String getCreatorName() {
		return creatorName;
	}
	@Column(name="ins_name")
	public void setCreatorName(String creatorName) {
		if (equals(this.creatorName, creatorName)) return;
		this.creatorName = notEmpty(creatorName, "creatorName");
		setModified();
	}
	@Column(name="owner_id", callon="write")
	public String getOwnerID() {
		return ifEmpty(ownerID, creatorID);
	}
	@Column(name="owner_id")
	public void setOwnerID(String ownerID) {
		if (equals(this.ownerID, ownerID)) return;
		this.ownerID = ownerID;
		setModified("ownerID");
	}
	@Column(name="owner_name", callon="write")
	public String getOwnerName() {
		return ifEmpty(ownerName, creatorName);
	}
	@Column(name="owner_name")
	public void setOwnerName(String ownerName) {
		if (equals(this.ownerName, ownerName)) return;
		this.ownerName = ownerName;
		setModified("ownerName");
	}
	@Column(name="ins_time", callon="write")
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	@Column(name="ins_time")
	private void setCreatedAt(Timestamp createdAt) {
		if (equals(this.createdAt, createdAt)) return;
		this.createdAt = notEmpty(createdAt, "createdAt");
		setModified();
	}
	@Column(name="upd_time", callon="write")
	public Timestamp getLastModified() {
		return lastModified;
	}
	@Column(name="upd_time")
	private void setLastModified(Timestamp lastModified) {
		if (equals(this.lastModified, lastModified)) return;
		this.lastModified = notEmpty(lastModified, "lastModified");
		setModified();
	}

	public Status getStatus() {
		return status;
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
	@Override
	protected void read(String fieldName, Object value) {
		if ("status".equals(fieldName))
			setStatus(Status.codeOf(asString(value)));
		else
			super.read(fieldName, value);
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
			persistents.add(attr.setSiteID(id));
			setChanged(attr.key());
		}
	}
	@Override
	public void write(DataRecord record) {
		super.write(record.set("status", getStatus().code()));
	}
	@Override
	public String toString() {
		return getClass().getName() + "(\"" + getId() + "\")";
	}
	@Selector("select * from ${crsnd_site_attr} where site_id = ? and attr_key = ?")
	public static class Attribute extends Attributes.Entry {
		private static final long serialVersionUID = 1L;
		private String siteID;
		@Column(name="site_id", callon="write")
		public String siteID() {
			return siteID;
		}
		@Column(name="site_id")
		public Attribute setSiteID(String siteID) {
			if (!equals(this.siteID, siteID)) {
				this.siteID = siteID;
				setModified();
			}
			return this;
		}
	}

	public static interface Provider extends Servant {
		public Site getSite(String siteID, String siteType);

		public static class Factory {
			public static final Provider create(Feature feature) {
				Class<? extends Provider> klass = feature.klass("site-servant");
				return Feature.instance(klass).set(feature);
			}
		}
	}

	public static class Profile extends Feature {
		private static final long serialVersionUID = 1L;
		private static final String DEF_PROF = "crescendo/default/site-profile.xml";

		public static final Profile get(Site site) {
			return load(ifEmpty(site.profile(), () -> DEF_PROF));
		}

		void fireContextEvent(Consumer<SiteContextEvent> consumer) {
			Event.fire(Client.Profile.get(), SiteContextEvent.class, consumer);
			Event.fire(this, SiteContextEvent.class, consumer);
		}
		@Override
		public Object get(Object key) {
			Object obj = super.get(key);
			if (obj == null)
				obj = Crescendo.get().get(key);
			return obj;
		}
	}
}