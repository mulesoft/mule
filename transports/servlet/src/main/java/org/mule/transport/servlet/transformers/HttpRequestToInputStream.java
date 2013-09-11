/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


