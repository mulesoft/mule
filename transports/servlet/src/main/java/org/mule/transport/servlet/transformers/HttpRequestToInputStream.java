/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractDiscoverableTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

/**
 * Converts a {@link HttpServletRequest} into an {@link InputStream}.
 */
public class HttpRequestToInputStream extends AbstractDiscoverableTransformer
{
    public HttpRequestToInputStream()
    {
        super();
        registerSourceType(DataTypeFactory.create(HttpServletRequest.class));
        setReturnDataType(DataTypeFactory.INPUT_STREAM);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            return ((HttpServletRequest) src).getInputStream();
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
    }
}


