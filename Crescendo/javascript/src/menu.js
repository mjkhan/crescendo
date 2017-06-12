requestHandler.extensions["menus"] = function(handler) {
	handler.type = function(type) {
		return handler.set("menu-type", type);
	};
	handler.getInfo = function(id) {
		return handler.set("get", id);
	};
	handler.newInfo = function() {
		return handler.set("new");
	};
	handler.validate = function(menuInfo, state) {
		return handler.set("validate").set("state", state).setMap(menuInfo, "menu-info");
	};
	handler.create = function(menu) {
		return handler.set("create").setMap(menu, "menu-info");
	};
	handler.setup = function(id) {
		return handler.set("setup", id);
	};
	handler.update = function(menu) {
		return handler.set("update").setMap(menu, "menu-info");
	};
	handler.move = function(id, to) {
		return handler.set("move", id).set("to", to);
	};
	handler.reorder = function(id, offset) {
		return handler.set("reorder", id).set("offset", offset);
	};
	handler.setStatus = function(status, id) {
		return handler.set(status, id);
	};
	handler.remove = function(id) {
		return handler.set("remove", id);
	};
};
var menuHandler = requestHandler.get("menus");