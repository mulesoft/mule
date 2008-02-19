/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.endpoint.EndpointURIEndpointBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Endpoint factory bean which uses type attribute to determine endpoint type (Used by 1to2migration module)
 */
public class EndpointFactoryBean extends EndpointURIEndpointBuilder
    implements FactoryBean, MuleContextAware, Initialisable
{

    // as in ImmutableMuleEndpoint (but those not public)
    public static final String ENDPOINT_TYPE_SENDER = "sender";
    public static final String ENDPOINT_TYPE_RECEIVER = "receiver";

    public static final String ALWAYS_CREATE_STRING = "ALWAYS_CREATE";
    public static final String NEVER_CREATE_STRING = "NEVER_CREATE";

    protected final Log logger = LogFactory.getLog(getClass());

    private String type;

    public EndpointFactoryBean()
    {
        super();
    }

    public Object getObject() throws Exception
    {
        if (ENDPOINT_TYPE_RECEIVER.equals(type))
        {
            logger.debug("Endpont type is \"receiver\", building inbound endpoint");
            return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(this);
        }
        else if (ENDPOINT_TYPE_SENDER.equals(type))
        {
            logger.debug("Endpont type is \"sender\", building inbound endpoint");
            return muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(this);
        }
        else
        {
            throw new IllegalArgumentException("The endpoint type: " + type + "is not recognized.");
        }
    }

    public Class getObjectType()
    {
        // TODO MULE-2292 Use role-specific interface
        return ImmutableEndpoint.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleTransitionResult.OK;
    }

    public void setType(String type)
    {
        this.type = type;
    }

}
