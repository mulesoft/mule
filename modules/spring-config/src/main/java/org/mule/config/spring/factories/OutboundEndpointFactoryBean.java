/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.ServiceType;
import org.mule.endpoint.AbstractEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.transport.service.TransportServiceDescriptor;

/**
 * Spring FactoryBean used to create concrete instances of outbound endpoints
 */
public class OutboundEndpointFactoryBean extends AbstractEndpointFactoryBean
{

    public OutboundEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public OutboundEndpointFactoryBean()
    {
        super();
    }
    
    public Class getObjectType()
    {
        return OutboundEndpoint.class;
    }

    public Object doGetObject() throws Exception
    {
        // If this is a meta endpoint, then we can wrap it using the meta endpoint builder from the TransportServiceDescriptor
        String scheme = getEndpointBuilder().getEndpoint().getFullScheme();
        TransportServiceDescriptor tsd = (TransportServiceDescriptor) muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, scheme, null);
        EndpointBuilder endpointBuilder = tsd.createEndpointBuilder(this);

        OutboundEndpoint outboundEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
        if (outboundEndpoint instanceof AbstractEndpoint)
        {
            AbstractEndpoint.class.cast(outboundEndpoint).setAnnotations(getAnnotations());
        }
        return outboundEndpoint;
    }

    @Override
    public void setRedeliveryPolicy(AbstractRedeliveryPolicy redeliveryPolicy)
    {
        throw new IllegalStateException("A redelivery policy cannot be specified for an outbound endpoint.");
    }
}
