/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi.components;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.Map;

import javax.jbi.JBIException;

/**
 * A Jbi component that has a Mule muleEndpoint component configured on it. Both the
 * Dispatcher and Receiver components extend this component.
 *
 */
public abstract class AbstractEndpointComponent extends AbstractJbiComponent
{

    protected UMOEndpoint muleEndpoint;

    protected String endpoint;

    protected Map endpointProperties;

    protected AbstractEndpointComponent()
    {
        
    }

    public UMOEndpoint getMuleEndpoint()
    {
        return muleEndpoint;
    }

    public void setMuleEndpoint(UMOEndpoint muleEndpoint)
    {
        this.muleEndpoint = muleEndpoint;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public Map getEndpointProperties()
    {
        return endpointProperties;
    }

    public void setEndpointProperties(Map endpointProperties)
    {
        this.endpointProperties = endpointProperties;
    }

    protected void doInit() throws JBIException
    {
        try
        {
            if (muleEndpoint == null)
            {
                if (endpoint == null)
                {
                    throw new IllegalArgumentException("A Mule muleEndpoint must be set on this component");
                }
                else
                {
                    muleEndpoint = new MuleEndpoint(endpoint, true);
                }
            }

            if (endpointProperties != null)
            {
                muleEndpoint.getProperties().putAll(endpointProperties);
            }
            //TODO Fix muleEndpoint.initialise(managementContext);

        }
        catch (Exception e)
        {
            throw new JBIException(e);
        }
    }
}
