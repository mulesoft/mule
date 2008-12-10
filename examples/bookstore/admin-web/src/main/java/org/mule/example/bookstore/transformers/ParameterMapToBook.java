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
import org.mule.config.i18n.MessageFactory;
import org.mule.example.bookstore.Book;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.StringUtils;

import java.util.Map;

/**
 * Transforms a Map of HttpRequest parameters into a Book object.  
 * The request parameters are always strings (they come from the HTML form), 
 * so we need to parse and convert them to their appropriate types.
 */
public class ParameterMapToBook extends AbstractTransformer
{
    public ParameterMapToBook()
    {
        super();
        registerSourceType(Map.class);
        setReturnClass(Book.class);
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        Map <String, String> parameters = (Map <String, String>) src;
        
        String author = parameters.get("author");
        String title = parameters.get("title");
        String price = parameters.get("price");

        if (StringUtils.isBlank(author))
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Missing author field"));
        }
        if (StringUtils.isBlank(title))
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Missing title field"));
        }
        if (StringUtils.isBlank(price))
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Missing price field"));
        }

        return new Book(author, title, Double.parseDouble(price));
    }
}