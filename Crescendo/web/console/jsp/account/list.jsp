<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@page import="java.text.SimpleDateFormat, crescendo.system.Account" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<%!private static final SimpleDateFormat datetime = new SimpleDateFormat("yy-MM-dd HH:mm");%>
<crsnd:resource name="account" basepath="crescendo/account"/>
<c:if test="${'search' == action}">
<div id="search-accounts">
<div style="padding-bottom:1em;">
	<input id="account-terms" type="text" placeholder="<crsnd:restring res="account" string="search.prompt.terms"/>">
	<button onclick="searchAccounts()"><crsnd:restring res="account" string="search"/></button>
	<span style="margin-left:7%;">
		<button onclick="newAccountInfo()"><crsnd:restring res="account" string="add"/></button>
		<button onclick="removeAccountInfo()" class="show-onselect"><crsnd:restring res="account" string="delete"/></button>
	</span>
</div>
<div id="account-list"><hrzn:cut when="${param.inline}">
	<table class="info-list striped" style="width:100%;"><c:set var="more"><hrzn:dataset name="<%=Account.LIST%>" info="has-more"/></c:set>
		<thead>
			<tr><th></th>
				<th><crsnd:restring res="account" string="ID"/></th>
				<th><crsnd:restring res="account" string="Account.alias"/></th>			
				<th><crsnd:restring res="account" string="last.modified"/></th>			
				<th><crsnd:restring res="account" string="Status"/></th>			
			</tr>
		</thead><c:set var="accountNotFound" scope="page"><tr><td colspan="5" style="text-align:center;"><crsnd:restring res="account" format="not.found" args="Account"/></td></tr></c:set>
		<tbody><hrzn:loopRecords dataName="<%=Account.LIST%>" ifEmpty="${pageScope.accountNotFound}"><c:set var="onclick">onclick="getAccountInfo('<hrzn:field name="user_id"/>')"</c:set>
			<tr><td><input name="account" value="<hrzn:field name="user_id"/>" type="radio"></td>
				<td ${onclick}><hrzn:field name="user_id"/></td>
				<td ${onclick}><hrzn:field name="alias" pre="true"/></td>
				<td ${onclick}><hrzn:field name="ins_time" format="<%=datetime%>"/></td>
				<td ${onclick}><crsnd:codestring field="status" mapper="<%=Account.Status.class%>" res="account"/></td>
			</tr></hrzn:loopRecords>
		</tbody>
	</table>
	<c:if test="${pageScope.more}"><div class="paging"><crsnd:restring res="account" string="more"/>: <hrzn:paginate dataName="<%=Account.LIST%>" attrs="onclick='searchAccounts(@{start})'"/></div></c:if></hrzn:cut><hrzn:cut write="true"/>
</div>
</div>
<div id="get-account" class="hidden"></div>
<crsnd:script src="/asset/js/account.js"/>
<%--
<c:set var="scriptSrc" scope="request">${requestScope.scriptSrc}
<script src="/asset/js/account.js"></script></c:set>
--%>
<c:set var="script" scope="request">${requestScope.script}
function initAccountChecks() {
	var showOnselect = function(checked) {
		var e = $("#search-accounts .show-onselect");
		if (checked) e.show();
		else e.hide({effect:"fade"});
	};
	$("input[name='account']").change(function(){showOnselect(true);});
	showOnselect(false);
}

function searchAccounts(start) {
	var req = {terms:$("#account-terms").val(), start:start, resp:"list", inline:true};
	accountHandler.search(req).get(function(resp) {
		$("#account-list").html(resp).show();
		initAccountChecks();
	});
	window.afterAccountUpdated = function(){searchAccounts(start);};
}
window.afterAccountUpdated = function(){searchAccounts();};

function showAccounts(search) {
	var hide = search != false ? "get-account" : "search-accounts",
		show = search != false ? "search-accounts" : "get-account";
	$("#" + hide).hide();
	$("#" + show).show({effect:"fade"});
}

function getAccountInfo(accountID) {
	accountHandler.getInfo(accountID).set("resp", "info").get(function(resp) {
		$("#get-account").html(resp);
		showAccounts(false);
	});
}
window.reloadAccountInfo = function(account) {
	if ("${client.id}" == account.id)
		setGreeting(account.alias);
	getAccountInfo(account.id);
}

function newAccountInfo() {
	accountHandler.newInfo().set("resp", "info").get(function(resp) {
		$("#get-account").html(resp);
		showAccounts(false);
		window.afterAccountCreated = searchAccounts;
	});
}

function removeAccountInfo() {
	Confirm({
		content: "<crsnd:restring res="account" format="confirm.action" args="delete,Account"/>",
		onOk: function() {
			var checked = checkbox("#account-list tbody input[type='radio']").value();
			accountHandler.set("resp", "list").remove(checked).post(Eval);
		}
	});
}

function accountInfoClosed() {
	showAccounts();
}

$("#account-terms").focus();
</c:set>
<c:set var="onload" scope="request">${requestScope.onload}
onEnterPress($("#account-terms"), searchAccounts);
initAccountChecks();
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
if (window.afterAccountUpdated) afterAccountUpdated();
showMessage("<crsnd:restring res="account" format="save.succeeded" args="Account"/>");
</c:if>
<c:if test="${!actionPerformed}">
showMessage("<crsnd:restring res="account" format="save.failed" args="Account"/>");
</c:if>
</c:if>