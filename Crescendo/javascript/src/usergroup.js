requestHandler.extensions["usergroups"] = function(handler) {
	handler.search = function(req) {
		return handler.set("search").setMap(req);
	};
	handler.getInfo = function(id) {
		return handler.set("get", id);
	};
	handler.newInfo = function() {
		return handler.set("new");
	};
	handler.validate = function(usergroupInfo, state) {
		return handler.set("validate").set("state", state).setMap(usergroupInfo, "usergroup-info");
	};
	handler.create = function(usergroup) {
		return handler.set("create").setMap(usergroup, "usergroup-info");
	};
	handler.update = function(usergroup) {
		return handler.set("update").setMap(usergroup, "usergroup-info");
	};
	handler.remove = function(id) {
		return handler.set("remove", id);
	};
	handler.searchUser = function(req) {
		return handler.set("users").setMap(req);
	};
	handler.addUser = function(accountIDs, groupID) {
		return handler.set("add", accountIDs).set("groupID", groupID);
	};
	handler.removeUser = function(accountIDs, groupID) {
		return handler.set("remove-user", accountIDs).set("groupID", groupID);
	}
};
var usergroupHandler = requestHandler.get("usergroups");