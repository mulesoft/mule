<%@ page import="org.mule.examples.loanbroker.esn.LoanBrokerApp,
                 org.mule.examples.loanbroker.messages.Customer,
                 org.mule.examples.loanbroker.messages.CustomerQuoteRequest,
                 org.mule.extras.client.MuleClient,
                 org.mule.umo.UMOMessage,
                 java.util.Iterator,
                 java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<html>
<head>
<title>Mule Loan Broker Example</title>
</head>
<body>
<%
    String amountString = request.getParameter("amount");
    String durationString = request.getParameter("duration");
    String name = request.getParameter("name");
    String random = request.getParameter("random");

    if(random!=null) {
        LoanBrokerApp consumer = new LoanBrokerApp();
        int requests = Integer.parseInt(random);
        //to get all the result and print them out
        List results = consumer.requestSend(requests, "CustomerRequests");
        %>
        <b>You have just made <%=requests%> Loan Requests!</b>
        <ol>
        <% for(Iterator iter = results.iterator(); iter.hasNext();) {
            %><li><%=iter.next().toString()%></li><%
        }%>
        </ol>

        <%
    } else if(amountString!=null && durationString!=null && name!=null) {
        MuleClient client = new MuleClient();
        Customer cust = new Customer(name, 1234);
        double amount = Double.valueOf(amountString).doubleValue();
        int duration = Integer.parseInt(durationString);
        CustomerQuoteRequest loanRequest = new CustomerQuoteRequest(cust, amount,  duration);
        UMOMessage message = client.send("CustomerRequests", loanRequest, null);
        %>
<h3>The best quote was received from: <br/> <%=message.getPayload()%></h3>
     <%} else {%>
<form method="POST" name="submitRequest" action="">
    Send 10 random requests:
    <table>
        <tr><td>
            <input type="hidden" name="random" value="10"/><input type="submit" name="submit" value="Submit" />
        </td></tr>
    </table>
</form>
<br/>- Or -<br/>
<form method="POST" name="submitRequest" action="">
    Please enter your Loan Details:
    <table>
        <tr><td>Name: </td><td>
            <input type="text" name="name"/>
        </td></tr>
        <tr><td>Amount: </td><td>
            <input type="text" name="amount"/>
        </td></tr>
        <tr><td>Duration: </td><td>
            <input type="text" name="duration"/>
        </td></tr>

        <tr><td colspan="2">
            <input type="submit" name="submit" value="Submit" />
        </td></tr>
    </table>
</form>
<%}%>

<p/>
<table border="1" bordercolor="#990000"  align="left">
<tr><td>For more information about Loan Broker example go <a target="_blank" href="http://www.muledocs.org/display/MULE/Loan+Broker+Example">here</a>.<br/>
To view the source and configuration go <a target="_blank" href="http://svn.mule.codehaus.org/browse/mule/trunk/mule/examples/loanbroker/">here</a>.</td></tr>
</table>
</body>
</html>
