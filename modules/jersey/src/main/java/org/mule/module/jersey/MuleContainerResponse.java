/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.spi.container.WebApplication;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Custom implementation of ContainerResponse that doesn't write the whole response to the stream in the write() method.
 * The Jersey application processes the request, sets all the data required to build the response in this object, and
 * calls the write method. This class overrides the write method by only setting the headers and status code in the
 * MuleMessage. Nothing is written to the output stream until the writeToStream method is called.
 */
public class MuleContainerResponse extends ContainerResponse
{

    public MuleContainerResponse(WebApplication wa, ContainerRequest request, ContainerResponseWriter responseWriter)
    {
        super(wa, request, responseWriter);
    }

    @Override
    public void write() throws IOException
    {
        getContainerResponseWriter().writeStatusAndHeaders(-1, this);
        return;
    }

    public void writeToStream(OutputStream out) throws IOException
    {
        MuleResponseWriter muleResponseWriter = (MuleResponseWriter) getContainerResponseWriter();
        muleResponseWriter.setOutputStream(out);
        super.write();
    }
}
