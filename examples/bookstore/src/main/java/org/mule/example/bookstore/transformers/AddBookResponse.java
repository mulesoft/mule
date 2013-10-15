/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.bookstore.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.example.bookstore.web.HtmlTemplate;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

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
        registerSourceType(DataTypeFactory.create(Long.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        Long numBooks = (Long) src;
        String response = "Catalog now contains: " + numBooks + " book(s)";
        return HtmlTemplate.wrapHtmlBody(response);
    }
}
