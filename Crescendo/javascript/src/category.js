requestHandler.extensions["categories"] = function(handler) {
	handler.feature = function(feature) {
		return handler.set("feature-id", feature);
	};
	handler.owner = function(id) {
		return handler.set("owner-id", id);
	}
	handler.search = function(req) {
		return handler.set("search").setMap(req);
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
	handler.validate = function(categoryInfo, state) {
		return handler.set("validate").set("state", state).setMap(categoryInfo, "category-info");
	};
	handler.create = function(category) {
		return handler.set("create").setMap(category, "category-info");
	};
	handler.setup = function(id) {
		return handler.set("setup", id);
	};
	handler.update = function(category) {
		return handler.set("update").setMap(category, "category-info");
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
var categoryHandler = requestHandler.get("categories");