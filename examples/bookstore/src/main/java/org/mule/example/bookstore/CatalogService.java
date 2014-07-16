/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.bookstore;

import java.util.Collection;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/** 
 * Interface for working with the bookstore's catalog of books 
 */
@WebService
public interface CatalogService
{
    /** The catalog will be accesible as a web service at this URL */
    static final String URL = "http://0.0.0.0:8777/services/catalog";

    /** Return a collection of all books in the catalog */
    @WebResult(name="books") 
    Collection<Book> getBooks();

    /** Look up the details for a particular book by ID */
    @WebResult(name="book") 
    Book getBook(@WebParam(name="bookId") long bookId);
}
