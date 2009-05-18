/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.tcp.TcpConnector;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

/**
 * <code>HttpConnector</code> provides a way of receiving and sending http requests
 * and responses. The Connector itself handles dispatching http requests. The
 * <code>HttpMessageReceiver</code> handles the receiving requests and processing
 * of headers This endpoint recognises the following properties - <p/>
 * <ul>
 * <li>hostname - The hostname to send and receive http requests</li>
 * <li>port - The port to listen on. The industry standard is 80 and if this propert
 * is not set it will default to 80</li>
 * <li>proxyHostname - If you access the web through a proxy, this holds the server
 * address</li>
 * <li>proxyPort - The port the proxy is configured on</li>
 * <li>proxyUsername - If the proxy requires authentication supply a username</li>
 * <li>proxyPassword - If the proxy requires authentication supply a password</li>
 * </ul>
 * 
 */

public class HttpConnector extends TcpConnector
{

    public static final String HTTP = "http";
    public static final String HTTP_PREFIX = "http.";
    
    /**
     * MuleEvent property to pass back the status for the response
     */
    public static final String HTTP_STATUS_PROPERTY = HTTP_PREFIX + "status";
    public static final String HTTP_VERSION_PROPERTY = HTTP_PREFIX + "version";
    public static final String HTTP_CUSTOM_HEADERS_MAP_PROPERTY = HTTP_PREFIX + "custom.headers";
    public static final String HTTP_METHOD_PROPERTY = HTTP_PREFIX + "method";
    
    /**
     * The path and query portions of the URL being accessed. 
     */
    public static final String HTTP_REQUEST_PROPERTY = HTTP_PREFIX + "request";
    
    /**
     * The path portion of the URL being accessed. No query string is included.
     */
    public static final String HTTP_REQUEST_PATH_PROPERTY = HTTP_PREFIX + "request.path";
    
    /**
     * The context path of the endpoint being accessed. This is the path that the 
     * HTTP endpoint is listening on.
     */
    public static final String HTTP_CONTEXT_PATH_PROPERTY = HTTP_PREFIX + "context.path";

    /**
     * Allows the user to set a {@link org.apache.commons.httpclient.params.HttpMethodParams} object in the client
     * request to be set on the HttpMethod request object
     */
    public static final String HTTP_PARAMS_PROPERTY = HTTP_PREFIX + "params";
    public static final String HTTP_GET_BODY_PARAM_PROPERTY = HTTP_PREFIX + "get.body.param";
    public static final String DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY = "body";
    public static final String HTTP_POST_BODY_PARAM_PROPERTY = HTTP_PREFIX + "post.body.param";

    public static final Set<String> HTTP_INBOUND_PROPERTIES;
    
    static 
    {
        Set<String> props = new HashSet<String>();
        props.add(HTTP_CONTEXT_PATH_PROPERTY);
        props.add(HTTP_GET_BODY_PARAM_PROPERTY);
        props.add(HTTP_METHOD_PROPERTY);
        props.add(HTTP_PARAMS_PROPERTY);
        props.add(HTTP_POST_BODY_PARAM_PROPERTY);
        props.add(HTTP_REQUEST_PROPERTY);
        props.add(HTTP_REQUEST_PATH_PROPERTY);
        props.add(HTTP_STATUS_PROPERTY);
        props.add(HTTP_VERSION_PROPERTY);
        HTTP_INBOUND_PROPERTIES = props;
    }
    
    public static final String HTTP_COOKIE_SPEC_PROPERTY = "cookieSpec";
    public static final String HTTP_COOKIES_PROPERTY = "cookies";
    public static final String HTTP_ENABLE_COOKIES_PROPERTY = "enableCookies";

    public static final String COOKIE_SPEC_NETSCAPE = "netscape";
    public static final String COOKIE_SPEC_RFC2109 = "rcf2109";

    private String proxyHostname = null;

    private int proxyPort = HttpConstants.DEFAULT_HTTP_PORT;

    private String proxyUsername = null;

    private String proxyPassword = null;

    private String cookieSpec;

    private boolean enableCookies = false;

    protected HttpConnectionManager clientConnectionManager;

    private boolean followRedirects = false;

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if (clientConnectionManager == null)
        {
            clientConnectionManager = new MultiThreadedHttpConnectionManager();
            HttpConnectionManagerParams params = new HttpConnectionManagerParams();
            if (getSendBufferSize() != INT_VALUE_NOT_SET)
            {
                params.setSendBufferSize(getSendBufferSize());
            }
            if (getReceiveBufferSize() != INT_VALUE_NOT_SET)
            {
                params.setReceiveBufferSize(getReceiveBufferSize());
            }
            if (getClientSoTimeout() != INT_VALUE_NOT_SET)
            {
                params.setSoTimeout(getClientSoTimeout());
            }
            if (getSocketSoLinger() != INT_VALUE_NOT_SET)
            {
                params.setLinger(getSocketSoLinger());
            }

            params.setTcpNoDelay(isSendTcpNoDelay());
            params.setMaxTotalConnections(getDispatcherThreadingProfile().getMaxThreadsActive());
            params.setDefaultMaxConnectionsPerHost(getDispatcherThreadingProfile().getMaxThreadsActive());

            clientConnectionManager.setParams(params);
        }
    }

    /**
     * @see Connector#registerListener(Service, InboundEndpoint)
     */
    @Override
    public MessageReceiver registerListener(Service service, InboundEndpoint endpoint) throws Exception
    {
        if (endpoint != null)
        {
            Map endpointProperties = endpoint.getProperties();
            if (endpointProperties != null)
            {
                // normalize properties for HTTP
                Map newProperties = new HashMap(endpointProperties.size());
                for (Iterator entries = endpointProperties.entrySet().iterator(); entries.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) entries.next();
                    Object key = entry.getKey();
                    Object normalizedKey = HttpConstants.ALL_HEADER_NAMES.get(key);
                    if (normalizedKey != null)
                    {
                        // normalized property exists
                        key = normalizedKey;
                    }
                    newProperties.put(key, entry.getValue());
                }
                // set normalized properties back on the endpoint
                endpoint.getProperties().clear();
                endpoint.getProperties().putAll(newProperties);
            }
        }
        // proceed as usual
        return super.registerListener(service, endpoint);
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param service the service for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the service
     * @return the key to store the newly created receiver against
     */
    @Override
    protected Object getReceiverKey(Service service, InboundEndpoint endpoint)
    {
        String key = endpoint.getEndpointURI().toString();
        int i = key.indexOf('?');
        if (i > -1)
        {
            key = key.substring(0, i);
        }
        return key;
    }

    /**
     * @see org.mule.api.transport.Connector#getProtocol()
     */
    @Override
    public String getProtocol()
    {
        return HTTP;
    }

    public String getProxyHostname()
    {
        return proxyHostname;
    }

    public String getProxyPassword()
    {
        return proxyPassword;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyUsername()
    {
        return proxyUsername;
    }

    public void setProxyHostname(String host)
    {
        proxyHostname = host;
    }

    public void setProxyPassword(String string)
    {
        proxyPassword = string;
    }

    public void setProxyPort(int port)
    {
        proxyPort = port;
    }

    public void setProxyUsername(String string)
    {
        proxyUsername = string;
    }

    @Override
    public Map getReceivers()
    {
        return this.receivers;
    }

    public String getCookieSpec()
    {
        return cookieSpec;
    }

    public void setCookieSpec(String cookieSpec)
    {
        if (!(COOKIE_SPEC_NETSCAPE.equalsIgnoreCase(cookieSpec) || COOKIE_SPEC_RFC2109.equalsIgnoreCase(cookieSpec)))
        {
            throw new IllegalArgumentException(
                CoreMessages.propertyHasInvalidValue("cookieSpec", cookieSpec).toString());
        }
        this.cookieSpec = cookieSpec;
    }

    public boolean isEnableCookies()
    {
        return enableCookies;
    }

    public void setEnableCookies(boolean enableCookies)
    {
        this.enableCookies = enableCookies;
    }


    public HttpConnectionManager getClientConnectionManager()
    {
        return clientConnectionManager;
    }

    public void setClientConnectionManager(HttpConnectionManager clientConnectionManager)
    {
        this.clientConnectionManager = clientConnectionManager;
    }

    public boolean isFollowRedirects()
    {
        return followRedirects;
    }
    
    public void setFollowRedirects(boolean followRedirects)
    {
        this.followRedirects = followRedirects;
    }
    
    protected HttpClient doClientConnect() throws Exception
    {
        HttpState state = new HttpState();

        if (getProxyUsername() != null)
        {
            state.setProxyCredentials(
                    new AuthScope(null, -1, null, null),
                    new UsernamePasswordCredentials(getProxyUsername(), getProxyPassword()));
        }

        HttpClient client = new HttpClient();
        client.setState(state);
        client.setHttpConnectionManager(getClientConnectionManager());

        return client;
    }

    protected void setupClientAuthorization(MuleEvent event, HttpMethod httpMethod,
                                            HttpClient client, ImmutableEndpoint endpoint)
            throws UnsupportedEncodingException
    {
        httpMethod.setDoAuthentication(true);
        if (event != null && event.getCredentials() != null)
        {
            MuleMessage msg = event.getMessage();
            String authScopeHost = msg.getStringProperty(HTTP_PREFIX + "auth.scope.host", null);
            int authScopePort = msg.getIntProperty(HTTP_PREFIX + "auth.scope.port", -1);
            String authScopeRealm = msg.getStringProperty(HTTP_PREFIX + "auth.scope.realm", null);
            String authScopeScheme = msg.getStringProperty(HTTP_PREFIX + "auth.scope.scheme", null);
            client.getState().setCredentials(
                new AuthScope(authScopeHost, authScopePort, authScopeRealm, authScopeScheme),
                new UsernamePasswordCredentials(event.getCredentials().getUsername(), new String(
                    event.getCredentials().getPassword())));
            client.getParams().setAuthenticationPreemptive(true);
        }
        else if (endpoint.getEndpointURI().getUserInfo() != null
            && endpoint.getProperty(HttpConstants.HEADER_AUTHORIZATION) == null)
        {
            // Add User Creds
            StringBuffer header = new StringBuffer(128);
            header.append("Basic ");
            header.append(new String(Base64.encodeBase64(endpoint.getEndpointURI().getUserInfo().getBytes(
                endpoint.getEncoding()))));
            httpMethod.addRequestHeader(HttpConstants.HEADER_AUTHORIZATION, header.toString());
        }
        else
        {
            // don't use preemptive if there are no credentials to send
            client.getParams().setAuthenticationPreemptive(false);
        }
    }

    /**
     * Ensures that the supplied URL starts with a '/'.
     */
    public static String normalizeUrl(String url)
    {
        if (url == null) 
        {
            url = "/";
        } 
        else if (!url.startsWith("/")) 
        {
            url = "/" + url;
        }
        return url;
    }

}
