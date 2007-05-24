/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.streaming;

import org.mule.config.i18n.CoreMessages;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This outputStream allows a stream to be choosen after this has been created.  It is used by
 * Mule when using streaming to pass to the Streaming component. The actual output stream is
 * choosen at the point where the first byte is written. At this point an outbound router can be
 * choosen and vales set on the event context can affect which stream is choosen.
 */
public class DeferredOutputStream extends OutputStream
{
    
    protected final Log logger = LogFactory.getLog(DeferredOutputStream.class);
    private UMOEventContext event;
    private OutputStream out = null;
    private int buffer = 0;


    public DeferredOutputStream(UMOEventContext event)
    {
        this.event = event;
    }


    public DeferredOutputStream(UMOEventContext event, int buffer)
    {
        this.event = event;
        this.buffer = buffer;
    }

    public void write(int b) throws IOException
    {
        if (out == null)
        {
            out = getOutputStream();
        }
        out.write(b);
    }


    public void flush() throws IOException
    {
        //out could be null if the stream hasn't been written to yet
        if (out != null)
        {
            logger.debug("flushing deferred output stream");
            out.flush();
        }
        else
        {
            logger.debug("deferred output stream unflushed");
        }
    }

    public void close() throws IOException
    {
        //out could be null if the stream hasn't been written to yet
        if (out != null)
        {
            out.close();
        }
    }

    protected OutputStream getOutputStream() throws IOException
    {
        StreamMessageAdapter adapter = (StreamMessageAdapter) event.getMessage().getAdapter();
        OutputStream temp = getOutputStreamFromRouter();
        if (temp == null)
        {
            temp = adapter.getOutputStream();
        }
        if (temp == null)
        {
            throw new IOException("No output stream was found for the current event: " + event);
        }
        else if (getBuffer() > 0)
        {
            return new BufferedOutputStream(temp, getBuffer());
        }
        else
        {
            return temp;
        }
    }


    public int getBuffer()
    {
        return buffer;
    }

    public void setBuffer(int buffer)
    {
        if (out != null)
        {
            throw new IllegalStateException("The stream buffer cannot be set after the stream has been written to");
        }
        this.buffer = buffer;
    }

    protected OutputStream getOutputStreamFromRouter() throws IOException
    {
        UMODescriptor descriptor = event.getComponentDescriptor();
        UMOEndpointURI endpoint = event.getEndpointURI();

        UMOOutboundRouterCollection messageRouter = descriptor.getOutboundRouter();
        if (messageRouter.hasEndpoints())
        {
            for (Iterator iterator = messageRouter.getRouters().iterator(); iterator.hasNext();)
            {
                UMOOutboundRouter router = (UMOOutboundRouter) iterator.next();
                boolean match = false;
                try
                {
                    match = router.isMatch(event.getMessage());
                }
                catch (MessagingException e)
                {
                    throw (IOException) new IOException(e.toString()).initCause(e);
                }
                if (match)
                {
                    if (router.getEndpoints().size() != 1)
                    {
                        throw new IOException(
                            CoreMessages.streamingComponentMustHaveOneEndpoint(descriptor.getName()).toString());
                    }
                    else
                    {
                        UMOEndpoint ep = (UMOEndpoint) router.getEndpoints().get(0);
                        try
                        {
                            return ep.getConnector().getOutputStream(ep, event.getMessage());
                        }
                        catch (UMOException e)
                        {
                            throw (IOException) new IOException(
                                CoreMessages.streamingFailedForEndpoint(endpoint.toString()).toString()).initCause(e);
                        }
                    }
                }
            }
            //If we got to here there are no matching outbound Routers
            throw new IOException(
                CoreMessages.streamingComponentMustHaveOneEndpoint(descriptor.getName()).toString());
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("there are no outbound endpoints configured on this component, the otput stream provided from the message adapter will be used");
        }

        //Use the response output stream on the StreamingMessage adapter
        return null;
    }
}
