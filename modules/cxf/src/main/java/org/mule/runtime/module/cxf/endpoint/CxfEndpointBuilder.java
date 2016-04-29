/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.endpoint;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.endpoint.AbstractMetaEndpointBuilder;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.endpoint.URIBuilder;
import org.mule.runtime.module.cxf.builder.LocalClientMessageProcessorBuilder;

import java.util.ArrayList;

public class CxfEndpointBuilder extends AbstractMetaEndpointBuilder
{

    public CxfEndpointBuilder()
    {
        super();
    }

    public CxfEndpointBuilder(EndpointURI endpointURI)
    {
        super(endpointURI);
    }

    public CxfEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public CxfEndpointBuilder(ImmutableEndpoint source)
    {
        super(source);
    }

    public CxfEndpointBuilder(String address, MuleContext muleContext)
    {
        super(getEndpointAddressWithoutMetaScheme(address), muleContext);
    }

    public CxfEndpointBuilder(URIBuilder builder)
    {
        super(builder);
    }

    @Override
    public InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        throw new UnsupportedOperationException("Inbound meta CXF endpoints not supported");
    }

    @Override
    public OutboundEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        LocalClientMessageProcessorBuilder builder = new LocalClientMessageProcessorBuilder();
        builder.setMuleContext(muleContext);
        builder.setAddress(getEndpointBuilder().getEndpoint().toString());
        
        try
        {
            ArrayList<MessageProcessor> processors = new ArrayList<MessageProcessor>();
            processors.add(builder.build());
            if (messageProcessors != null) 
            {
                processors.addAll(messageProcessors);
            }
            messageProcessors = processors;
        }
        catch (Exception e)
        {
            throw new EndpointException(e);
        }

        return super.buildOutboundEndpoint();
    }

}
