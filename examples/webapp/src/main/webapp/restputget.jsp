<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
<head>
<link rel="stylesheet" href="http://www.muleumo.org/style/maven.css" type="text/css" media="all"></link>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"></meta>
<meta name="author" content="MuleSource, Inc."></meta>
<title>Mule REST Examples</title>
</head>
<body>
This example demostrates dispatching an event to Mule asynchronously using the <br/>Http Put method
and then receiving the event  using the Http Get method.
<br/>This example uses the Servlet Connector which is part of the <a target="_blank" href="http://mule.mulesource.org/display/MULE2USER/HTTP+Transport">Http Transport</a> to talk to Mule.
When a result is returned there <br/> will be in ascii text form so you'll need to hit the back button to return to this page.
<p>Task for the reader</p>
<h2>Http PUT</h2>
Http PUT will send an event asynchronously to the Mule server
<p/>
Please enter something:
<form method="POST" name="submitPut" action="<%=request.getContextPath()%>/rest/restPUTTest">
    <table>
        <tr><td>
            <input type="text" name="payload"/><input type="hidden" name="PUT"/></td><td><input type="submit" value=" PUT " />
        </td></tr>
    </table>
</form>
<p></p>
<h2>Http GET</h2>
Get get something back
<form method="GET" name="submitGet" action="<%=request.getContextPath()%>/rest">
    <table>
        <tr><td>
            <input type="hidden" name="endpoint" value="restFileEndpoint"/>
            <input type="submit" value=" GET " />
        </td></tr>
    </table>
</form>
</body>
</html>
