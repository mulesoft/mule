/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.util.StringUtils;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import java.io.Serializable;
import java.util.Properties;

/**
 * <code>MuleActivationSpec</code> defines the contract between a Message Driven
 * Bean (MDB) and the Mule Resource Adapter. This spec holds the configuration values
 * used to register inbound communication with the Resource Adapter
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleActivationSpec implements ActivationSpec, Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 735353511874563914L;

    private Properties propertiesMap;
    private String endpointName;
    private String connectorName;
    private int createConnector;
    private MuleResourceAdapter resourceAdapter;
    private String endpoint;
    private UMOEndpointURI endpointURI;

    public Properties getPropertiesMap()
    {
        return propertiesMap;
    }

    public void setPropertiesMap(Properties propertiesMap)
    {
        this.propertiesMap = propertiesMap;
    }

    public void setPropertiesMap(String properties)
    {
        String[] pairs = StringUtils.splitAndTrim(properties, ",");
        propertiesMap = new Properties();
        for (int i = 0; i < pairs.length; i++)
        {
            String pair = pairs[i];
            int x = pair.indexOf("=");
            if (x == -1)
            {
                propertiesMap.setProperty(pair, null);
            }
            else
            {
                propertiesMap.setProperty(pair.substring(0, x), pair.substring(x + 1));
            }
        }
    }

    public String getEndpointName()
    {
        return endpointName;
    }

    public void setEndpointName(String endpointName)
    {
        this.endpointName = endpointName;
    }

    public String getConnectorName()
    {
        return connectorName;
    }

    public void setConnectorName(String connectorName)
    {
        this.connectorName = connectorName;
    }

    public int getCreateConnector()
    {
        return createConnector;
    }

    public void setCreateConnector(int createConnector)
    {
        this.createConnector = createConnector;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;

    }

    public void validate() throws InvalidPropertyException
    {
        try
        {
            this.endpointURI = new MuleEndpointURI(endpoint);
        }
        catch (MalformedEndpointException e)
        {
            throw new InvalidPropertyException(e);
        }

        if (propertiesMap != null)
        {
            propertiesMap.putAll(this.endpointURI.getParams());
        }
        else
        {
            propertiesMap = this.endpointURI.getParams();
        }
        if (endpoint == null)
        {
            throw new InvalidPropertyException("endpoint is null");
        }

        if (endpointURI == null)
        {
            throw new InvalidPropertyException("endpointURI is null");
        }
    }

    public ResourceAdapter getResourceAdapter()
    {
        return resourceAdapter;
    }

    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException
    {
        // spec section 5.3.3
        if (this.resourceAdapter != null)
        {
            throw new ResourceException("ResourceAdapter already set");
        }
        if (!(resourceAdapter instanceof MuleResourceAdapter))
        {
            throw new ResourceException("ResourceAdapter is not of type: "
                                        + MuleResourceAdapter.class.getName());
        }
        this.resourceAdapter = (MuleResourceAdapter)resourceAdapter;
    }
}
