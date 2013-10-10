/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore;

import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BookstoreTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "bookstore-config.xml";
    }
    
    @Test
    public void testGetBooks()
    {
        // Catalog web service
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(CatalogService.class);
        pf.setAddress(CatalogService.URL);
        CatalogService catalog = (CatalogService) pf.create();
        assertNotNull(catalog);

        Collection <Book> books = catalog.getBooks();
        assertNotNull(books);
        // Number of books added as test data in CatalogServiceImpl.initialise()
        assertEquals(13, books.size());
    }

    @Test
    public void testOrderBook()
    {
        // Catalog web service
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(CatalogService.class);
        pf.setAddress(CatalogService.URL);
        CatalogService catalog = (CatalogService) pf.create();
        assertNotNull(catalog);

        // Order web service
        JaxWsProxyFactoryBean pf2 = new JaxWsProxyFactoryBean();
        pf2.setServiceClass(OrderService.class);
        pf2.setAddress(OrderService.URL);
        OrderService orderService = (OrderService) pf2.create();     
        assertNotNull(orderService);

        // Place an order for book #3 from the catalog
        Book book = catalog.getBook(3); 
        assertNotNull(book);
        Order order = orderService.orderBook(book, 2, "Somewhere", "me@my-mail.com"); 
        assertNotNull(order);
        assertEquals(3, order.getBook().getId());
        assertEquals(2, order.getQuantity());
        assertEquals("me@my-mail.com", order.getEmail());
    }

//    @Test
//    public void testAddBook() throws Exception
//    {
//        HttpServletRequest request = new Request();
//        request.setAttribute("title", "blah");
//        request.setAttribute("author", "blah");
//        
//        MuleClient client = new MuleClient(muleContext);
//        client.send("servlet://catalog", request, null);
//    }
}
