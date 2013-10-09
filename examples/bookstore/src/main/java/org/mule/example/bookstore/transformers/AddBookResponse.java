/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
