<%@ page import="org.mule.example.bookstore.Book,
 				 org.mule.example.bookstore.Bookstore,
 				 java.util.Collection,
				 java.util.Iterator"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<html>
<head>
<title>Bookstore Administration Console</title>
</head>
<body>

<form method="POST" name="addBook" action="/bookstore-admin/rest/bookstore">
    Add a new book:
    <table>
        <tr><td>Title: </td><td>
            <input type="text" name="title"/>
        </td></tr>
        <tr><td>Author: </td><td>
            <input type="text" name="author"/>
        </td></tr>

        <tr><td colspan="2">
            <input type="submit" name="submit" value="Submit" />
        </td></tr>
    </table>
</form>

<form method="GET" name="getLastOrder" action="/bookstore-admin/rest">
    Get last order placed:
    <table>
        <tr><td colspan="2">
            <input type="hidden" name="endpoint" value="orders"/>
            <input type="submit" name="submit" value="Submit" />
        </td></tr>
    </table>
</form>

<p/>
<table border="1" bordercolor="#990000"  align="left">
<tr><td>For more information about the Bookstore example go <a target="_blank" href="http://mule.mulesource.org/display/MULE2INTRO/Bookstore+Example">here</a>.</td></tr>
</table>
</body>
</html>
