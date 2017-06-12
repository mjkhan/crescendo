requestHandler.extensions["accounts"] = function(handler) {
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
	handler.save = function(action, account, attributes) {
		return handler.set(action).setMap(account, "account-info").setMap(attributes, "account-attr");
	}
	handler.login = function(id, password) {
		return handler.setMap({login: id, pwd: password});
	};
	handler.logout = function(id) {
		return handler.set("logout", id);
	};
	handler.setStatus = function(status, id) {
		return handler.set(status, id);
	};
	handler.remove = function(id) {
		return handler.set("remove", id);
	};
};
var accountHandler = requestHandler.get("accounts");