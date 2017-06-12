<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<!DOCTYPE html>
<html><crsnd:resource name="site" basepath="crescendo/site"/>
<head><crsnd:resource name="account" basepath="crescendo/account"/>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
<title>Crescendo console</title>

<link rel="stylesheet" href="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/themes/smoothness/jquery-ui.css"/>

<style>
html, body, h1, h2, h3, h4, h5, h6, p, ol, ul, li, pre, code, address, variable, form, div, fieldset, blockquote, input, textarea {
	margin:0;
	padding:0;
	
	font:normal normal 100% Helvetica, Arial, sans-serif, dotum;

	box-sizing: border-box;
	-moz-box-sizing: border-box;
	-webkit-box-sizing: border-box;
	
	vertical-align: baseline;
}

html {width:100%; height:100%; font-size:0.8em;}
body {width:100%; height:100%;}

input[type="text"], input[type="password"], button {padding:0.3em;}
input[readonly] {border:none}
table {border-collapse:collapse; border-spacing:0;}
td, th, caption {font-weight:normal; text-align:left;}
.striped tbody tr:nth-child(even) {background-color:#f0f0f0;}

.info-list {border-top:2px solid #d3d3d3; border-bottom:2px solid #d3d3d3;}
.info-list thead {background-color:#f0f0f0; border-bottom:1px solid #d3d3d3;}
.info-list th, .info-list td {padding:0.5em; max-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;}
.paging {padding:1em; text-align:center;}

img, fieldset {border:0;}

ol {padding-left:1.4em; list-style:decimal;}
ul {padding-left:1.4em; list-style:square;}

q:before, q:after {content:'';}
a:hover {text-decoration:underline;}

*[onclick], *[href] {cursor:pointer;}
button {min-width:5%;}

.hidden, .show-onselect {display:none;}
</style>
</head>

<body>
<div style="width:100%; height:5em; padding:0.5%; color:white; background-color:purple; ">
	<span style="float:left; margin-left:10%; font:2em bold;">Crescendo Console</span>
	<div style="float:right; margin-right:10%; text-align:right;">
	<c:if test="${!client.unknown}">
		<span id="greeting"></span> | 
		<a onclick="logout()"><crsnd:restring res="account" string="Logout"/></a><br />
	</c:if>
	<span id="msg" style="float:right;"></span>
	</div>
	<br clear="both"/>
</div>
<div style="width:100%; height:92%; background-color:#f0f0f0;">
	<div style="width:80%; min-height:100%; border:none; margin:0 auto; background-color:#ffffff;">
		<c:if test="${!client.unknown}"><div id="navs" style="width:100%; height:6%; border:none;">
			<ul><li><a href="#content"><crsnd:restring res="site" string="Site"/></a></li>
				<li><a id="accounts"><crsnd:restring res="account" string="Account"/></a></li>
			</ul>
			<div id="content" style="width:100%;"><jsp:include page="/site/list.jsp"/></div>
		</div></c:if>
		<c:if test="${client.unknown}"><jsp:include page="/login.jsp"/></c:if>
	</div>
</div>

<!--[if lt ie 9]><script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script><![endif]-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
<script src="/asset/js/base.js"></script>
<script src="/asset/js/page.js"></script>
<script src="/asset/js/account.js"></script>
<crsnd:script write="true"/>
<%--${requestScope.scriptSrc}--%>
<script>
function showMessage(s) {
	message("msg").set(s);
}

requestHandler.prefix = "${requestScope['javax.servlet.forward.request_uri']}";
requestHandler.siteID = "${requestScope['site-id']}";
requestHandler.askWait = function() {
	showMessage("<crsnd:restring res="account" string="wait"/>");
};
dialog.title = "Crescendo Console";

<c:if test="${!client.unknown}">
function setGreeting(alias) {
	$("#greeting").html("<crsnd:restring res="account" string="welcome.user"/>".replace("{0}", alias));
}

function logout() {
	accountHandler.set("resp", "logout").logout("${client.id}").post(Eval);
}
</c:if>
${requestScope.script}

$(function(){<c:if test="${!client.unknown}">
	setGreeting("${client.name}");
	$("#navs a[id]").each(function(){
		var e = $(this);
		e.attr("href", requestHandler.prefix + "/${requestScope['site-id']}/" + e.attr("id") + "?resp=list&include=false");
	});
	$("#navs").tabs();</c:if>
	${requestScope.onload}
});
</script>
</body>
</html>
