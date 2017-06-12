<%@page contentType="text/html;charset=UTF-8" session="false" isELIgnored="false"%>
<%@page import="crescendo.system.CrescendoException"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="hrzn" uri="horizon.tld"%>
<%@taglib prefix="crsnd" uri="crescendo.tld"%>
<crsnd:resource name="account" basepath="crescendo/account"/>
<div style="height:12em;"></div>
<table align="center" cellpadding="8" style="width:30%; border:1em solid pink;">
	<tr><td><crsnd:restring res="account" string="permission.denied"/></td>
	</tr>
	<tr><td style="text-align:right;">
			<button onclick="login()"><crsnd:restring res="account" string="back"/></button>
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
