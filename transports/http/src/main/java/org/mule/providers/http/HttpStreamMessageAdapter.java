/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.config.i18n.CoreMessages;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.provider.OutputHandler;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpMethod;

/**
 * A HttpStream adapter that can be used with the HttpClientMessageDispatcher who
 * knows when to release the Http Connection.
 */
public class HttpStreamMessageAdapter extends StreamMessageAdapter
{
    private static final long serialVersionUID = -7836682641618511926L;

    protected volatile HttpMethod httpMethod;

    public HttpStreamMessageAdapter(InputStream in)
    {
        super(in);
    }

    public HttpStreamMessageAdapter(InputStream in, OutputStream out)
    {
        super(in, out);
    }

    public HttpStreamMessageAdapter(OutputHandler handler)
    {
        super(handler);
    }

    public HttpStreamMessageAdapter(OutputStream out, OutputHandler handler)
    {
        super(out, handler);
    }

    public HttpStreamMessageAdapter(InputStream in, OutputStream out, OutputHandler handler)
    {
        super(in, out, handler);
    }

    public HttpMethod getHttpMethod()
    {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    public void release()
    {
        if (httpMethod == null)
        {
            throw new IllegalStateException(CoreMessages.objectIsNull("httpMethod object").toString());
        }
        else
        {
            httpMethod.releaseConnection();
        }
    }
}
