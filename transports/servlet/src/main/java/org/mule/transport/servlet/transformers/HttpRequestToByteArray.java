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
