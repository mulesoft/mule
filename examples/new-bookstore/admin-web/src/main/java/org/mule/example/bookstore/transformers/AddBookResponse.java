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
import org.mule.transformer.AbstractTransformer;

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

        return "Current inventory: " + numBooks + " book(s)<br/><a href=\"/bookstore-admin\">Add more books</a>";
    }
}


