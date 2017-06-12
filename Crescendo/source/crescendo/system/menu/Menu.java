package crescendo.system.menu;

import java.text.DecimalFormat;
import java.util.Collection;

import crescendo.system.Feature;
import crescendo.system.HierarchicalEntity;
import crescendo.system.SiteContext;
import horizon.persistence.Column;
import horizon.persistence.Selector;
@Selector("select * from ${crsnd_menu} where site_id = ? and menu_id = ?")
public class Menu extends HierarchicalEntity {
	private static final long serialVersionUID = 1L;

	public static final String OBJ = "menu";
	public static final String OBJS = OBJ + "s";
	public static final String INFO = OBJ + "-info";
	public static final String LIST = OBJ + "-list";
	public static final String ID = OBJ + "-id";
	public static final String TYPE = OBJ + "-type";

	public static Feature feature(SiteContext sctx, String entryID) {
		return Feature.get(sctx, entryID);
	}

	protected String
		type,
		description,
		action,
		setting,
		imgUrl;
	protected int sortOrder;
	@Column(name="menu_type", callon="write")
	public String getType() {
		return type;
	}
	@Column(name="menu_type")
	public void setType(String type) {
		if (equals(this.type, type)) return;
		this.type = notEmpty(type, "type");
		setModified("type");
	}
	@Override
	protected void setId() {
		String query = "select max(menu_id) a_val from {table} where site_id = ?".replace("{table}", Info.table(getClass())),
			   max = pm.dataAccess().query("a_val").getValue(query, getSiteID());
		int lastID = Integer.parseInt(ifEmpty(max, "0"));
		setId(new DecimalFormat("00000").format(Integer.valueOf(++lastID)));
	}
	@Override
	@Column(name="menu_id", callon="write")
	public String getId() {
		return super.getId();
	}
	@Override
	@Column(name="menu_id")
	public void setId(String id) {
		super.setId(id);
	}
	@Override
	@Column(name="menu_name", callon="write")
	public String getName() {
		return super.getName();
	}
	@Override
	@Column(name="menu_name")
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
	@Column(name="menu_action", callon="write")
	public String getAction() {
		return action;
	}
	@Column(name="menu_action")
	public void setAction(String action) {
		if (equals(this.action, action)) return;
		this.action = action;
		setModified("action");
	}
	@Column(name="setting", callon="write")
	public String getSetting() {
		return setting;
	}
	@Column(name="setting")
	public void setSetting(String setting) {
		if (equals(this.setting, setting)) return;
		this.setting = setting;
		setModified("setting");
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
	@Override
	protected void read(String fieldName, Object value) {
		if ("sort_ord".equals(fieldName))
			setSortOrder(asNumber(value).intValue());
		else
			super.read(fieldName, value);
	}
	@Override
	public Menu getParent() {
		return (Menu)parent;
	}
	@Override
	public Collection<? extends Menu> getChildren() {
		return (Collection<? extends Menu>)super.getChildren();
	}
}