<%@ page import="org.mule.example.bookstore.Book,
 				 org.mule.example.bookstore.Bookstore,
 				 java.util.Collection,
				 java.util.Iterator,
				 org.apache.cxf.jaxws.JaxWsProxyFactoryBean,
				 org.apache.cxf.BusFactory"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<html>
<head>
<title>Bookstore</title>
</head>
<body>
<%
    String title = request.getParameter("title");
    String author = request.getParameter("author");

    if (title!=null || author!=null) {
    	BusFactory.setDefaultBus(null);
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Bookstore.class);
        pf.setAddress("http://localhost:8777/services/bookstore");
        Bookstore bookstore = (Bookstore) pf.create();

        Collection < Book > books = bookstore.getBooks();
        %>
        Request returned <%=books.size()%> book(s)<br/>
        <br/>

        <%
        for (Iterator i = books.iterator(); i.hasNext();)
        {
            Book book = (Book) i.next();
            %>
            Title: <%=book.getTitle()%><br/>
            Author: <%=book.getAuthor()%><br/>
            <br/><%
        }
     } else {%>
<form method="POST" name="submitRequest" action="">
    Search for a book:
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
<%}%>

<p/>
<table border="1" bordercolor="#990000"  align="left">
<tr><td>For more information about the Bookstore example go <a target="_blank" href="http://mule.mulesource.org/display/MULE2INTRO/Bookstore+Example">here</a>.</td></tr>
</table>
</body>
</html>
