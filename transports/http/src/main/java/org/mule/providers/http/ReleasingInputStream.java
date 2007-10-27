/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.impl.model.streaming.DelegatingInputStream;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;

public class ReleasingInputStream extends DelegatingInputStream
{
    private final HttpMethod method;

    public ReleasingInputStream(InputStream is, HttpMethod method)
    {
        super(is);
        
        this.method = method;
    }

    public void close() throws IOException 
    {
        super.close();
        
        if (method != null)
        {
            method.releaseConnection();
        }
    }
}

