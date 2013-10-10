/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.model.streaming.DelegatingInputStream;

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

