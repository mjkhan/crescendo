<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@page import="java.text.SimpleDateFormat, crescendo.system.Site" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<%!private static final SimpleDateFormat datetime = new SimpleDateFormat("yy-MM-dd HH:mm");%>
<crsnd:resource name="site" basepath="crescendo/site"/>
<c:if test="${action == 'get' || action == 'new'}">
<div style="padding-bottom:1em;">
	<button onclick="saveSiteInfo()"><crsnd:restring res="site" string="Save"/></button>
	<button onclick="closeSiteInfo();"><crsnd:restring res="site" string="Close"/></button>
</div>
<div id="site-info"><hrzn:dataset name="<%=Site.INFO%>"/>
	<table class="info-list" style="width:100%;"><hrzn:record dataName="<%=Site.INFO%>"><c:set var="state"><hrzn:record dataName="<%=Site.INFO%>" info="state"/></c:set>
		<tr><th><label for="site_id"><crsnd:restring res="site" string="ID"/></label></th>
			<td><input id="site_id" value="<hrzn:field name="site_id"/>" type="text" <c:if test="${'CREATED' != pageScope.state}">readonly</c:if><c:if test="${'CREATED' == pageScope.state}">required maxlength="<hrzn:field name="site_id" info="maxlength"/>" onblur="if (!isEmpty(this.value)) siteFieldsValid();"</c:if>></td>
		</tr>
		<tr><th><label for="site_name"><crsnd:restring res="site" string="Name"/></label></th>
			<td><input id="site_name" value="<hrzn:field name="site_name" pre="true"/>" type="text" required maxlength="<hrzn:field name="site_name" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="site_type"><crsnd:restring res="site" string="Type"/></label></th>
			<td><input id="site_type" value="<hrzn:field name="site_type" pre="true"/>" type="text" maxlength="<hrzn:field name="site_type" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="site_spc"><crsnd:restring res="site" string="Site.space"/></label></th>
			<td><input id="site_spc" value="<hrzn:field name="site_spc" pre="true"/>" type="text" maxlength="<hrzn:field name="site_spc" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="db_conn"><crsnd:restring res="site" string="Site.dbconn"/></label></th>
			<td><input id="db_conn" value="<hrzn:field name="db_conn" pre="true"/>" type="text" required maxlength="<hrzn:field name="db_conn" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="rd_conn"><crsnd:restring res="site" string="Site.rdconn"/></label></th>
			<td><input id="rd_conn" value="<hrzn:field name="rd_conn" pre="true"/>" type="text" maxlength="<hrzn:field name="rd_conn" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="profile"><crsnd:restring res="site" string="Profile"/></label></th>
			<td><input id="profile" value="<hrzn:field name="profile" pre="true"/>" type="text" maxlength="<hrzn:field name="profile" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="filebase"><crsnd:restring res="site" string="filebase"/></label></th>
			<td><input id="filebase" value="<hrzn:field name="filebase" pre="true"/>" type="text" maxlength="<hrzn:field name="filebase" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="evt_cfg"><crsnd:restring res="site" string="Site.event.config"/></label></th>
			<td><input id="evt_cfg" value="<hrzn:field name="evt_cfg" pre="true"/>" type="text" maxlength="<hrzn:field name="evt_cfg" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="job_cfg"><crsnd:restring res="site" string="Site.job.config"/></label></th>
			<td><input id="job_cfg" value="<hrzn:field name="job_cfg" pre="true"/>" type="text" maxlength="<hrzn:field name="job_cfg" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="ui_ctx"><crsnd:restring res="site" string="Site.ui.context"/></label></th>
			<td><input id="ui_ctx" value="<hrzn:field name="ui_ctx" pre="true"/>" type="text" maxlength="<hrzn:field name="ui_ctx" info="maxlength"/>"></td>
		</tr>
		<c:if test="${'CREATED' != pageScope.state}">
		<tr><th><crsnd:restring res="site" string="creator"/></th>
			<td><hrzn:field name="ins_name"/></td>
		</tr>
		<tr><th><crsnd:restring res="site" string="Site.owner"/></th>
			<td><input id="owner_id" value="<hrzn:field name="owner_id"/>" type="hidden"><hrzn:field name="owner_name"/></td>
		</tr>
		<tr><th><crsnd:restring res="site" string="created.at"/></th>
			<td><hrzn:field name="ins_time" format="<%=datetime%>"/></td>
		</tr>
		<tr><th><crsnd:restring res="site" string="last.modified"/></th>
			<td><hrzn:field name="upd_time" format="<%=datetime%>"/></td>
		</tr>
		<tr><th><crsnd:restring res="site" string="Status"/></th>
			<td><crsnd:codestring field="status" mapper="<%=Site.Status.class%>" res="site"/></td>
		</tr>
		</c:if></hrzn:record>
	</table>
</div>
<script>
function siteFieldsEmpty() {
	var empty = false;
	$("#site-info input[required]").each(function(){
		empty = isEmpty($(this).val());
		if (!empty) return true;
		
		$(this).focus();
		var label = $("label[for=\"" + $(this).attr("id") + "\"]").text();
		showMessage("<crsnd:restring res="site" string="prompt.value"/>".replace("{0}", label));
		return false;
	});
	return empty;
}

function siteFieldsValid() {
<c:if test="${'CREATED' != pageScope.state}">return true;</c:if>
<c:if test="${'CREATED' == pageScope.state}">
	var result = siteHandler.validate("id", $("#site_id").val()).set("resp", "info").get(),
		valid = "true" == result;
	if (!valid) {
		$("#site_id").focus();
		switch (result) {
		case "invalid-site-id": showMessage("<crsnd:restring res="site" format="invalid.value" args="ID"/>"); break;
		case "site-id-in-use": showMessage("<crsnd:restring res="site" format="value.in.use" args="ID"/>"); break;
		case "reserved-site-id": showMessage("<crsnd:restring res="site" format="reserved.value" args="ID"/>"); break;
		}
	}
	return valid;
</c:if>
}

function saveSiteInfo() {
	if (siteFieldsEmpty()) return;
	if (!siteFieldsValid()) return;
	
	var siteInfo = {};
	$("#site-info input").each(function(){
		siteInfo[$(this).attr("id")] = $(this).val();
	});
	siteHandler.save("<c:if test="${'CREATED' == pageScope.state}">create</c:if><c:if test="${'CREATED' != pageScope.state}">update</c:if>", siteInfo).set("resp", "info").post(Eval);
}

function closeSiteInfo() {
	if (window.beforeSiteInfoClose) beforeSiteInfoClose();
	if (window.siteInfoClosed) siteInfoClosed();
}

setTimeout(function() {
	window.beforeSiteInfoClose = undefined;
	onEnterPress("#site-info input[type='text']", saveSiteInfo);
	$("#<c:if test="${'CREATED' != pageScope.state}">site_name</c:if><c:if test="${'CREATED' == pageScope.state}">site_id</c:if>").focus();
}, 10);
</script>
</c:if>
<c:if test="${action == 'validate'}"><%
Site.Validator.Result result = (Site.Validator.Result)request.getAttribute("val-result");
out.write(result.isValid() ? "true" : result.getMessage());
%></c:if>
<c:if test="${action == 'create' || action == 'update'}">
<c:if test="${actionPerformed}">
if (window.reloadSiteInfo)
	reloadSiteInfo("${site.id}");
showMessage("<crsnd:restring res="site" format="save.succeeded" args="Site"/>");
setTimeout(function(){
	window.beforeSiteInfoClose = function() {
		<c:if test="${action == 'create'}">if (window.afterSiteCreated) afterSiteCreated();</c:if>
		<c:if test="${action == 'update'}">if (window.afterSiteUpdated) afterSiteUpdated("${site.id}");</c:if>
		if (window.afterSiteSaved) afterSiteSaved("${site.id}");
	};
}, 100);
</c:if>
<c:if test="${!actionPerformed}">showMessage("<crsnd:restring res="site" format="save.failed" args="Site"/>");</c:if>
</c:if>