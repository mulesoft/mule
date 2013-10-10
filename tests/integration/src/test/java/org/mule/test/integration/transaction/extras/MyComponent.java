/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


