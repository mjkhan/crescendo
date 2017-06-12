package crescendo.system.usergroup;

import java.io.Serializable;
import java.util.List;

import crescendo.system.Principal;
import crescendo.system.group.GroupEvent;

public class UserGroupEvent extends GroupEvent {
	private static final long serialVersionUID = 1L;
	public static final String REORDER_USER = "reorder-user";
	public static final String CHANGE_USER = "change-user";

	public UserGroup userGroup() {
		return UserGroup.class.cast(get(UserGroup.OBJ));
	}
	@Override
	public UserGroupEvent on() {
		switch (type())	 {
		case LOAD_SCTX:
			if (siteContext().get(Principal.OBJS) == null)
				loadUserGroups();
			break;
		case CREATE:
		case UPDATE:
			loadUserGroups();
			break;
		case REMOVE:
			add(removeSiteUsers());
			loadUserGroups();
			break;
		case CHANGE_USER:
			String[] userIDs = objects(SiteUser.ID);
			updateSiteUserGroups(userIDs);
			break;
		case ENTER_SCTX:
			String accountID = accountContext().account().getId();
			if (!siteContext().site().getOwnerID().equals(accountID))
				updateSiteUserGroups(accountID); break;
		}
		super.on();
		return this;
	}

	protected void loadUserGroups() {
		UserGroupServant servant = UserGroupServant.create(siteContext(), feature(), feature().string("servant"));
		UserGroup.Tree<UserGroup> usergroups = servant.getGroupTree();
		setLoad(Principal.OBJS, usergroups);
	}

	protected void updateSiteUserGroups(String... userIDs) {
		SiteUserServant servant = SiteUserServant.create(siteContext(), feature());
		servant.getUserGroupIDs(userIDs).forEach((key, value) ->
			setLoad(key, Principal.ID, (Serializable)value)
		);
	}

	protected UserGroupEvent removeSiteUsers() {
		List<String> groupIDs = (List<String>)get(feature().string("id"));
		if (isEmpty(groupIDs)) return null;
		SiteUserServant servant = SiteUserServant.create(siteContext(), feature());
		return servant.remove(groupIDs.toArray(new String[groupIDs.size()]), null);
	}
}