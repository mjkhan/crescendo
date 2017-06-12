package crescendo.system.group;

import crescendo.system.SiteContextEvent;

public class GroupEvent extends SiteContextEvent {
	private static final long serialVersionUID = 1L;

	public static final String MOVE = Group.MOVE;
	public static final String REORDER = Group.REORDER;
	public static final String CHANGE_STATUS = Group.CHANGE_STATUS;
}