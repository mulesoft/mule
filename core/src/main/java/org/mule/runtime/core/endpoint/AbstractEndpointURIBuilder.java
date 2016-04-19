/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.EndpointURIBuilder;
import org.mule.runtime.core.api.endpoint.MalformedEndpointException;
import org.mule.runtime.core.util.PropertiesUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Properties;

/**
 * {@link UrlEndpointURIBuilder} is the default endpointUri strategy suitable for
 * most connectors
 */

public abstract class AbstractEndpointURIBuilder implements EndpointURIBuilder
{
    protected String address;
    protected String endpointName;
    protected String connectorName;
    protected String transformers;
    protected String responseTransformers;
    protected String userInfo;
    private URI uri;

    @Override
    public EndpointURI build(URI uri, MuleContext muleContext) throws MalformedEndpointException
    {
        this.uri = uri;
        Properties props = getPropertiesForURI(uri, muleContext);
        String replaceAddress = null;
        //If the address has been set as a parameter on the URI, then we must ensure that that value is used
        //for the address. We still call the setEndpoint() method so that other information on the URI
        //is still processed
        if (address != null)
        {
            replaceAddress = address;
            setEndpoint(uri, props);
            address = replaceAddress;
        }
        else
        {
            setEndpoint(uri, props);
        }

        EndpointURI ep = new MuleEndpointURI(address, endpointName, connectorName, transformers,
            responseTransformers, props, this.uri, userInfo, muleContext);
        address = null;
        endpointName = null;
        connectorName = null;
        transformers = null;
        responseTransformers = null;
        this.uri = null;
        return ep;
    }

    protected void rewriteURI(URI newURI)
    {
        this.uri = newURI;
    }

    protected abstract void setEndpoint(URI uri, Properties props) throws MalformedEndpointException;

    protected Properties getPropertiesForURI(URI uri, MuleContext muleContext) throws MalformedEndpointException
    {
        Properties properties = PropertiesUtils.getPropertiesFromQueryString(uri.getRawQuery());

        // Decode values from the properties
        for (String propertyName : properties.stringPropertyNames())
        {
            String propertyValue = decode((String) properties.get(propertyName), uri, muleContext);
            properties.setProperty(propertyName, propertyValue);
        }

        String tempEndpointName = (String) properties.get(EndpointURI.PROPERTY_ENDPOINT_NAME);
        if (tempEndpointName != null)
        {
            this.endpointName = tempEndpointName;
        }
        // override the endpointUri if set
        String endpoint = (String) properties.get(EndpointURI.PROPERTY_ENDPOINT_URI);
        if (endpoint != null)
        {
            this.address = endpoint;
            address = decode(address, uri, muleContext);
        }

        String cnnName = (String) properties.get(EndpointURI.PROPERTY_CONNECTOR_NAME);
        if (cnnName != null)
        {
            this.connectorName = cnnName;
        }

        transformers = (String) properties.get(EndpointURI.PROPERTY_TRANSFORMERS);
        if (transformers != null)
        {
            transformers = transformers.replaceAll(" ", ",");
        }
        responseTransformers = (String) properties.get(EndpointURI.PROPERTY_RESPONSE_TRANSFORMERS);
        if (responseTransformers != null)
        {
            responseTransformers = responseTransformers.replaceAll(" ", ",");
        }
        
        userInfo = uri.getUserInfo();
        return properties;
    }

    private String decode(String string, URI uri, MuleContext context) throws MalformedEndpointException
    {
        try
        {
            String encoding = "UTF-8";
            if(context!=null)
            {
                encoding = context.getConfiguration().getDefaultEncoding();
            }
            return URLDecoder.decode(string, encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new MalformedEndpointException(uri.toString(), e);
        }
    }
}
