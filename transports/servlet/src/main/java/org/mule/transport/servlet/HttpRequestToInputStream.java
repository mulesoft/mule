/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractDiscoverableTransformer;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
/**
 * Converts an {@link javax.servlet.http.HttpServletRequest} into an {@link InputStream}.
 */
public class HttpRequestToInputStream extends AbstractDiscoverableTransformer
{

    public HttpRequestToInputStream()
    {
        super();
        setReturnClass(InputStream.class);
        registerSourceType(HttpServletRequest.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
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


