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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Converts an {@link javax.servlet.http.HttpServletRequest} into an array of bytes by extracting
 * the payload of the request.
 */
public class HttpRequestToByteArray extends AbstractDiscoverableTransformer
{
    public HttpRequestToByteArray()
    {
        registerSourceType(DataTypeFactory.create(HttpServletRequest.class));
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        try
        {
            IOUtils.copy(((HttpServletRequest) src).getInputStream(), baos);
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
        return baos.toByteArray();
    }
}
