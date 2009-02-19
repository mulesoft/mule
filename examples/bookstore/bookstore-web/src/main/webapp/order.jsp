<%@ page import="org.mule.example.bookstore.Book,
                  org.mule.example.bookstore.CatalogService,
                  org.mule.example.bookstore.OrderService,
                  org.apache.cxf.jaxws.JaxWsProxyFactoryBean"%>
<%@ page language="java" %>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
<title>On-line Bookstore</title>
</head>

<body link="#FFFFFF" vlink="#FFFFFF" alink="#FFFFFF" bgcolor="#000055" text="#FFFFFF">

<%
    // Get form parameters and provide defaults if blank

    long id = Long.parseLong(request.getParameter("id"));

    String field = request.getParameter("quantity");
    int quantity = field != null ? Integer.parseInt(field) : 1;

    field = request.getParameter("address");
    String address = field != null ? field : "Someplace, Somewhere";

    field = request.getParameter("email");
    String email = field != null ? field : "me@my-mail.com";

    // Invoke CXF web service
    JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
    pf.setServiceClass(CatalogService.class);
    pf.setAddress(CatalogService.URL);
    CatalogService catalog = (CatalogService) pf.create();

    // Look up book details
    Book book = catalog.getBook(id);
%>


<h2>Place an order</h2>
<form method="POST" name="submitRequest" action="">
    <table>
        <tr>
            <td>Title: </td>
            <td><%=book.getTitle()%></td>
           </tr>
        <tr>
            <td>Author: </td>
            <td><%=book.getAuthor()%></td>
        </tr>
        <tr>
            <td>Price: </td>
            <td>$<%=book.getPrice()%></td>
        </tr>
        <tr>
            <td>Quantity: </td>
            <td><input type="text" name="quantity" value="<%=quantity%>"/></td>
        </tr>
        <tr>
            <td>Shipping Address: </td>
            <td><input type="text" name="address" value="<%=address%>"/></td>
        </tr>
        <tr>
            <td>E-mail: </td>
            <td><input type="text" name="email" value="<%=email%>"/></td>
        </tr>        
    </table>
    <input type="hidden" name="submitted" value="true"/>
    <input type="submit" name="submit" value="Order" />
</form>

<%
    String submitted = request.getParameter("submitted");

    // Validate form fields
    if (submitted != null && quantity > 0 && address != null && email != null)
    {
        // Invoke CXF web service
        JaxWsProxyFactoryBean pf2 = new JaxWsProxyFactoryBean();
        pf2.setServiceClass(OrderService.class);
        pf2.setAddress(OrderService.URL);
        OrderService orderService = (OrderService) pf2.create();

        orderService.orderBook(book, quantity, address, email);
        %>
        Thank you for your order, a notification e-mail will be sent to <%=email%><br/>
        <%
     }
 %>

<br/>
<a href="/<%=request.getContextPath()%>">Return to Home Page</a>
</body>
</html>
