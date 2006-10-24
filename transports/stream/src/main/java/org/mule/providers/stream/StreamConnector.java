/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.stream;

import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>StreamConnector</code> can send and receive mule events over IO streams.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class StreamConnector extends AbstractServiceEnabledConnector
{

    public static final String STREAM_SYSTEM_IN = "system.in";
    public static final String STREAM_SYSTEM_OUT = "system.out";
    public static final String STREAM_SYSTEM_ERR = "system.err";

    protected OutputStream outputStream;
    protected InputStream inputStream;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#registerListener(org.mule.umo.UMOSession,
     *      org.mule.umo.endpoint.UMOEndpoint)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        return serviceDescriptor.createMessageReceiver(this, component, endpoint,
            new Object[]{new Long(1000)});
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doStop()
     */
    public void doStop()
    {
        // template method
    }

    protected void doDispose()
    {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doStart()
     */
    public void doStart()
    {
        // template method
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#getProtocol()
     */

    public String getProtocol()
    {
        return "stream";
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

}
