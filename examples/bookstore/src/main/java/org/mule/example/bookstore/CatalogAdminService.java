/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
