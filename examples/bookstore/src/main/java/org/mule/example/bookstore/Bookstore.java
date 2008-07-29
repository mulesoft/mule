/*
 * $$Id$$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.bookstore;

import java.util.Collection;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public interface Bookstore
{
long addBook(@WebParam(name="book") Book book);
    
    @WebResult(name="bookIds")
    Collection<Long> addBooks(@WebParam(name="books") Collection<Book> books);
    
    @WebResult(name="books") 
    Collection<Book> getBooks();
    
    void orderBook(@WebParam(name="bookId") long bookId, 
                   @WebParam(name="address") String address,
                   @WebParam(name="email") String email);
}


