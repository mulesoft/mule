/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.transport;

import org.mule.module.cxf.support.DelegatingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class MuleUniversalDestination extends AbstractDestination
{
    public static final String RESPONSE_OBSERVER = "mule.destination.response.observer";

    private static final Logger LOGGER = Logger.getLogger(MuleUniversalDestination.class.getName());
    private MuleUniversalTransport transport;

    public MuleUniversalDestination(MuleUniversalTransport transport,
                                    EndpointReferenceType ref,
                                    EndpointInfo ei)
    {
        super(ref, ei); 
        this.transport = transport;
    }

    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage)
    {
        return new ResponseConduit(null);
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }

    @Override
    public void shutdown()
    {
        transport.remove(this);

        super.shutdown();
    }

    public class ResponseConduit extends AbstractConduit
    {

        public ResponseConduit(EndpointReferenceType arg0)
        {
            super(arg0);
        }

        public void prepare(Message message) throws IOException {
            // set an outputstream which will be used for things like attachment headers.
            // we'll stream the body later on down the line via the OutputHandler in CxfServiceComponent
            DelegatingOutputStream stream = new DelegatingOutputStream(new ByteArrayOutputStream());
            message.setContent(OutputStream.class, stream);
            message.setContent(DelegatingOutputStream.class, stream);
        }

        @Override
        public void close(Message message) throws IOException
        {
            message.getContent(OutputStream.class).close();
        }

        @Override
        protected Logger getLogger()
        {
            return LOGGER;
        }

    }
}
