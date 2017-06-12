package crescendo.system.group;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import crescendo.system.Entity;
import crescendo.system.Feature;
import horizon.data.GenericMap;
import horizon.persistence.Column;
import horizon.persistence.Selector;
@Selector("select * from ${crsnd_group_member} where site_id = :site-id and grp_type = :group-type and and grp_id = :group-id and mb_type = :member-type and mb_id = :member-id")
public class Member extends Entity {
	private static final long serialVersionUID = 1L;

	public static List<Map<String, Object>> toList(String type, String... IDs) {
		return isEmpty(IDs) ? Collections.emptyList() :
			Arrays.asList(IDs).stream().map(
				id -> new GenericMap<Object>().caseSensitiveKey(false).set("mb_type", type).set("mb_id", id)
			).collect(Collectors.toList());
	}

	public static Config config(Feature feature) {
		return Config.get(feature, "member");
	}

	private String
		siteID,
		groupType,
		groupID,
		memberType,
		memberID,
		creatorID;
	private int sortOrder;
	private Timestamp createdAt;
	@Column(name="site_id", callon="write")
	public String getSiteID() {
		return siteID;
	}
	@Column(name="site_id")
	public void setSiteID(String siteID) {
		if (equals(this.siteID, siteID)) return;
		this.siteID = siteID;
		setModified();
	}
	@Column(name="grp_type", callon="write")
	public String getGroupType() {
		return groupType;
	}
	@Column(name="grp_type")
	public void setGroupType(String groupType) {
		if (equals(this.groupType, groupType)) return;
		this.groupType = groupType;
		setModified();
	}
	@Column(name="grp_id", callon="write")
	public String getGroupID() {
		return groupID;
	}
	@Column(name="grp_id")
	public void setGroupID(String groupID) {
		if (equals(this.groupID, groupID)) return;
		this.groupID = groupID;
		setModified();
	}
	@Column(name="mb_type", callon="write")
	public String getMemberType() {
		return memberType;
	}
	@Column(name="mb_type")
	public void setMemberType(String memberType) {
		if (equals(this.memberType, memberType)) return;
		this.memberType = memberType;
		setModified();
	}
	@Column(name="mb_id", callon="write")
	public String getMemberID() {
		return memberID;
	}
	@Column(name="mb_id")
	public void setMemberID(String memberID) {
		if (equals(this.memberID, memberID)) return;
		this.memberID = memberID;
		setModified();
	}
	@Column(name="ins_id", callon="write")
	public String getCreatorID() {
		return creatorID;
	}
	@Column(name="ins_id")
	public void setCreatorID(String creatorID) {
		if (equals(this.creatorID, creatorID)) return;
		this.creatorID = creatorID;
		setModified();
	}
	@Column(name="sort_ord", callon="write")
	public int getSortOrder() {
		return sortOrder;
	}
	@Column(name="sort_ord")
	public void setSortOrder(int sortOrder) {
		if (this.sortOrder == sortOrder) return;
		this.sortOrder = sortOrder;
		setModified();
	}
	@Column(name="ins_time", callon="write")
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	@Column(name="ins_time")
	public void setCreatedAt(Timestamp createdAt) {
		if (equals(this.createdAt, createdAt)) return;
		this.createdAt = createdAt;
		setModified();
	}
}