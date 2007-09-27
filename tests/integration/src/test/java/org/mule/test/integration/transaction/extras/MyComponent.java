/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction.extras;

public class MyComponent
{
    private LibraryDao library;

    public boolean doInsertTitle(Book book) throws Exception
    {
        return library.insertBook(book);
    }

    public LibraryDao getLibrary()
    {
        return library;
    }

    public void setLibrary(LibraryDao library)
    {
        this.library = library;
    }
}


