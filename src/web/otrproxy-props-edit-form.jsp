<%@ page import="java.util.*,
                 org.jivesoftware.openfire.XMPPServer,
                 org.jivesoftware.openfire.user.*,
                 org.heilerich.otrProxy,
                 org.jivesoftware.util.*"
%>
<%@ page import="java.util.regex.Pattern"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
	int MINCMD = 3;
    boolean save = request.getParameter("save") != null;
    boolean reset = request.getParameter("reset") !=null;
    boolean success = request.getParameter("success") != null;
	boolean	cmderr = request.getParameter("error") != null;
    
    boolean pluginEnabled = ParamUtils.getBooleanParameter(request, "pluginenabled");
	String cmdStatus = ParamUtils.getParameter(request, "cmdstatus");
	String cmdStart = ParamUtils.getParameter(request, "cmdstart");
	String cmdStop = ParamUtils.getParameter(request, "cmdstop");
	String cmdRefresh = ParamUtils.getParameter(request, "cmdrefresh");
	String cmdMyfp = ParamUtils.getParameter(request, "cmdmyfp");
	String cmdFp = ParamUtils.getParameter(request, "cmdfp");
	String cmdVerify = ParamUtils.getParameter(request, "cmdverify");
	String cmdUnverify = ParamUtils.getParameter(request, "cmdunverify");
	String cmdHelp = ParamUtils.getParameter(request, "cmdhelp");
	String propKeyfile = ParamUtils.getParameter(request, "propkeyfile");
    
	otrProxy plugin = (otrProxy) XMPPServer.getInstance().getPluginManager().getPlugin("otrproxy");

    if (save) {
		cmderr = !(cmdStatus.length()>MINCMD
						&& cmdStart.length()>MINCMD
						&& cmdStop.length()>MINCMD
						&& cmdRefresh.length()>MINCMD
						&& cmdMyfp.length()>MINCMD
						&& cmdFp.length()>MINCMD
						&& cmdVerify.length()>MINCMD
						&& cmdUnverify.length()>MINCMD
						&& cmdHelp.length()>MINCMD);
		if(!cmderr) {
			plugin.setPluginEnabled(pluginEnabled);
			plugin.setCmdStatus(cmdStatus);
			plugin.setCmdStart(cmdStart);
			plugin.setCmdStop(cmdStop);
			plugin.setCmdRefresh(cmdRefresh);
			plugin.setCmdMyfp(cmdMyfp);
			plugin.setCmdFp(cmdFp);
			plugin.setCmdVerify(cmdVerify);
			plugin.setCmdUnverify(cmdUnverify);
			plugin.setCmdHelp(cmdHelp);
			plugin.setPropKeyfile(propKeyfile);
			response.sendRedirect("otrproxy-props-edit-form.jsp?success=true");
		} else {
			response.sendRedirect("otrproxy-props-edit-form.jsp?error=cmd");
		}
		return;
    } else if (reset) {
		plugin.reset();
		response.sendRedirect("otrproxy-props-edit-form.jsp?success=true");
    }
	
    pluginEnabled = plugin.isPluginEnabled();
	cmdStatus = plugin.getCmdStatus();
	cmdStart = plugin.getCmdStart();
	cmdStop = plugin.getCmdStop();
	cmdRefresh = plugin.getCmdRefresh();
	cmdMyfp = plugin.getCmdMyfp();
	cmdFp = plugin.getCmdFp();
	cmdVerify = plugin.getCmdVerify();
	cmdUnverify = plugin.getCmdUnverify();
	cmdHelp = plugin.getCmdHelp();
	propKeyfile = plugin.getPropKeyfile();

%>

<html>
    <head>
        <title>otrProxy</title>
        <meta name="pageID" content="otrproxy-props-edit-form"/>
    </head>
    <body>

<p>
Use the form below to edit otrproxy settings.<br>
</p>

<%  if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
	        <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
	        <td class="jive-icon-label">Settings updated successfully.</td>
        </tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (cmderr) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""></td>
        	<td class="jive-icon-label">Error saving the settings. Commands must be at least 4 characters long.</td>
        </tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="otrproxy-props-edit-form.jsp" method="post">

<div class="jive-contentBoxHeader">
	otrProxy Service
</div>
<div class="jive-contentBox">
	<table border="0" cellpadding="3" cellspacing="0">
	<tbody>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="pluginenabled" value="true" type="radio" <%= ((pluginEnabled) ? "checked" : "") %>>
		</td>
		<td width="99%">
			<label for="rb02">
				<b>Enabled</b>
				- otrProxy Service will be enabled
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="pluginenabled" value="false" type="radio" <%= ((pluginEnabled) ? "" : "checked") %>>
		</td>
		<td width="99%">
			<label for="rb01">
				<b>Disabled</b>
				- otrProxy Service will be disabled
			</label>
		</td>
	</tr>
	</tbody>
	</table>
</div>

<div class="jive-contentBoxHeader">
	Chat Commands
</div>
<div class="jive-contentBox">
	<table border="0" cellpadding="3" cellspacing="0">
	<tbody>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdstart" value="<%= cmdStart %>" type="text">
		</td>
		<td width="99%">
			<label>
				- start otr session
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdstop" value="<%= cmdStop %>" type="text">
		</td>
		<td width="99%">
			<label>
				- end otr session
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdstatus" value="<%= cmdStatus %>" type="text">
		</td>
		<td width="99%">
			<label>
				- show encrpytion and verification status of the session
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdrefresh" value="<%= cmdRefresh %>" type="text">
		</td>
		<td width="99%">
			<label>
				- refresh otr session
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdmyfp" value="<%= cmdMyfp %>" type="text">
		</td>
		<td width="99%">
			<label>
				- show your own public key fingerprint
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdfp" value="<%= cmdFp %>" type="text">
		</td>
		<td width="99%">
			<label>
				- show your buddy's public key fingerprint for verification
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdverify" value="<%= cmdVerify %>" type="text">
		</td>
		<td width="99%">
			<label>
				- mark your buddy's public key fingerprint as verified
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdunverify" value="<%= cmdUnverify %>" type="text">
		</td>
		<td width="99%">
			<label>
				- mark your buddy's public key fingerprint as unverified
			</label>
		</td>
	</tr>
	<tr valign="middle">
		<td nowrap="nowrap" width="1%">
			<input name="cmdhelp" value="<%= cmdHelp %>" type="text">
		</td>
		<td width="99%">
			<label>
				- show help message
			</label>
		</td>
	</tr>
	</tbody>
	</table>
</div>

<div class="jive-contentBoxHeader">
	Key Storage File
</div>
<div class="jive-contentBox">
	<table border="0" cellpadding="3" cellspacing="0">
	<tbody>
		<td nowrap="nowrap" width="1%">
			<input name="propkeyfile" value="<%= propKeyfile %>" type="text">
		</td>
		<td width="99%">
			<label>
				- path to Key Storage File
			</label>
		</td>
	</tbody>
	</table>
</div>

<br><br>

<input type="submit" name="save" value="Save settings">
<input type="submit" name="reset" value="Restore factory settings*">
</form>
</body>
</html>