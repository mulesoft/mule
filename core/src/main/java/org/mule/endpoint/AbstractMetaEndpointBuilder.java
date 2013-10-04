/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnector;
import org.mule.util.BeanUtils;

import java.util.Map;

/**
 * A base class used for Meta endpoint builders such as RSS or ATOM.  This class overrides the {@link #setProperties(java.util.Map)}
 * method
 */
public abstract class AbstractMetaEndpointBuilder extends EndpointURIEndpointBuilder
{
    protected AbstractMetaEndpointBuilder()
    {
    }

    protected AbstractMetaEndpointBuilder(EndpointURIEndpointBuilder global)
            throws EndpointException
    {
        super(global);
    }

    protected AbstractMetaEndpointBuilder(URIBuilder builder)
    {
        super(builder);
    }

    protected AbstractMetaEndpointBuilder(String address, MuleContext muleContext)
    {
        super(address, muleContext);
    }

    protected AbstractMetaEndpointBuilder(EndpointURI endpointURI)
    {
        super(endpointURI);
    }

    protected AbstractMetaEndpointBuilder(ImmutableEndpoint source)
    {
        super(source);
    }

    @Override
    public void setProperties(Map<Object, Object> properties)
    {
        //This is required since properties were historically set as a properties map
        for (Map.Entry<Object, Object> entry : properties.entrySet())
        {
            try
            {
                BeanUtils.setProperty(this, entry.getKey().toString(), entry.getValue());
            }
            catch (Exception e)
            {
                //ignore
            }
        }
        properties.remove("connector");
        super.setProperties(properties);
    }

    @Override
    protected String getScheme()
    {
        return uriBuilder.getEndpoint().getScheme();
    }

    public static String getEndpointAddressWithoutMetaScheme(String string)
    {
        int idx = string.indexOf(':');
        if (idx != -1)
        {
            string = string.substring(idx+1);
        }
        return string;
    }

    @Override
    protected Connector getConnector() throws EndpointException
    {
        AbstractConnector c = (AbstractConnector) super.getConnector();
        EndpointURI endpointURI = uriBuilder.getEndpoint();
        if(!c.supportsProtocol(endpointURI.getFullScheme()))
        {
            c.registerSupportedMetaProtocol(endpointURI.getSchemeMetaInfo());
        }
        return c;
    }
}
