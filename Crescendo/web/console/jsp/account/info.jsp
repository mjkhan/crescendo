<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@page import="java.text.SimpleDateFormat, crescendo.system.Account" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<%!private static final SimpleDateFormat datetime = new SimpleDateFormat("yy-MM-dd HH:mm");%>
<c:if test="${actionFailed}"><jsp:include page="/actionFailed.jsp"/></c:if>
<c:if test="${!actionFailed}">
<crsnd:resource name="account" basepath="crescendo/account"/>
<c:if test="${action == 'get' || action == 'new'}">
<div style="padding-bottom:1em;">
	<button onclick="saveAccountInfo()"><crsnd:restring res="account" string="Save"/></button>
	<button onclick="closeAccountInfo();"><crsnd:restring res="account" string="Close"/></button>
</div>
<div id="account-info"><hrzn:dataset name="<%=Account.INFO%>"/>
	<table class="info-list" style="width:100%;"><hrzn:record dataName="<%=Account.INFO%>"><c:set var="state"><hrzn:record dataName="<%=Account.INFO%>" info="state"/></c:set>
		<tr><th><label for="user_id"><crsnd:restring res="account" string="ID"/></label></th>
			<td><input id="user_id" value="<hrzn:field name="user_id"/>" type="text" <c:if test="${'CREATED' != pageScope.state}">readonly</c:if><c:if test="${'CREATED' == pageScope.state}">required maxlength="<hrzn:field name="user_id" info="maxlength"/>" onblur="if (!isEmpty(this.value)) accountFieldsValid();"</c:if>></td>
		</tr>
		<tr><th><label for="alias"><crsnd:restring res="account" string="Account.alias"/></label></th>
			<td><input id="alias" value="<hrzn:field name="alias" pre="true"/>" type="text" maxlength="<hrzn:field name="alias" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="passwd"><crsnd:restring res="account" string="Password"/></label></th>
			<td><input id="passwd" value="<hrzn:field name="passwd"/>" type="password" required maxlength="<hrzn:field name="passwd" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="user_type"><crsnd:restring res="account" string="Type"/></label></th>
			<td><input id="user_type" value="<hrzn:field name="user_type" pre="true"/>" type="text" maxlength="<hrzn:field name="user_type" info="maxlength"/>"></td>
		</tr>
		<tr><th><label for="img_url"><crsnd:restring res="account" string="image"/></label></th>
			<td><input id="img_url" value="<hrzn:field name="img_url" pre="true"/>" type="text" maxlength="<hrzn:field name="img_url" info="maxlength"/>"></td>
		</tr>
		<c:if test="${'CREATED' != pageScope.state}">
		<tr><th><crsnd:restring res="account" string="created.at"/></th>
			<td><hrzn:field name="ins_time" format="<%=datetime%>"/></td>
		</tr>
		<tr><th><crsnd:restring res="account" string="last.modified"/></th>
			<td><hrzn:field name="upd_time" format="<%=datetime%>"/></td>
		</tr>
		</c:if>
		<tr><th><crsnd:restring res="account" string="Status"/></th>
			<td><crsnd:codestring field="status" mapper="<%=Account.Status.class%>" res="account"/></td>
		</tr>
		</hrzn:record>
	</table>
</div>
<script>
function accountFieldsEmpty() {
	var empty = false;
	$("#account-info input[required]").each(function(){
		empty = isEmpty($(this).val());
		if (!empty) return true;
		
		$(this).focus();
		var label = $("label[for=\"" + $(this).attr("id") + "\"]").text();
		showMessage("<crsnd:restring res="account" string="prompt.value"/>".replace("{0}", label));
		return false;
	});
	return empty;
}

function accountFieldsValid() {
<c:if test="${'CREATED' != pageScope.state}">return true;</c:if>
<c:if test="${'CREATED' == pageScope.state}">
	var result = accountHandler.validate("id", $("#user_id").val()).set("resp", "info").get(),
		valid = "true" == result;
	if (!valid) {
		$("#user_id").focus();
		switch (result) {
		case "invalid-account-id": showMessage("<crsnd:restring res="account" format="invalid.value" args="ID"/>"); break;
		case "account-id-in-use": showMessage("<crsnd:restring res="account" format="value.in.use" args="ID"/>"); break;
		case "reserved-account-id": showMessage("<crsnd:restring res="account" format="reserved.value" args="ID"/>"); break;
		}
		log("[" + result + "]");
	}
	return valid;
</c:if>
}

function saveAccountInfo() {
	if (accountFieldsEmpty()) return;
	if (!accountFieldsValid()) return;
	
	var accountInfo = {};
	$("#account-info input").each(function(){
		accountInfo[$(this).attr("id")] = $(this).val();
	});
	accountHandler.save("<c:if test="${'CREATED' == pageScope.state}">create</c:if><c:if test="${'CREATED' != pageScope.state}">update</c:if>", accountInfo).set("resp", "info").post(Eval);
}

function closeAccountInfo() {
	if (window.beforeAccountInfoClose) beforeAccountInfoClose();
	if (window.accountInfoClosed) accountInfoClosed();
}

setTimeout(function() {
	window.beforeAccountInfoClose = undefined;
	onEnterPress("#account-info input[type='text']", saveAccountInfo);
	$("#<c:if test="${'CREATED' != pageScope.state}">alias</c:if><c:if test="${'CREATED' == pageScope.state}">user_id</c:if>").focus();
}, 10);
</script>
</c:if>
<c:if test="${action == 'validate'}"><%
Account.Validator.Result result = (Account.Validator.Result)request.getAttribute("val-result");
out.write(result.isValid() ? "true" : result.getMessage());
%></c:if>
<c:if test="${action == 'create' || action == 'update'}">
<c:if test="${actionPerformed}">
if (window.reloadAccountInfo)
	reloadAccountInfo({id:"${account.id}", alias:"${account.alias}"});
showMessage("<crsnd:restring res="account" format="save.succeeded" args="Account"/>");
setTimeout(function(){
	window.beforeAccountInfoClose = function() {
		<c:if test="${action == 'create'}">if (window.afterAccountCreated) afterAccountCreated();</c:if>
		<c:if test="${action == 'update'}">if (window.afterAccountUpdated) afterAccountUpdated("${account.id}");</c:if>
		if (window.afterAccountSaved) afterAccountSaved("${account.id}");
	};
}, 100);
</c:if>
<c:if test="${!actionPerformed}">showMessage("<crsnd:restring res="account" format="save.failed" args="Account"/>");</c:if>
</c:if>
</c:if>