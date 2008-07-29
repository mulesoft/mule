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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

@WebService(serviceName="BookstoreService",
    portName="BookstorePort",
    endpointInterface="org.mule.example.bookstore.Bookstore")
public class BookstoreImpl implements Bookstore
{
    private Map < Long, Book > books = new HashMap < Long, Book > ();
    
    public long addBook(Book book)
    {
        System.out.println("Adding book " + book.getTitle());
        long id = books.size() + 1;
        book.setId(id);
        books.put(id, book);
        return id;
    }

    public Collection < Long > addBooks(Collection < Book > books)
    {
        List < Long > ids = new ArrayList < Long > ();
        if (books != null)
        {
            for (Book book : books)
            {
                ids.add(addBook(book));
            }
        }
        return ids;
    }

    public Collection < Book > getBooks()
    {
        return books.values();
    }

    public void orderBook(long bookId, String address, String email)
    {
        // In the real world we'd want this hidden behind an OrderService interface
        try
        {
            Book book = books.get(bookId);
            MuleMessage msg = new DefaultMuleMessage(new Object[] { book, address, email} );

            RequestContext.getEventContext().dispatchEvent(msg, "orderEmailService");
            System.out.println("Dispatched message to orderService.");
        }
        catch (MuleException e)
        {
            // If this was real, we'd want better error handling
            throw new RuntimeException(e);
        }
    }
}
