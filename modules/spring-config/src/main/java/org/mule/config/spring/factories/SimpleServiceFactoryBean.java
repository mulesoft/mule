/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.SimpleService;

public class SimpleServiceFactoryBean extends AbstractFlowConstructFactoryBean
{
    private String address;

    private Component component;

    public Class<?> getObjectType()
    {
        return SimpleService.class;
    }

    @Override
    protected AbstractFlowConstruct createFlowConstruct() throws Exception
    {
        return new SimpleService(muleContext, name, getInboundEndpoint(), component);
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public void setComponent(Component component)
    {
        this.component = component;
    }

    private InboundEndpoint getInboundEndpoint() throws MuleException
    {
        return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(address);
    }
}
