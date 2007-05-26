<%@ page import="org.mule.extras.client.MuleClient,
                 org.mule.umo.UMOMessage"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<html>
<head>
<title>Mule Stock Quote Example</title>
</head>
<body>
<%
    String symbol = request.getParameter("symbol");
    if(symbol!=null) {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://stockquote", symbol, null);

        if(message!=null) { %>
            <%if(message.getExceptionPayload()!=null) {%>
                <h3>A Error occurred: <%=message.getExceptionPayload()%></h3>
           <%} else {%>
                <h3><%=message.getPayload()%></h3>
           <%}%>
        <%} else {%>
           <h3>Message returned is null and no error information is available. Check the logs for more information. Note you need an internet connection to invoke this service.</h3>
       <%}
   }%>
Enter a stock symbol:
<form method="POST" name="submitSymbol" action="">
    <table>
        <tr><td>
            <input type="text" name="symbol"/></td><td><input type="submit" name="Go" value=" Go " />
        </td></tr>
    </table>
</form>
<p/>
<table border="1" bordercolor="#990000"  align="left">
<tr><td>For more information about the Stock Quote example go <a target="_blank" href="http://www.muledocs.org/Stock+Quote+Example">here</a>.<br/>
To view the source and configuration go <a target="_blank" href="http://svn.mule.codehaus.org/browse/mule/trunk/mule/examples/stockquote/">here</a>.</td></tr>
</table>
</body>
</html>
