/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore;

import java.util.Collection;

/** 
 * Administration interface for adding new books to the bookstore's catalog 
 */
public interface CatalogAdminService
{
    /** Add a new book to the catalog */
    long addBook(Book book);

    /** Add a group of new books to the catalog */
    Collection <Long> addBooks(Collection <Book> books);
}
