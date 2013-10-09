/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
