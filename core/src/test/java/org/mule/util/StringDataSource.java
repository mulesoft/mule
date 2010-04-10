/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class StringDataSource implements DataSource
{
    protected String content;

    public StringDataSource(String payload)
    {
        super();
        content = payload;
    }

    public InputStream getInputStream() throws IOException
    {
        return new ByteArrayInputStream(content.getBytes());
    }

    public OutputStream getOutputStream()
    {
        throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
    }

    public String getContentType()
    {
        return "text/plain";
    }

    public String getName()
    {
        return "StringDataSource";
    }
}

