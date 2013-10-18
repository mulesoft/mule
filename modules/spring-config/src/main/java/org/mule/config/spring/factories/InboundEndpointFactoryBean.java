/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.AbstractEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;

/**
 * Spring FactoryBean used to create concrete instances of inbound endpoints
 */
public class InboundEndpointFactoryBean extends AbstractEndpointFactoryBean
{

    public InboundEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public InboundEndpointFactoryBean()
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
            InboundEndpoint inboundEndpoint = ef.getInboundEndpoint(this);
            if (inboundEndpoint instanceof AbstractEndpoint)
            {
                AbstractEndpoint.class.cast(inboundEndpoint).setAnnotations(getAnnotations());
            }
            return inboundEndpoint;
        }
        else
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("EndpointFactory not found in Registry"));
        }
    }

}
