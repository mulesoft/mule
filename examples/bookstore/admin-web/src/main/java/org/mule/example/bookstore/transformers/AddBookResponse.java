/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.bookstore.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.example.bookstore.web.HtmlTemplate;
import org.mule.transformer.AbstractTransformer;

/**
 * A call to addBook() returns a Long representing the number of 
 * books in the catalog.  This transformer wraps the Long into 
 * a nice HTML page.  
 */
public class AddBookResponse extends AbstractTransformer
{
    public AddBookResponse()
    {
        super();
        registerSourceType(Long.class);
        setReturnClass(String.class);
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        Long numBooks = (Long) src;
        String response = "Catalog now contains: " + numBooks + " book(s)";
        return HtmlTemplate.wrapHtmlBody(response);
    }
}


