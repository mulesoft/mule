/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/** 
 * Interface for placing a book order 
 */
@WebService
public interface OrderService
{
    /** The order service will be accesible as a web service at this URL */
    static final String URL = "http://0.0.0.0:8777/services/order";

    /** Place a book order */
    @WebResult(name="order") 
    Order orderBook(@WebParam(name="book") Book book,
                    @WebParam(name="quantity") int quantity, 
                    @WebParam(name="address") String address,
                    @WebParam(name="email") String email);
}
