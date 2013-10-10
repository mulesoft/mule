/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore;

import javax.jws.WebService;

/**
 * Service for placing a book order.
 * @see OrderService
 */
@WebService(serviceName="OrderService", endpointInterface="org.mule.example.bookstore.OrderService")
public class OrderServiceImpl implements OrderService
{
    public Order orderBook(Book book, int quantity, String address, String email)
    {
        System.out.println("Order has been placed for book: " + book.getTitle());
        return new Order(book, quantity, address, email);
    }
}
