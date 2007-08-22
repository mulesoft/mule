/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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