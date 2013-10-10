/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jca;

import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.util.StringUtils;

import java.io.Serializable;
import java.util.Properties;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

/**
 * <code>MuleActivationSpec</code> defines the contract between a Message Driven
 * Bean (MDB) and the Mule Resource Adapter. This spec holds the configuration values
 * used to register inbound communication with the Resource Adapter
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
    private EndpointURI endpointURI;
    private String modelName;

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
        Properties props = new Properties();

        for (int i = 0; i < pairs.length; i++)
        {
            String pair = pairs[i];
            int x = pair.indexOf('=');
            if (x == -1)
            {
                props.setProperty(pair, null);
            }
            else
            {
                props.setProperty(pair.substring(0, x), pair.substring(x + 1));
            }
        }

        this.setPropertiesMap(props);
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
            this.endpointURI = new MuleEndpointURI(endpoint, resourceAdapter.muleContext);
        }
        catch (EndpointException e)
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
