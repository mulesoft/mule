/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.http.servlet;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.DiscoverableTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Converts an {@link javax.servlet.http.HttpServletRequest} into an array of bytes by extracting the payload of
 * the request.
 */
public class HttpRequestToByteArray extends AbstractTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public HttpRequestToByteArray()
    {
        registerSourceType(HttpServletRequest.class);
        setReturnClass(byte[].class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
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

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
