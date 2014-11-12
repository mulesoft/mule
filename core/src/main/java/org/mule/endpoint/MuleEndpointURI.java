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
import org.mule.api.endpoint.EndpointURIBuilder;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.ServiceException;
import org.mule.api.registry.ServiceType;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.service.TransportServiceDescriptor;
import org.mule.util.ClassUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleEndpointURI</code> is used to determine how a message is sent or received. The url
 * defines the protocol, the endpointUri destination of the message and optionally the endpoint to
 * use when dispatching the event. Mule urls take the form of -
 * protocol://[host]:[port]/[provider]/endpointUri or
 * protocol://[host]:[port]/endpointUri i.e. vm:///my.object
 * <br/>
 * The protocol can be any of any connector registered with Mule. The endpoint name if specified
 * must be the name of a registered global endpoint. The endpointUri can be any endpointUri
 * recognized by the endpoint type.
 */
public class MuleEndpointURI implements EndpointURI
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
    private Properties params = new Properties();
    private URI uri;
    private String userInfo;
    private String schemeMetaInfo;
    private String resourceInfo;
    private boolean dynamic;
    private transient MuleContext muleContext;
    private Properties serviceOverrides;

    MuleEndpointURI(String address,
                    String endpointName,
                    String connectorName,
                    String transformers,
                    String responseTransformers,
                    Properties properties,
                    URI uri,
                    String userInfo, MuleContext muleContext)
    {
        this(address, endpointName, connectorName, transformers, responseTransformers,
                properties, uri, muleContext);
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
                           Properties properties,
                           URI uri, MuleContext muleContext)
    {
        this.address = address;
        this.endpointName = endpointName;
        this.connectorName = connectorName;
        this.transformers = transformers;
        this.responseTransformers = responseTransformers;
        this.params = properties;
        this.uri = uri;
        this.userInfo = uri.getUserInfo();
        this.muleContext = muleContext;
        if (properties != null)
        {
            resourceInfo = (String) properties.remove("resourceInfo");
        }
    }

    public MuleEndpointURI(EndpointURI endpointUri)
    {
        initialise(endpointUri);
    }

    public MuleEndpointURI(EndpointURI endpointUri, String filterAddress)
    {
        initialise(endpointUri);
        this.filterAddress = filterAddress;
    }

    public MuleEndpointURI(String uri, MuleContext muleContext) throws EndpointException
    {
        this(uri, null, muleContext);
    }

    public MuleEndpointURI(String uri, MuleContext muleContext, Properties serviceOverrides) throws EndpointException
    {
        this(uri, null, muleContext);
        this.serviceOverrides = serviceOverrides;
    }

    /**
     * Creates but does not initialize the endpoint URI.  It is up to the caller
     * to call initialise() at some point.
     */
    public MuleEndpointURI(String uri, String encodedUri, MuleContext muleContext) throws EndpointException
    {
        this.muleContext = muleContext;
        uri = preprocessUri(uri);
        String startUri = uri;
        uri = convertExpressionDelimiters(uri, "#");
        uri = convertExpressionDelimiters(uri, "$");

        if (uri.indexOf("#[") >= 0)
        {
            address = uri;
            dynamic = true;
        }
        else
        {
            try
            {
                this.uri = new URI((encodedUri != null && uri.equals(startUri)) ? preprocessUri(encodedUri) : uri);
            }
            catch (URISyntaxException e)
            {
                throw new MalformedEndpointException(uri, e);
            }
            this.userInfo = this.uri.getRawUserInfo();
        }
    }

    private String convertExpressionDelimiters(String uriString, String startChar)
    {
        //Allow Expressions to be embedded
        int uriLength = uriString.length();
        for (int index = 0; index < uriLength; )
        {
            index = uriString.indexOf(startChar + "{", index);
            if (index < 0)
            {
                break;
            }
            int braceCount = 1;
            for (int seek = index + 2; seek < uriLength; seek++)
            {
                char c = uriString.charAt(seek);
                if (c == '{')
                {
                    braceCount++;
                }
                else if (c == '}')
                {
                    if (--braceCount == 0)
                    {
                        uriString = uriString.substring(0, index) + startChar + "[" + uriString.substring(index + 2, seek) + "]" + uriString.substring(seek+1);
                        break;
                    }
                }
            }
            index += 2;
        }
        return uriString;
    }

    protected String preprocessUri(String uriString) throws MalformedEndpointException
    {
        uriString = uriString.trim().replaceAll(" ", "%20");
        if (!validateUrl(uriString))
        {
            throw new MalformedEndpointException(uriString);
        }
        schemeMetaInfo = retrieveSchemeMetaInfo(uriString);
        if (schemeMetaInfo != null)
        {
            uriString = uriString.replaceFirst(schemeMetaInfo + ":", "");
        }
        return uriString;
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            String scheme = getFullScheme();
            TransportServiceDescriptor sd;
            sd = (TransportServiceDescriptor) muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, scheme, serviceOverrides);
            if (sd == null)
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
            }
            EndpointURIBuilder builder = sd.createEndpointURIBuilder();
            EndpointURI built = builder.build(this.uri, muleContext);
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

    private void initialise(EndpointURI endpointUri)
    {
        this.address = endpointUri.getAddress();
        if (this.endpointName == null)
        {
            this.endpointName = endpointUri.getEndpointName();
        }
        this.connectorName = endpointUri.getConnectorName();
        this.transformers = endpointUri.getTransformers();
        this.responseTransformers = endpointUri.getResponseTransformers();
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
        p.putAll(getParams());
        p.remove(PROPERTY_ENDPOINT_NAME);
        p.remove(PROPERTY_ENDPOINT_URI);
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
        return isDynamic() ? getDynamicScheme() : uri.getScheme();
    }

    private String getDynamicScheme()
    {
        int colon = address.indexOf(':');
        return address.substring(0, colon);
    }

    public String getFullScheme()
    {
        String scheme;
        if (dynamic)
        {
            scheme = getDynamicScheme();
        }
        else
        {
            scheme = uri.getScheme();
        }
        return (schemeMetaInfo == null ? scheme : schemeMetaInfo + ':' + scheme);
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

    @Override
    public String toString()
    {
        if (StringUtils.isNotEmpty(userInfo) && (userInfo.indexOf(":") > 0))
        {
            return createUriStringWithPasswordMasked();
        }
        return uri.toASCIIString();
    }

    protected String createUriStringWithPasswordMasked()
    {
        String rawUserInfo =  uri.getRawUserInfo();
        // uri.getRawUserInfo() returns null for JMS endpoints with passwords, so use the userInfo
        // from this instance instead
        if (StringUtils.isBlank(rawUserInfo))
        {
            rawUserInfo = userInfo;
        }

        String maskedUserInfo = null;
        int index = rawUserInfo.indexOf(":");
        if (index > -1)
        {
            maskedUserInfo = rawUserInfo.substring(0, index);
        }

        maskedUserInfo = maskedUserInfo + ":****";
        return uri.toASCIIString().replace(rawUserInfo, maskedUserInfo);
    }

    public String getTransformers()
    {
        return transformers;
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

    public String getUser()
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

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public boolean isDynamic()
    {
        return dynamic;
    }

    @Override
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
        MuleEndpointURI muleEndpointURI = (MuleEndpointURI) o;
        return ClassUtils.equal(address, muleEndpointURI.address) &&
                ClassUtils.equal(connectorName, muleEndpointURI.connectorName) &&
                ClassUtils.equal(endpointName, muleEndpointURI.endpointName) &&
                ClassUtils.equal(filterAddress, muleEndpointURI.filterAddress) &&
                ClassUtils.equal(params, muleEndpointURI.params) &&
                ClassUtils.equal(resourceInfo, muleEndpointURI.resourceInfo) &&
                ClassUtils.equal(schemeMetaInfo, muleEndpointURI.schemeMetaInfo) &&
                ClassUtils.equal(transformers, muleEndpointURI.transformers) &&
                ClassUtils.equal(responseTransformers, muleEndpointURI.responseTransformers) &&
                ClassUtils.equal(uri, muleEndpointURI.uri);
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{
                address,
                filterAddress,
                endpointName,
                connectorName,
                transformers,
                responseTransformers,
                params,
                uri,
                schemeMetaInfo,
                resourceInfo
        });
    }
}
