/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.endpoint;

import java.util.ArrayList;
import java.util.Arrays;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMetaEndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.cxf.builder.WsdlClientMessageProcessorBuilder;
import org.mule.module.cxf.config.FlowConfiguringMessageProcessor;

public class WsdlCxfEndpointBuilder extends AbstractMetaEndpointBuilder
{

    private final String wsdlAddress;

    public WsdlCxfEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);

        this.wsdlAddress = getEndpointAddressWithoutMetaScheme(global.getEndpointBuilder().toString());
        this.uriBuilder = new EndpointURIEndpointBuilder(wsdlAddress, muleContext).getEndpointBuilder();
    }

    public WsdlCxfEndpointBuilder(String address, MuleContext muleContext)
    {
        super(getAddressWithoutQuery(getEndpointAddressWithoutMetaScheme(address)), muleContext);
        this.wsdlAddress = getEndpointAddressWithoutMetaScheme(address);
    }

    @Override
    public InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        throw new UnsupportedOperationException("Inbound meta CXF endpoints not supported");
    }

    @Override
    public OutboundEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        final WsdlClientMessageProcessorBuilder builder = new WsdlClientMessageProcessorBuilder();
        builder.setMuleContext(muleContext);
        builder.setWsdlLocation(getEndpointBuilder().toString() + "?wsdl");
        builder.setOperation(getOperation());

        try
        {
            // List must be mutable as it gets cleared on Mule shutdown
            messageProcessors = new ArrayList<MessageProcessor>(
                Arrays.asList(new FlowConfiguringMessageProcessor(builder)));
        }
        catch (final Exception e)
        {
            throw new EndpointException(e);
        }

        return super.buildOutboundEndpoint();
    }

    private String getOperation()
    {
        String query = wsdlAddress;
        final int idx = wsdlAddress.lastIndexOf('?');
        if (idx != -1)
        {
            query = wsdlAddress.substring(idx + 1);
        }
        else
        {
            return null;
        }

        final String[] params = query.split("&");
        for (final String p : params)
        {
            if (p.startsWith("method="))
            {
                return p.substring(7);
            }
        }
        return null;
    }

    private static String getAddressWithoutQuery(String string)
    {
        final int idx = string.indexOf('?');
        if (idx != -1)
        {
            string = string.substring(0, idx);
        }
        return string;
    }

}
