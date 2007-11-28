/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.config.i18n.CoreMessages;
import org.mule.providers.tcp.TcpConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.codec.binary.Base64;

/**
 * <code>HttpConnector</code> provides a way of receiving and sending http requests
 * and responses. The UMOConnector itself handles dispatching http requests. The
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

    /**
     * Event property to pass back the status for the response
     */
    public static final String HTTP_STATUS_PROPERTY = "http.status";
    public static final String HTTP_VERSION_PROPERTY = "http.version";
    public static final String HTTP_CUSTOM_HEADERS_MAP_PROPERTY = "http.custom.headers";
    public static final String HTTP_METHOD_PROPERTY = "http.method";
    public static final String HTTP_REQUEST_PROPERTY = "http.request";

    /**
     * Allows the user to set a {@link org.apache.commons.httpclient.params.HttpMethodParams} object in the client
     * request to be set on the HttpMethod request object
     */
    public static final String HTTP_PARAMS_PROPERTY = "http.params";
    public static final String HTTP_GET_BODY_PARAM_PROPERTY = "http.get.body.param";
    public static final String DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY = "body";
    public static final String HTTP_POST_BODY_PARAM_PROPERTY = "http.post.body.param";

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


    //@Override
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
     * @see UMOConnector#registerListener(UMOComponent, UMOEndpoint)
     */
    public UMOMessageReceiver registerListener(UMOComponent component, UMOImmutableEndpoint endpoint) throws Exception
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
                    Map.Entry entry = (Map.Entry)entries.next();
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
        return super.registerListener(component, endpoint);
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param component the component for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the component
     * @return the key to store the newly created receiver against
     */
    protected Object getReceiverKey(UMOComponent component, UMOImmutableEndpoint endpoint)
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
     * @see org.mule.umo.provider.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "http";
    }

    /**
     * @return
     */
    public String getProxyHostname()
    {
        return proxyHostname;
    }

    /**
     * @return
     */
    public String getProxyPassword()
    {
        return proxyPassword;
    }

    /**
     * @return
     */
    public int getProxyPort()
    {
        return proxyPort;
    }

    /**
     * @return
     */
    public String getProxyUsername()
    {
        return proxyUsername;
    }

    /**
     * @param host
     */
    public void setProxyHostname(String host)
    {
        proxyHostname = host;
    }

    /**
     * @param string
     */
    public void setProxyPassword(String string)
    {
        proxyPassword = string;
    }

    /**
     * @param port
     */
    public void setProxyPort(int port)
    {
        proxyPort = port;
    }

    /**
     * @param string
     */
    public void setProxyUsername(String string)
    {
        proxyUsername = string;
    }

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

    protected void setupClientAuthorization(UMOEvent event, HttpMethod httpMethod,
                                            HttpClient client, UMOImmutableEndpoint endpoint)
            throws UnsupportedEncodingException
    {
        httpMethod.setDoAuthentication(true);
        if (event != null && event.getCredentials() != null)
        {
            UMOMessage msg = event.getMessage();
            String authScopeHost = msg.getStringProperty("http.auth.scope.host", null);
            int authScopePort = msg.getIntProperty("http.auth.scope.port", -1);
            String authScopeRealm = msg.getStringProperty("http.auth.scope.realm", null);
            String authScopeScheme = msg.getStringProperty("http.auth.scope.scheme", null);
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

}
