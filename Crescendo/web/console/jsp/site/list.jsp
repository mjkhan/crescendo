<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@page import="java.text.SimpleDateFormat, crescendo.system.Site" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<%!private static final SimpleDateFormat datetime = new SimpleDateFormat("yy-MM-dd HH:mm");%>
<crsnd:resource name="site" basepath="crescendo/site"/>
<c:if test="${'search' == action}">
<div id="search-sites">
<div style="padding-bottom:1em;">
	<input id="site-terms" type="text" placeholder="<crsnd:restring res="site" string="search.prompt.terms"/>">
	<button onclick="searchSites()"><crsnd:restring res="site" string="search"/></button>
	<span style="margin-left:7%;">
		<button onclick="newSiteInfo()"><crsnd:restring res="site" string="add"/></button>
		<button onclick="removeSiteInfo()" class="show-onselect"><crsnd:restring res="site" string="delete"/></button>
	</span>
</div>
<div id="site-list"><hrzn:cut when="${param.inline}">
	<table class="info-list striped" style="width:100%;"><c:set var="more"><hrzn:dataset name="<%=Site.LIST%>" info="has-more"/></c:set>
		<thead>
			<tr><th></th>
				<th><crsnd:restring res="site" string="ID"/></th>
				<th><crsnd:restring res="site" string="Name"/></th>			
				<th><crsnd:restring res="site" string="Site.owner"/></th>			
				<th><crsnd:restring res="site" string="last.modified"/></th>			
				<th><crsnd:restring res="site" string="Status"/></th>			
			</tr>
		</thead><c:set var="siteNotFound" scope="page"><tr><td colspan="6" style="text-align:center;"><crsnd:restring res="site" format="not.found" args="Site"/></td></tr></c:set>
		<tbody><hrzn:loopRecords dataName="<%=Site.LIST%>" ifEmpty="${pageScope.siteNotFound}"><c:set var="onclick">onclick="getSiteInfo('<hrzn:field name="site_id"/>')"</c:set>
			<tr><td><input name="site" value="<hrzn:field name="site_id"/>" type="radio"></td>
				<td ${onclick}><hrzn:field name="site_id"/></td>
				<td ${onclick}><hrzn:field name="site_name" pre="true"/></td>
				<td ${onclick}><hrzn:field name="owner_name"/></td>
				<td ${onclick}><hrzn:field name="ins_time" format="<%=datetime%>"/></td>
				<td ${onclick}><crsnd:codestring field="status" mapper="<%=Site.Status.class%>" res="site"/></td>
			</tr></hrzn:loopRecords>
		</tbody>
	</table>
	<c:if test="${pageScope.more}"><div class="paging"><crsnd:restring res="site" string="more"/>: <hrzn:paginate dataName="<%=Site.LIST%>" attrs="onclick='searchSites(@{start})'"/></div></c:if></hrzn:cut><hrzn:cut write="true"/>
</div>
</div>
<div id="get-site" class="hidden"></div>
<crsnd:script src="/asset/js/site.js"/>
<%--
<c:set var="scriptSrc" scope="request">${requestScope.scriptSrc}
<script src="/asset/js/site.js"></script></c:set>
--%>
<c:set var="script" scope="request">${requestScope.script}
function initSiteChecks() {
	var showOnselect = function(checked) {
		var e = $("#search-sites .show-onselect");
		if (checked) e.show();
		else e.hide({effect:"fade"});
	};
	$("input[name='site']").change(function(){showOnselect(true);});
	showOnselect(false);
}

function searchSites(start) {
	var req = {terms:$("#site-terms").val(), start:start, resp:"list", inline:true};
	siteHandler.search(req).get(function(resp) {
		$("#site-list").html(resp).show();
		initSiteChecks();
	});
	window.afterSiteUpdated = function(){searchSites(start);};
}
window.afterSiteUpdated = function(){searchSites();};

function showSites(search) {
	var hide = search != false ? "get-site" : "search-sites",
		show = search != false ? "search-sites" : "get-site";
	$("#" + hide).hide();
	$("#" + show).show({effect:"fade"});
}

function getSiteInfo(siteID) {
	siteHandler.getInfo(siteID).set("resp", "info").get(function(resp) {
		$("#get-site").html(resp);
		showSites(false);
	});
}
window.reloadSiteInfo = getSiteInfo;

function newSiteInfo() {
	siteHandler.newInfo().set("resp", "info").get(function(resp) {
		$("#get-site").html(resp);
		showSites(false);
		window.afterSiteCreated = searchSites;
	});
}

function removeSiteInfo() {
	Confirm({
		content: "<crsnd:restring res="site" format="confirm.action" args="delete,Site"/>",
		onOk: function() {
			var checked = checkbox("#site-list tbody input[type='radio']").value();
			siteHandler.set("resp", "list").remove(checked).post(Eval);
		}
	});
}

function siteInfoClosed() {
	showSites();
}

$("#site-terms").focus();
</c:set>
<c:set var="onload" scope="request">${requestScope.onload}
onEnterPress($("#site-terms"), searchSites);
initSiteChecks();
</c:set>
<c:if test="${'false' == param.include}">
${requestScope.scriptSrc}
<script>
${requestScope.script}
${requestScope.onload}
</script>
</c:if>
</c:if>
<c:if test="${'remove' == action}">
<c:if test="${actionPerformed}">
if (window.afterSiteUpdated) afterSiteUpdated();
showMessage("<crsnd:restring res="site" format="save.succeeded" args="Site"/>");
</c:if>
<c:if test="${!actionPerformed}">
showMessage("<crsnd:restring res="site" format="save.failed" args="Site"/>");
</c:if>
</c:if>