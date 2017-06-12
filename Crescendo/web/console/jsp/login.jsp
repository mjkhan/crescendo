<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<crsnd:resource name="account" basepath="crescendo/account"/>
<c:if test="${'login' != param.resp && 'logout' != param.resp}">
<div style="height:12em;"></div>
<table align="center" cellpadding="8" style="width:25%; border:1em solid pink;">
	<tr><td><crsnd:restring res="account" string="ID"/></td>
		<td><input id="user-id" type="text" placeholder="<crsnd:restring res="account" format="prompt.value" args="ID"/>" style="width:100%;"></td>
	</tr>
	<tr><td><crsnd:restring res="account" string="Password"/></td>
		<td><input id="password" type="password" placeholder="<crsnd:restring res="account" format="prompt.value" args="Password"/>" style="width:100%;"></td>
	</tr>
	<tr><td colspan="2" style="text-align:right;">
			<crsnd:restring res="account" string="Loggedin.keep"/> <input id="remember" type="checkbox">
			<button onclick="login()"><crsnd:restring res="account" string="Login"/></button>
		</td>
	</tr>	
</table>
<c:set var="script" scope="request">${requestScope.script}
function inputValue(id) {
	var input = $(id),
		value = input.val();
	if (!isEmpty(value)) return value;
	
	input.focus();
	return null;
}

function login() {
	var userID = inputValue("#user-id");
	if (userID == null) return;
	var password = inputValue("#password");
	if (password == null) return;
	accountHandler.set("remember", $("#remember").is(":checked") ? true : undefined).set("resp", "login").login(userID, password).post(Eval);
}
</c:set>
<c:set var="onload" scope="request">${requestScope.onload}
onEnterPress($("input[id]"), login);
$("#user-id").focus();
</c:set>
</c:if>
<c:if test="${!empty param.login}">
<c:if test="${loggedin}">document.location.reload();</c:if>
<c:if test="${!loggedin}">
showMessage("<crsnd:restring res="account" string="Login.failure.reason"/>");
$("#user-id").focus().select();
</c:if>
</c:if>
<c:if test="${!empty param.logout}">document.location.reload();</c:if>