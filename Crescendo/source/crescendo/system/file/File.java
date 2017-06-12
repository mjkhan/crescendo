package crescendo.system.file;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import crescendo.system.Entity;
import crescendo.util.TimeSupport;
import horizon.data.DataRecord;
import horizon.persistence.Column;
import horizon.persistence.Selector;
import horizon.util.FileSupport;
@Selector("select * from ${crsnd_file} where site_id = ? and file_id = ?")
public class File extends Entity {
	private static final long serialVersionUID = 1L;
	public static final String OBJ = "file";
	public static final String OBJS = "files";
	public static final String INFO = OBJ + "-info";
	public static final String LIST = OBJ + "-list";
	public static final String ID = OBJ + "-id";
	public static final String TYPE = OBJ + "-type";
	public static final String DIR = "dir-id";

	public static final String MOVE = "move";

	protected String
		siteID,
		id,
		type,
		name,
		dirID,
		path,
		mimeType,
		creatorID,
		creatorName,
		ipAddress;
	protected long
		size,
		downloads;
	protected Timestamp createdAt;
	protected Status status;

	protected byte[] data;
	@Column(name="site_id", callon="write")
	public String getSiteID() {
		return siteID;
	}
	@Column(name="site_id")
	public void setSiteID(String siteID) {
		if (equals(this.siteID, siteID)) return;
		this.siteID = notEmpty(siteID, "siteID");
		setModified("siteID");
	}
	@Column(name="file_id", callon="write")
	public String getId() {
		return id;
	}
	@Column(name="file_id")
	public void setId(String id) {
		if (equals(this.id, id)) return;
		this.id = notEmpty(id, "id");
		setModified("id");
	}

	protected void setId() {
		String today = TimeSupport.now("yyMMdd"),
			   query = "select max(file_id) a_val from {table} where site_id = ? and file_id like ?".replace("{table}", Info.table(getClass())),
			   max = pm.dataAccess().query("a_val").getValue(query, getSiteID(), today + "%");
		int lastID = Integer.parseInt(ifEmpty(max, "0").replace(today, ""));
		String tail = new DecimalFormat(cfg.string("id-digit")).format(Integer.valueOf(++lastID));
		setId(today + tail);
		String ext = getExt();
		setPath(TimeSupport.now("yy/MM/dd/") + tail + (!isEmpty(ext) ? "." + ext : ""));
	}
	@Column(name="file_type", callon="write")
	public String getType() {
		return type;
	}
	@Column(name="file_type")
	public void setType(String type) {
		if (equals(this.type, type)) return;
		this.type = notEmpty(type, "type");
		setModified("type");
	}
	@Column(name="file_name", callon="write")
	public String getName() {
		return name;
	}
	@Column(name="file_name")
	public void setName(String name) {
		if (equals(this.name, name)) return;
		this.name = notEmpty(name, "name");
		setModified("name");
	}

	public String getExt() {
		return FileSupport.ext(name);
	}
	@Column(name="dir_id", callon="write")
	public String getDirID() {
		return dirID;
	}
	@Column(name="dir_id")
	public void setDirID(String dirID) {
		if (equals(this.dirID, dirID)) return;
		this.dirID = notEmpty(dirID, "dirID");
		setModified("dirID");
	}
	@Column(name="path", callon="write")
	public String getPath() {
		return path;
	}
	@Column(name="path")
	public void setPath(String path) {
		if (equals(this.path, path)) return;
		this.path = notEmpty(path, "path");
		setModified("path");
	}

	protected Filebase filebase() {
		return Filebase.load(cfg.string("filebase"));
	}

	private String getAbsolutePath(Status status) {
		return (filebase().getDir(this, status) + path).replace("{site-id}", siteID).replace("{user-id}", creatorID);
	}

	public String getAbsolutePath() {
		return getAbsolutePath(null);
	}

	public String getUrl() {
		return (filebase().getUrlPrefix(this) + path).replace("{site-id}", siteID).replace("{user-id}", creatorID);
	}
	@Column(name="mime_type", callon="write")
	public String getMimeType() {
		return ifEmpty(mimeType, "application/octet-stream");
	}
	@Column(name="mime_type")
	public void setMimeType(String mimeType) {
		if (equals(this.mimeType,mimeType)) return;
		this.mimeType = notEmpty(mimeType, "mimeType");
		setModified("mimeType");
	}
	@Column(name="ins_id", callon="write")
	public String getCreatorID() {
		return creatorID;
	}
	@Column(name="ins_id")
	public void setCreatorID(String creatorID) {
		if (equals(this.creatorID, creatorID)) return;
		this.creatorID = notEmpty(creatorID, "creatorID");
		setModified("creatorID");
	}
	@Column(name="ins_name", callon="write")
	public String getCreatorName() {
		return creatorName;
	}
	@Column(name="ins_name")
	public void setCreatorName(String creatorName) {
		if (equals(this.creatorName, creatorName)) return;
		this.creatorName = notEmpty(creatorName, "creatorName");
		setModified("creatorName");
	}
	@Column(name="ip_addr", callon="write")
	public String getIpAddress() {
		return ipAddress;
	}
	@Column(name="ip_addr")
	public void setIpAddress(String ipAddress) {
		if (equals(this.ipAddress, ipAddress)) return;
		this.ipAddress = notEmpty(ipAddress, "ipAddress");
		setModified("ipAddress");
	}
	@Column(name="file_size", callon="write")
	public long getSize() {
		return size;
	}
	@Column(name="file_size")
	public void setSize(long size) {
		if (this.size == size) return;
		if (size < 0)
			throw new IllegalArgumentException("size < 0");
		this.size = size;
		setModified("size");
	}
	@Column(name="downloads", callon="write")
	public long getDownloads() {
		return downloads;
	}
	@Column(name="downloads")
	public void setDownloads(long downloads) {
		if (this.downloads == downloads) return;
		if (downloads < 0)
			throw new IllegalArgumentException("downloads < 0");
		this.downloads = downloads;
		setModified("downloads");
	}
	@Column(name="ins_time", callon="write")
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	@Column(name="ins_time")
	public void setCreatedAt(Timestamp createdAt) {
		if (equals(this.createdAt, createdAt)) return;
		this.createdAt = notEmpty(createdAt, "createdAt");
		setModified("createdAt");
	}

	public Status getStatus() {
		return ifEmpty(status, Status.ACTIVE);
	}

	public void setStatus(Status status) {
		if (equals(this.status, status)) return;
		this.status = notEmpty(status, "status");
		setModified("status");
	}

	public File init(FileSupport.Upload upload) {
		if (upload != null && upload.isValid()) {
			setName(upload.filename());
			data = upload.data();
			setSize(data.length);
		}
		return this;
	}

	boolean save() {
		if (isEmpty(data)) return false;
		FileSupport.get().create(getAbsolutePath(), data, "/");
		return true;
	}

	public boolean changeStatus(Status status) {
		if (equals(this.status, status)) return false;

		String src = getAbsolutePath(),
			   dest = getAbsolutePath(status);
		return equals(src, dest) ? true :// CREATED <--> ACTIVE or INACTIVE <--> REMOVED
			   FileSupport.get().move(src, FileSupport.dirName(dest, "/"));
	}
	@Override
	protected void read(String fieldName, Object value) {
		if ("status".equals(fieldName))
			setStatus(Status.codeOf(asString(value)));
		else if ("file_size".equals(fieldName))
			setSize(asNumber(value).longValue());
		else if ("downloads".equals(fieldName))
			setDownloads(asNumber(value).longValue());
		else
			super.read(fieldName, value);
	}
	@Override
	public void write(DataRecord record) {
		record.setValue("status", getStatus().code());
		if (State.CREATED.equals(state)) {
			if (isEmpty(id))
				setId();
			setType(cfg.feature().string(TYPE));
			setCreatedAt(TimeSupport.now());
		}
		super.write(record);
	}
	@Override
	public String toString() {
		return getClass().getName() + "(\"" + siteID + "\", \"" + id + "\", \"" + name + "\")";
	}

	public static class Factory extends Entity.Factory {
		public static final Collection<File> create(Config cfg, Collection<FileSupport.Upload> uploads, String dirID) {
			notEmpty(dirID, DIR);
			return notEmpty(uploads, "uploads").stream().filter(upload -> upload != null && upload.isValid())
					.map(upload -> {
						File file = File.Factory.create(cfg);
						file.init(upload).setDirID(dirID);
						return file;
					}).collect(Collectors.toCollection(ArrayList::new));
		}
	}
}