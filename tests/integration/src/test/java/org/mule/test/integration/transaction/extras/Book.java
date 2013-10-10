/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction.extras;

import java.io.Serializable;

public class Book implements Serializable
{
    private static final long serialVersionUID = -2519185601486498262L;

    int serialNo;
    String title;
    String author;

    public Book()
    {
        // empty constructor
    }

    public Book(int serialNo, String title, String author)
    {
        super();
        this.serialNo = serialNo;
        this.title = title;
        this.author = author;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public int getSerialNo()
    {
        return serialNo;
    }

    public void setSerialNo(int serialNo)
    {
        this.serialNo = serialNo;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
