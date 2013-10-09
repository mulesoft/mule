/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;

/**
 * Spring FactoryBean used to create concrete instances of inbound endpoints
 */
public class PollInboundEndpointFactoryBean extends AbstractEndpointFactoryBean
{

    public PollInboundEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public PollInboundEndpointFactoryBean()
    {
        super();
    }

    public Class<?> getObjectType()
    {
        return InboundEndpoint.class;
    }

    @Override
    public Object doGetObject() throws Exception
    {
        EndpointFactory ef = muleContext.getEndpointFactory();
        if (ef != null)
        {
            return ef.getInboundEndpoint(this);
        }
        else
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("EndpointFactory not found in Registry"));
        }
    }

}
