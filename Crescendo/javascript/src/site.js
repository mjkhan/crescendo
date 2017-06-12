requestHandler.extensions["sites"] = function(handler) {
	handler.search = function(req) {
		return handler.setMap(req);
	};
	handler.getInfo = function(id) {
		return handler.set("get", id);
	};
	handler.viewInfo = function(id) {
		return handler.set("view", id);
	};
	handler.newInfo = function() {
		return handler.set("new");
	};
	handler.validate = function(name, value) {
		return handler.set("validate", name).set("value", value);
	};
	handler.save = function(action, site, attributes) {
		return handler.set(action).setMap(site, "site-info").setMap(attributes, "site-attr");
	};
	handler.setup = function(id) {
		return handler.set("setup", id);
	};
	handler.setStatus = function(status, id) {
		return handler.set(status, id);
	};
	handler.remove = function(id) {
		return handler.set("remove", id);
	};
};
var siteHandler = requestHandler.get("sites");