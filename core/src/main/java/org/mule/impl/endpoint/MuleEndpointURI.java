/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.service.TransportFactory;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleEndpointURI</code> is used to determine how a message is sent of
 * received. The url defines the protocol, the endpointUri destination of the message
 * and optionally the endpoint to use when dispatching the event. Mule urls take the
 * form of - protocol://[host]:[port]/[provider]/endpointUri or
 * protocol://[host]:[port]/endpointUri i.e. vm:///my.object or The protocol can be
 * any of any connector registered with Mule. The endpoint name if specified must be
 * the name of a register global endpoint The endpointUri can be any endpointUri
 * recognised by the endpoint type.
 */

public class MuleEndpointURI implements UMOEndpointURI
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3906735768171252877L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleEndpointURI.class);

    public static boolean isMuleUri(String url)
    {
        return url.indexOf(":/") != -1;
    }

    private String address;
    private String filterAddress;
    private String endpointName;
    private String connectorName;
    private String transformers;
    private String responseTransformers;
    private int createConnector = TransportFactory.GET_OR_CREATE_CONNECTOR;
    private Properties params = new Properties();
    private URI uri;
    private String userInfo;
    private String schemeMetaInfo;
    private String resourceInfo;

    MuleEndpointURI(String address,
                    String endpointName,
                    String connectorName,
                    String transformers,
                    String responseTransformers,
                    int createConnector,
                    Properties properties,
                    URI uri,
                    String userInfo)
    {
        this(address, endpointName, connectorName, transformers, responseTransformers, createConnector,
            properties, uri);
        if (userInfo != null)
        {
            this.userInfo = userInfo;
        }
    }

    public MuleEndpointURI(String address,
                           String endpointName,
                           String connectorName,
                           String transformers,
                           String responseTransformers,
                           int createConnector,
                           Properties properties,
                           URI uri)
    {
        this.address = address;
        this.endpointName = endpointName;
        this.connectorName = connectorName;
        this.transformers = transformers;
        this.responseTransformers = responseTransformers;
        this.createConnector = createConnector;
        this.params = properties;
        this.uri = uri;
        this.userInfo = uri.getUserInfo();
        if (properties != null)
        {
            resourceInfo = (String) properties.remove("resourceInfo");
        }
    }

    public MuleEndpointURI(UMOEndpointURI endpointUri)
    {
        initialise(endpointUri);
    }

    public MuleEndpointURI(UMOEndpointURI endpointUri, String filterAddress)
    {
        initialise(endpointUri);
        this.filterAddress = filterAddress;
    }

    public MuleEndpointURI(String uri) throws EndpointException
    {
        uri = uri.trim().replaceAll(" ", "%20");

        if (!validateUrl(uri))
        {
            throw new MalformedEndpointException(uri);
        }
        try
        {
            schemeMetaInfo = retrieveSchemeMetaInfo(uri);
            if (schemeMetaInfo != null)
            {
                uri = uri.replaceFirst(schemeMetaInfo + ":", "");
            }
            this.uri = new URI(uri);
            this.userInfo = this.uri.getRawUserInfo();
        }
        catch (URISyntaxException e)
        {
            throw new MalformedEndpointException(uri, e);
        }

//        try
//        {
//            initialise();
//        }
//        catch (InitialisationException e)
//        {
//            throw new EndpointException(e);
//        }

//        try
//        {
//            String scheme = (schemeMetaInfo == null ? this.uri.getScheme() : schemeMetaInfo);
//            TransportServiceDescriptor sd;
//            sd = (TransportServiceDescriptor)RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, null);
//            if (sd == null)
//            {
//                throw new ServiceException(Message.createStaticMessage("No service descriptor found for transport: " + scheme + ".  This transport does not appear to be installed."));
//            }
//        }
//        catch (Exception e)
//        {
//            throw new EndpointException(e);
//        }
    }


    public void initialise() throws InitialisationException
    {
        try
        {
            String scheme = (schemeMetaInfo == null ? this.uri.getScheme() : schemeMetaInfo);
            TransportServiceDescriptor sd;
            sd = (TransportServiceDescriptor)RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, null);
            if (sd == null)
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
            }
            EndpointURIBuilder builder = sd.createEndpointBuilder();
            UMOEndpointURI built = builder.build(this.uri);
            initialise(built);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    private String retrieveSchemeMetaInfo(String url)
    {
        int i = url.indexOf(':');
        if (i == -1)
        {
            return null;
        }
        if (url.charAt(i + 1) == '/')
        {
            return null;
        }
        else
        {
            return url.substring(0, i);
        }
    }

    protected boolean validateUrl(String url)
    {
        return (url.indexOf(":/") > 0);
    }

    private void initialise(UMOEndpointURI endpointUri)
    {
        this.address = endpointUri.getAddress();
        if (this.endpointName == null)
        {
            this.endpointName = endpointUri.getEndpointName();
        }
        this.connectorName = endpointUri.getConnectorName();
        this.transformers = endpointUri.getTransformers();
        this.responseTransformers = endpointUri.getResponseTransformers();
        this.createConnector = endpointUri.getCreateConnector();
        this.params = endpointUri.getParams();
        this.uri = endpointUri.getUri();
        this.resourceInfo = endpointUri.getResourceInfo();
        this.userInfo = endpointUri.getUserInfo();
    }

    public String getAddress()
    {
        return address;
    }

    public String getEndpointName()
    {
        return (StringUtils.isEmpty(endpointName) ? null : endpointName);
    }

    public Properties getParams()
    {
        // TODO fix this so that the query string properties are not lost.
        // not sure whats causing this at the moment
        if (params.size() == 0 && getQuery() != null)
        {
            params = PropertiesUtils.getPropertiesFromQueryString(getQuery());
        }
        return params;
    }

    public Properties getUserParams()
    {
        Properties p = new Properties();
        p.putAll(params);
        p.remove(PROPERTY_ENDPOINT_NAME);
        p.remove(PROPERTY_ENDPOINT_URI);
        p.remove(PROPERTY_CREATE_CONNECTOR);
        p.remove(PROPERTY_TRANSFORMERS);
        return p;
    }

    public URI parseServerAuthority() throws URISyntaxException
    {
        return uri.parseServerAuthority();
    }

    public URI normalize()
    {
        return uri.normalize();
    }

    public URI resolve(URI uri)
    {
        return uri.resolve(uri);
    }

    public URI resolve(String str)
    {
        return uri.resolve(str);
    }

    public URI relativize(URI uri)
    {
        return uri.relativize(uri);
    }

    public String getScheme()
    {
        return uri.getScheme();
    }

    public String getFullScheme()
    {
        return (schemeMetaInfo == null ? uri.getScheme() : schemeMetaInfo + ':' + uri.getScheme());

    }

    public boolean isAbsolute()
    {
        return uri.isAbsolute();
    }

    public boolean isOpaque()
    {
        return uri.isOpaque();
    }

    public String getRawSchemeSpecificPart()
    {
        return uri.getRawSchemeSpecificPart();
    }

    public String getSchemeSpecificPart()
    {
        return uri.getSchemeSpecificPart();
    }

    public String getRawAuthority()
    {
        return uri.getRawAuthority();
    }

    public String getAuthority()
    {
        return uri.getAuthority();
    }

    public String getRawUserInfo()
    {
        return uri.getRawUserInfo();
    }

    public String getUserInfo()
    {
        return userInfo;
    }

    public String getHost()
    {
        return uri.getHost();
    }

    public int getPort()
    {
        return uri.getPort();
    }

    public String getRawPath()
    {
        return uri.getRawPath();
    }

    public String getPath()
    {
        return uri.getPath();
    }

    public String getRawQuery()
    {
        return uri.getRawQuery();
    }

    public String getQuery()
    {
        return uri.getQuery();
    }

    public String getRawFragment()
    {
        return uri.getRawFragment();
    }

    public String getFragment()
    {
        return uri.getFragment();
    }

    public String toString()
    {
        return uri.toASCIIString();
    }

    public String getTransformers()
    {
        return transformers;
    }

    public int getCreateConnector()
    {
        return createConnector;
    }

    public URI getUri()
    {
        return uri;
    }

    public String getConnectorName()
    {
        return connectorName;
    }

    public String getSchemeMetaInfo()
    {
        return (schemeMetaInfo == null ? uri.getScheme() : schemeMetaInfo);
    }

    public String getResourceInfo()
    {
        return resourceInfo;
    }

    public String getFilterAddress()
    {
        return filterAddress;
    }

    public void setEndpointName(String name)
    {
        endpointName = name;
    }

    public String getUsername()
    {
        if (StringUtils.isNotBlank(userInfo))
        {
            int i = userInfo.indexOf(':');
            if (i == -1)
            {
                return userInfo;
            }
            else
            {
                return userInfo.substring(0, i);
            }
        }
        return null;
    }

    public String getResponseTransformers()
    {
        return responseTransformers;
    }

    public String getPassword()
    {
        if (StringUtils.isNotBlank(userInfo))
        {
            int i = userInfo.indexOf(':');
            if (i > -1)
            {
                return userInfo.substring(i + 1);
            }
        }
        return null;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof MuleEndpointURI))
        {
            return false;
        }

        final MuleEndpointURI muleEndpointURI = (MuleEndpointURI) o;

        if (createConnector != muleEndpointURI.createConnector)
        {
            return false;
        }
        if (address != null ? !address.equals(muleEndpointURI.address) : muleEndpointURI.address != null)
        {
            return false;
        }
        if (connectorName != null
                        ? !connectorName.equals(muleEndpointURI.connectorName)
                        : muleEndpointURI.connectorName != null)
        {
            return false;
        }
        if (endpointName != null
                        ? !endpointName.equals(muleEndpointURI.endpointName)
                        : muleEndpointURI.endpointName != null)
        {
            return false;
        }
        if (filterAddress != null
                        ? !filterAddress.equals(muleEndpointURI.filterAddress)
                        : muleEndpointURI.filterAddress != null)
        {
            return false;
        }
        if (params != null ? !params.equals(muleEndpointURI.params) : muleEndpointURI.params != null)
        {
            return false;
        }
        if (resourceInfo != null
                        ? !resourceInfo.equals(muleEndpointURI.resourceInfo)
                        : muleEndpointURI.resourceInfo != null)
        {
            return false;
        }
        if (schemeMetaInfo != null
                        ? !schemeMetaInfo.equals(muleEndpointURI.schemeMetaInfo)
                        : muleEndpointURI.schemeMetaInfo != null)
        {
            return false;
        }
        if (transformers != null
                        ? !transformers.equals(muleEndpointURI.transformers)
                        : muleEndpointURI.transformers != null)
        {
            return false;
        }
        if (responseTransformers != null
                        ? !responseTransformers.equals(muleEndpointURI.responseTransformers)
                        : muleEndpointURI.responseTransformers != null)
        {
            return false;
        }
        if (uri != null ? !uri.equals(muleEndpointURI.uri) : muleEndpointURI.uri != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = (address != null ? address.hashCode() : 0);
        result = 29 * result + (filterAddress != null ? filterAddress.hashCode() : 0);
        result = 29 * result + (endpointName != null ? endpointName.hashCode() : 0);
        result = 29 * result + (connectorName != null ? connectorName.hashCode() : 0);
        result = 29 * result + (transformers != null ? transformers.hashCode() : 0);
        result = 29 * result + (responseTransformers != null ? responseTransformers.hashCode() : 0);
        result = 29 * result + createConnector;
        result = 29 * result + (params != null ? params.hashCode() : 0);
        result = 29 * result + (uri != null ? uri.hashCode() : 0);
        result = 29 * result + (schemeMetaInfo != null ? schemeMetaInfo.hashCode() : 0);
        return 29 * result + (resourceInfo != null ? resourceInfo.hashCode() : 0);
    }
}
