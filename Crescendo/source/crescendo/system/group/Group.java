package crescendo.system.group;

import java.text.DecimalFormat;
import java.util.Collection;

import crescendo.system.Feature;
import crescendo.system.HierarchicalEntity;
import crescendo.system.SiteContext;
import crescendo.util.TimeSupport;
import horizon.persistence.Column;
import horizon.persistence.Selector;
@Selector("select * from ${crsnd_group} where site_id = ? and grp_type = ? and grp_id = ?")
public class Group extends HierarchicalEntity {
	private static final long serialVersionUID = 1L;

	public static final String OBJ = "group";
	public static final String OBJS = "groups";
	public static final String INFO = OBJ + "-info";
	public static final String LIST = OBJ + "-list";
	public static final String ID = OBJ + "-id";
	public static final String TYPE = OBJ + "-type";
	public static final String OWNER_TYPE = "owner-type";

	public static Feature feature(SiteContext sctx, String entryID) {
		return Feature.get(sctx, entryID);
	}

	protected String
		type,
		subtype,
		description,
		imgUrl;
	protected int sortOrder;
	protected String
		ownerType,
		ownerID;
	@Column(name="grp_type", callon="write")
	public String getType() {
		return type;
	}
	@Column(name="grp_type")
	public void setType(String type) {
		if (equals(this.type, type)) return;
		this.type = notEmpty(type, "type");
		setModified("type");
	}
	@Override
	protected void setId() {
		String today = TimeSupport.now("yyMMdd"),
			   query = "select max(grp_id) a_val from {table} where site_id = ? and grp_type = ? grp_id like ?".replace("{table}", Info.table(getClass())),
			   max = pm.dataAccess().query("a_val").getValue(query, getSiteID(), type, today + "%");
		int lastID = Integer.parseInt(ifEmpty(max, "0").replace(today, ""));
		setId(today + new DecimalFormat(cfg.string("id-digit")).format(Integer.valueOf(++lastID)));
	}
	@Override
	@Column(name="grp_id", callon="write")
	public String getId() {
		return super.getId();
	}
	@Override
	@Column(name="grp_id")
	public void setId(String id) {
		super.setId(id);
	}
	@Column(name="subtype", callon="write")
	public String getSubtype() {
		return subtype;
	}
	@Column(name="subtype")
	public void setSubtype(String subtype) {
		if (equals(this.subtype, subtype)) return;
		this.subtype = subtype;
		setModified("subtype");
	}
	@Override
	@Column(name="grp_name", callon="write")
	public String getName() {
		return super.getName();
	}
	@Override
	@Column(name="grp_name")
	public void setName(String name) {
		super.setName(name);
	}
	@Column(name="descrp", callon="write")
	public String getDescription() {
		return description;
	}
	@Column(name="descrp")
	public void setDescription(String description) {
		if (equals(this.description, description)) return;
		this.description = description;
		setModified("description");
	}
	@Override
	@Column(name="prnt_id", callon="write")
	public String getParentID() {
		return super.getParentID();
	}
	@Override
	@Column(name="prnt_id")
	public void setParentID(String parentID) {
		super.setParentID(parentID);
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
	@Column(name="sort_ord", callon="write")
	public int getSortOrder() {
		return sortOrder;
	}
	@Column(name="sort_ord")
	public void setSortOrder(int sortOrder) {
		if (this.sortOrder == sortOrder) return;
		if (sortOrder < 0)
			throw new IllegalArgumentException("sortOrder < 0");

		this.sortOrder = sortOrder;
		setModified("sortOrder");
	}
	@Column(name="owner_type", callon="write")
	public String getOwnerType() {
		return ownerType;
	}
	@Column(name="owner_type")
	public void setOwnerType(String ownerType) {
		if (equals(this.ownerType, ownerType)) return;
		this.ownerType = ownerType;
		setModified();
	}
	@Column(name="owner_id", callon="write")
	public String getOwnerID() {
		return ownerID;
	}
	@Column(name="owner_id")
	public void setOwnerID(String ownerID) {
		if (equals(this.ownerID, ownerID)) return;
		this.ownerID = ownerID;
		setModified("ownerID");
	}
	@Override
	protected void read(String fieldName, Object value) {
		if ("sort_ord".equals(fieldName))
			setSortOrder(asNumber(value).intValue());
		else
			super.read(fieldName, value);
	}
	@Override
	public Group getParent() {
		return (Group)parent;
	}
	@Override
	public Collection<? extends Group> getChildren() {
		return (Collection<? extends Group>)super.getChildren();
	}
}