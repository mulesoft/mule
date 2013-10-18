/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.api.lifecycle.CreateException;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.endpoint.CxfEndpointBuilder;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;

/**
 * This builder uses a service that is already configured to build a CXF
 * client and it's corresponding MessageProcessor. Given the specified
 * <code>address</code> property, it will lookup the corresponding 
 * inbound MessageProcessor. It will then use this processor's service model
 * to configure a CXF client. 
 * <p>
 * This can be used via CXF meta endpoints. For instance, with MuleClient you can do:
 * <code>
 * MuleClient client = ...
 * client.send("cxf:http://host/yourService?method=remoteOperation", message);
 * </code>
 * This will find the remote service, configure the client appropriately, and 
 * invoke the remote service. 
 * <p>
 * This only works if the server and client are in the same Mule instance.
 * 
 * @see CxfEndpointBuilder
 */
public class LocalClientMessageProcessorBuilder extends AbstractOutboundMessageProcessorBuilder
{
    @Override
    protected void configureMessageProcessor(CxfOutboundMessageProcessor processor)
    {
    }

    @Override
    protected Client createClient() throws CreateException, Exception
    {
        String uri = getAddress();
        int idx = uri.indexOf('?');
        if (idx != -1)
        {
            uri = uri.substring(0, idx);
        }
        
        // remove username/password
        idx = uri.indexOf('@');
        int slashIdx = uri.indexOf("//");
        if (idx != -1 && slashIdx != -1)
        {
            uri = uri.substring(0, slashIdx + 2) + uri.substring(idx + 1);
        }
        
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(uri);

        DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm.getDestinationFactoryForUri(uri);
        if (df == null)
        {
            throw new Exception("Could not find a destination factory for uri " + uri);
        }

        Destination dest = df.getDestination(ei);
        MessageObserver mo = dest.getMessageObserver();
        if (mo instanceof ChainInitiationObserver)
        {
            ChainInitiationObserver cMo = (ChainInitiationObserver) mo;
            Endpoint cxfEP = cMo.getEndpoint();

            return new ClientImpl(getBus(), cxfEP);
        }
        else
        {
            throw new Exception("Could not create client! No Server was found directly on the endpoint: "
                                + uri);
        }
    }
    
}
