<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
<head>
<link rel="stylesheet" href="http://www.muleumo.org/style/maven.css" type="text/css" media="all"></link>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"></meta>
<meta name="author" content="MuleSource, Inc."></meta>
<title>Mule Hello World</title>
</head>
<body>
<h2>Http POST</h2>
Http POST will send an event to the Mule server and return a result once the request has been <br/>
processed.  This example uses the Servlet Connector in the <a target="_blank" href="http://mule.mulesource.org/display/MULE2USER/HTTP+Transport">Http Transport</a>.
<p/>

Please enter your name:
<form method="POST" name="submitName" action="<%=request.getContextPath()%>/rest?endpoint=greeter">
    <table>
        <tr><td>
            <input type="text" name="payload"/></td><td><input type="submit" value="POST" />
        </td></tr>
    </table>
</form>

</body>
</html>
