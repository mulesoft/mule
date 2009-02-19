<%@ page import="org.mule.example.bookstore.Book,
                  org.mule.example.bookstore.CatalogService,
                  java.util.ArrayList,
                  java.util.Collection,
                 java.util.Iterator,
                 org.apache.cxf.jaxws.JaxWsProxyFactoryBean"%>
<%@ page language="java" %>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
<title>On-line Bookstore</title>
</head>

<body link="#FFFFFF" vlink="#FFFFFF" alink="#FFFFFF" bgcolor="#000055" text="#FFFFFF">

<%
    String field = request.getParameter("title");
    String title = field != null ? field : "";
    field = request.getParameter("author");
    String author = field != null ? field : "";
%>

<center><h3>Welcome to the Mule-powered On-line Bookstore</h3></center>
<hr/>
    
<h2>Search for a book</h2>
<form method="POST" name="submitRequest" action="">
    <table>
        <tr>
            <td>Title: </td>
            <td><input type="text" name="title" value="<%=title%>"/></td>
           </tr>
        <tr>
            <td>Author: </td>
            <td><input type="text" name="author" value="<%=author%>"/></td>
        </tr>
    </table>
    <input type="hidden" name="submitted" value="true"/>
    <input type="submit" name="submit" value="Search" />
</form>

<%
    String submitted = request.getParameter("submitted");

    if (submitted != null) 
    {
        // Invoke the CXF web service
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(CatalogService.class);
        pf.setAddress(CatalogService.URL);
        CatalogService catalog = (CatalogService) pf.create();

        Collection <Book> books = catalog.getBooks();
        // Something in the way CXF marshalls the response converts an empty collection to null
        if (books == null)
        {
            books = new ArrayList();
        }
        %>
        Your search returned the following book(s):<br/>
        <br/>

        <table>
        <tr><th>Title</th><th>Author</th><th>Price</th><th/></tr>
        <%
        Book book;
        for (Iterator<Book> i = books.iterator(); i.hasNext();)
        {
            book = i.next();
            if (book.getTitle().contains(title) && book.getAuthor().contains(author))
            {
            %>
                <tr>
                    <td><%=book.getTitle()%></td>
                    <td><%=book.getAuthor()%></td>
                    <td>$<%=book.getPrice()%></td>
                    <td><a href="<%=request.getContextPath()%>/order.jsp?id=<%=book.getId()%>">Order this book</a></td>
                   </tr>
        <%
            }
        }
        %>
        </table>
    <%
     } 
     %>

<hr/>
<center><i>
For more information about the Bookstore example go <a target="_blank" href="http://mule.mulesource.org/display/MULE2INTRO/Bookstore+Example">here</a>.
</i></center>
</body>
</html>
