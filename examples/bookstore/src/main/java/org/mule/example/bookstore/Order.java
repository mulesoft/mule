/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore;

/**
 * Simple class which represents a Book Order.
 */
public class Order 
{
    private Book book;
    private int quantity;
    /** Shipping address */
    private String address;
    /** E-mail address used to receive order notifications */
    private String email;
    
    public Order()
    {
        // empty constructor
    }
    
    public Order(Book book, int quantity, String address, String email)
    {
        this.book = book;
        this.quantity = quantity;
        this.address = address;
        this.email = email;
    }
    
    public Book getBook()
    {
        return book;
    }

    public void setBook(Book book)
    {
        this.book = book;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public void setQuantity(int quantity)
    {
        this.quantity = quantity;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
