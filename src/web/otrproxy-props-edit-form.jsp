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
    boolean save = request.getParameter("save") != null;
    boolean reset = request.getParameter("reset") !=null;
    boolean success = request.getParameter("success") != null;
    
    boolean pluginEnabled = ParamUtils.getBooleanParameter(request, "pluginenabled");
    
	Map<String, String> errors = new HashMap<String, String>();
	otrProxy plugin = (otrProxy) XMPPServer.getInstance().getPluginManager().getPlugin("otrproxy");

    if (save) {
		    plugin.setPluginEnabled(pluginEnabled);
	        response.sendRedirect("otrproxy-props-edit-form.jsp?success=true");
	        return;
    } else if (reset) {
      plugin.reset();
      response.sendRedirect("otrproxy-props-edit-form.jsp?success=true");
    }
	
    pluginEnabled = plugin.isPluginEnabled();

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

<%  } else if (errors.size() > 0) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""></td>
        	<td class="jive-icon-label">Error saving the settings.</td>
        </tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="otrproxy-props-edit-form.jsp" method="post">

<fieldset>
    <legend>otrProxy</legend>
    <div>
    
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
    	<tr>
    	    <td width="1%">
            <input type="radio" name="pluginenabled" value="false" id="not01"
             <%= ((pluginEnabled) ? "" : "checked") %>>
        </td>
        <td width="99%">
            <label for="not01"><b>Disabled</b></label> - otrProxy disabled.
        </td>
    </tr>
    <tr>
        <td width="1%">
            <input type="radio" name="pluginenabled" value="true" id="not02"
             <%= ((pluginEnabled) ? "checked" : "") %>>
        </td>
        <td width="99%">
            <label for="not02"><b>Enabled</b></label> - otrProxy enabled.
        </td>
    </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br><br>

<input type="submit" name="save" value="Save settings">
<input type="submit" name="reset" value="Restore factory settings*">
</form>

<br><br>

<em>*Restores the plugin to its factory state, you will lose all changes ever made to this plugin!</em>
</body>
</html>