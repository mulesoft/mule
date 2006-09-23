<%@ page import="org.mule.extras.client.MuleClient,
                 org.mule.umo.UMOMessage"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<html>
<head>
<title>Mule Hello World</title>
</head>
<body>
<%
    String name = request.getParameter("name");
    if(name!=null) {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://greeter", name, null);
%>
<h3><%=message.getPayload()%></h3>
     <%}%>
Please enter your name:
<form method="POST" name="submitName" action="">
    <table>
        <tr><td>
            <input type="text" name="name"/></td><td><input type="submit" name="Go" value=" Go " />
        </td></tr>
    </table>
</form>
<p/>
<table border="1" bordercolor="#990000"  align="left">
<tr><td>For more information about Hello world example go <a target="_blank" href="http://mule.codehaus.org/Hello+World+Example">here</a>.<br/>
To view the source and configuration go <a target="_blank" href="http://svn.mule.codehaus.org/browse/mule/trunk/mule/examples/hello/">here</a>.</td></tr>
</table>
</body>
</html>
