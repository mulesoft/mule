/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.bookstore.client;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.example.bookstore.Book;
import org.mule.example.bookstore.Bookstore;
import org.mule.example.bookstore.LocaleMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

public class BookstoreClient
{
    protected static transient Log logger = LogFactory.getLog(BookstoreClient.class);
    protected static Bookstore bookstore;
    
    protected MuleContext muleContext;
    
    public BookstoreClient(String config) throws MuleException
    {
        // create mule
        muleContext = new DefaultMuleContextFactory().createMuleContext(config);
        muleContext.start();
        
        // create client
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Bookstore.class);
        pf.setAddress("http://localhost:8777/services/bookstore");
        bookstore = (Bookstore) pf.create();
        
        // add a book to the bookstore
        Book book = new Book("J.R.R. Tolkien", "The Lord of the Rings");
        book.setId(1);
        bookstore.addBook(book);
    }

    public void close()
    {
        muleContext.dispose();
    }
    
    public static void main(String[] args) throws Exception
    {
        // This is just a simple non mule way to invoke a web service.
        // It will place an order so you can see the emailOrderService
        // in action.
        // For learning how to use CXF with an outbound router, please
        // see the mule-publisher-demo portion of the project.
        new BookstoreClient("bookstore.xml");
        int response = 0;
        
        System.out.println("\n" + LocaleMessage.getWelcomeMessage());
        
        while (response != 'q')
        {
            System.out.println("\n" + LocaleMessage.getMenuOption1());
            System.out.println("\n" + LocaleMessage.getMenuOption2());
            System.out.println("\n" + LocaleMessage.getMenuOption3());
            System.out.println("\n" + LocaleMessage.getMenuOption4());
            System.out.println("\n" + LocaleMessage.getMenuOptionQuit());
            System.out.println("\n" + LocaleMessage.getMenuPromptMessage());
            response = getSelection();
            
            if (response == '1')
            {
                Book book = createBook();
                bookstore.addBook(book);
                System.out.println("Added Book");
            }
            else if (response == '2')
            {
                Collection < Book > books = new ArrayList< Book >();
                boolean isAddAnotherBook = true;
                while(isAddAnotherBook)
                {
                    Book book = createBook();
                    books.add(book);
                    System.out.println("\n" + LocaleMessage.getAddBooksMessagePrompt());
                    int result = getSelection();
                    if (result != 'y')
                    {
                        isAddAnotherBook = false;
                        bookstore.addBooks(books);
                        System.out.println("Added book list");
                    }
                }
            }
            else if (response == '3')
            {
                Collection < Book > books = bookstore.getBooks();
                System.out.println("Request returned " + books.size() + " book/s");

                for (Iterator i = books.iterator(); i.hasNext();)
                {
                    Book book = (Book) i.next();
                    System.out.println("Title: " + book.getTitle());
                    System.out.println("Author: " + book.getAuthor());
                    System.out.println("Id: " + book.getId());
                    System.out.println();
                }
            }
            else if (response == '4')
            {   
                System.out.println("\n" + LocaleMessage.getOrderWelcomeMessage());
                System.out.println("\n" + LocaleMessage.getBookIdPrompt());
                long bookId = Long.parseLong(getInput());
                System.out.println("\n" + LocaleMessage.getHomeAddressPrompt());
                String homeAddress = getInput();
                System.out.println("\n" + LocaleMessage.getEmailAddressPrompt());
                String emailAddress = getInput();
                
                // order book
                bookstore.orderBook(bookId, 
                                  homeAddress, 
                                  emailAddress);
                
                System.out.println("Book was ordered");
            }
            else if (response == 'q')
            {
                System.out.println(LocaleMessage.getGoodbyeMessage());
                System.exit(0);
            }
            else
            {
                System.out.println(LocaleMessage.getMenuErrorMessage());
            }
        }
    }
    
    private static Book createBook() throws Exception
    {
        String title = "";
        String author = "";
        while (title.compareTo("") == 0)
        {
            System.out.println("\n" + LocaleMessage.getBookTitlePrompt());
            title = getInput();
        }
        while (author.compareTo("") == 0)
        {
            System.out.println("\n" + LocaleMessage.getAuthorNamePrompt());
            author = getInput();
        }

        Book book = new Book(title,author);
        book.setId(generateBookId());
        return book;
    }
    
    private static int getSelection() throws IOException
    {
        byte[] buf = new byte[16];
        System.in.read(buf);
        return buf[0];
    }
    
    private static String getInput() throws IOException
    {
        BufferedReader request = new BufferedReader(new InputStreamReader(System.in));
        return request.readLine();
    }
    
    private static long generateBookId()
    {
        Random randomGenerator = new Random();
        return randomGenerator.nextInt(5000);
    }
}
