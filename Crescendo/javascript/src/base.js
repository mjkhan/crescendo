function log(msg) {
	if (window.console && window.console.log)
		console.log(msg);
}

function trim(s) {
	if (null == s || undefined == s) return "";
	if ("string" != typeof(s)) return s;
	return s.replace(/^\s+/, "").replace(/\s+$/, "");
}

function isEmpty(v) {
	if (v == undefined || v == "undefined"
	 || v == null || v == "null") return true;
			
	switch (typeof(v)) {
	case "boolean": if (false == v) return false;
	case "number": if (0 == v) return false;
	default: return "" == trim(v);
	}
}

function ifEmpty(v, nv) {
	if (!isEmpty(v)) return v;
	if (typeof(nv) == "function")
		return nv.apply();
	else
		return nv;
}

function isNumber(v, strict) {
	return strict ?
		  !isNaN(v) :
		  isEmpty(v) || !isNaN(v.replace(/,/gi, ""));
}
/*
function timezoneOffset() {
	var tzo = -new Date().getTimezoneOffset(),
		prefix = tzo >= 0 ? "+" : "-",
		abs = Math.abs(tzo),
		hour = Math.floor(abs / 60),
		minute = abs % 60;
	if (hour < 10) hour = "0" + hour;
	if (minute < 10) minute = "0" + minute;
	return prefix + hour + ":" + minute;
}
*/
function toQuery(map, encode) {
	if (isEmpty(map)) return "";

	var query = [];
	for (var key in map) {
		var v = map[key];
		if (v != null && v == undefined) continue;
		if (v != null)
		switch (typeof(v)) {
		case "object":
		case "function": continue;
		}
		if (isEmpty(v))
			query.push(key);
		else
			query.push(key + "=" + (encode != false ? encodeURIComponent(v) : v));
	}
	return query.join("&");
}

function Eval(expr, debug) {
	if (debug == true)
		log(expr);
	try {
		return eval(expr);
	} catch (e) {
		alert(e.description + "\n\n" + expr);
		throw e;
	}
}

/*
var resp = ajax(url).get();
ajax(url).callback(function(resp) {}).get();

var resp = ajax(url).post(data);
ajax(url).callback(function(resp) {}).post(data);
*/

function ajax(url) {
	var req = {
		_callback: null,
		callback: function(callback) {
			req._callback = callback;
			return req;
		},
		send: function(method, data) {
			var opt = {
				type: method,
				url: url,
				async: !isEmpty(req._callback),
				data: data
			};
			if (opt.async)
				opt.success = req._callback;
			return opt.async ? $.ajax(opt) : trim($.ajax(opt).responseText);
		},
		get: function() {return req.send("GET", null);},
		post: function(data) {return req.send("POST", data);}
	};
	return req;
}

var requestHandler = {
	url: function(handler) {
		var site = ifEmpty(handler._site, requestHandler.siteID);
		return ifEmpty(requestHandler.prefix, "") + (isEmpty(site) ? "" : "/" + site) + "/" + handler.name;
	},
	data: function(handler, encode) {return toQuery(handler.values, encode);},
	extensions: {},
	get: function(name) {
		var obj = {
			name: name,
			siteID: function(siteID) {
				obj._site = siteID;
				return obj;
			},
			set: function(name, value) {
				if (value == null || value != undefined)
					obj.values[name] = value;
				return obj;
			},
			setMap: function(map, prefix) {
				var prefixed = !isEmpty(prefix);
				for (var key in map) {
					var value = map[key];
					if (value == undefined) continue;
					if (prefixed)
						key = prefix + key;
					obj.set(key, value);
				}
				return obj;
			},
			url: function() {return requestHandler.url(obj);},
			data: function(encode) {return requestHandler.data(obj, encode);},
			reset: function() {
				obj._site = null;
				obj.values = {};
				return this;
			},
			string: function() {
				var url = obj.url(),
					data = obj.data(true);
				obj.reset();
				if (isEmpty(data))
					return url;
				
				if (url.indexOf("?") < 0)
					url += "?";
				return url + data;
			},
			send: function(method, success) {
				var askWait = ifEmpty(obj.askWait, requestHandler.askWait);
				if (askWait)
					askWait();
				var result = ajax(obj.url()).callback(success).send(method, obj.data());
				obj.reset();
				return result;
			},
			get: function(success) {
				return obj.send("GET", success);
			},
			post: function (success) {
				return obj.send("POST", success);
			}
		};
		var extend = requestHandler.extensions[name];
		if (!isEmpty(extend))
			extend(obj);
		return obj.reset();
	}
};

function validate(info) {
	var input = info.input;
	if (!input || !info.test) return;
	
	var onblur = info.onblur != false;
	if (onblur)
		input.onblur = undefined;
	var v = input.value;
	if (info.test(v)) {	//if ok
		if (info.valid)
			info.vaild(v);
	} else {			//if not ok
		if (info.invalid)
			info.invalid(v);
		setTimeout(function(){
			if (onblur)
				input.onblur = function() {validate(info);};
			input.focus();
			input.select();
		}, 10);
	}
	return ok;
}
