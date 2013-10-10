/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.bookstore;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;

/**
 * Bookstore catalog service which implements both the public interface for 
 * browsing the catalog and the admin interface for adding books to the catalog.
 * 
 * @see CatalogService
 * @see CatalogAdminService
 */
@WebService(serviceName="CatalogService", endpointInterface="org.mule.example.bookstore.CatalogService")
public class CatalogServiceImpl implements CatalogService, CatalogAdminService, Initialisable
{
    /** Simple hashmap used to store the catalog, in real life this would be a database */
    private Map <Long, Book> books = new HashMap <Long, Book> ();
    
    public void initialise() throws InitialisationException
    {
        books = new HashMap <Long, Book> ();

        // Add some initial test data
        addBook(new Book("J.R.R. Tolkien", "The Fellowship of the Ring", 8));
        addBook(new Book("J.R.R. Tolkien", "The Two Towers", 10));
        addBook(new Book("J.R.R. Tolkien", "The Return of the King", 10));
        addBook(new Book("C.S. Lewis", "The Lion, the Witch and the Wardrobe", 6));
        addBook(new Book("C.S. Lewis", "Prince Caspian", 8));
        addBook(new Book("C.S. Lewis", "The Voyage of the Dawn Treader", 6));
        addBook(new Book("Leo Tolstoy", "War and Peace", 8));
        addBook(new Book("Leo Tolstoy", "Anna Karenina", 6));
        addBook(new Book("Henry David Thoreau", "Walden", 8));
        addBook(new Book("Harriet Beecher Stowe", "Uncle Tom's Cabin", 6));
        addBook(new Book("George Orwell", "1984", 8));
        addBook(new Book("George Orwell", "Animal Farm", 8));
        addBook(new Book("Aldous Huxley", "Brave New World", 8));

    }

    public long addBook(Book book)
    {
        System.out.println("Adding book " + book.getTitle());
        long id = books.size() + 1;
        book.setId(id);
        books.put(id, book);
        return id;
    }

    public Collection <Long> addBooks(Collection<Book> booksToAdd)
    {
        List <Long> ids = new ArrayList <Long> ();
        if (booksToAdd != null)
        {
            for (Book book : booksToAdd)
            {
                ids.add(addBook(book));
            }
        }
        return ids;
    }

    public Collection <Book> getBooks()
    {
        return books.values();
    }

    public Book getBook(long bookId)
    {
        return books.get(bookId);
    }
}
