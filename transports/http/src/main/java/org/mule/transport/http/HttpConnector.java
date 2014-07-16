/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.MessageDispatcherUtils;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.http.ntlm.NTLMScheme;
import org.mule.transport.tcp.TcpConnector;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.util.IdleConnectionTimeoutThread;
import org.apache.commons.lang.BooleanUtils;

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
 */

public class HttpConnector extends TcpConnector
{

    public static final String HTTP = "http";
    public static final String HTTP_PREFIX = "http.";
    public static final String DISABLE_STALE_CONNECTION_CHECK_SYSTEM_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX
                                                                                + "transport."
                                                                                + HTTP_PREFIX
                                                                                + "disableHttpClientStaleConnectionCheck";
    public static final String SINGLE_DISPATCHER_PER_ENDPOINT_SYSTEM_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX
                                                                                + "transport."
                                                                                + HTTP_PREFIX
                                                                                + "singleDispatcherPerEndpoint";

    /**
     * MuleEvent property to pass back the status for the response
     */
    public static final String HTTP_STATUS_PROPERTY = HTTP_PREFIX + "status";
    public static final String HTTP_VERSION_PROPERTY = HTTP_PREFIX + "version";

    /**
     * @deprecated Instead users can now add properties to the outgoing request using the OUTBOUND property scope on the message.
     */
    @Deprecated
    public static final String HTTP_CUSTOM_HEADERS_MAP_PROPERTY = HTTP_PREFIX + "custom.headers";

    /**
     * Encapsulates all the HTTP headers
     */
    public static final String HTTP_HEADERS = HTTP_PREFIX + "headers";

    /**
     * Stores the HTTP query parameters received, supports multiple values per key and both query parameter key and
     * value are unescaped
     */
    public static final String HTTP_QUERY_PARAMS = HTTP_PREFIX + "query.params";

    public static final String HTTP_QUERY_STRING = HTTP_PREFIX + "query.string";

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
     * The context URI of the endpoint being accessed. This is the address that the
     * HTTP endpoint is listening on. It includes: [scheme]://[host]:[port][http.context.path]
     */
    public static final String HTTP_CONTEXT_URI_PROPERTY = HTTP_PREFIX + "context.uri";


    /**
     * The relative path of the URI being accessed in relation to the context path
     */
    public static final String HTTP_RELATIVE_PATH_PROPERTY = HTTP_PREFIX + "relative.path";

    public static final String HTTP_SERVLET_REQUEST_PROPERTY = HTTP_PREFIX + "servlet.request";
    public static final String HTTP_SERVLET_RESPONSE_PROPERTY = HTTP_PREFIX + "servlet.response";

    /**
     * Allows the user to set a {@link org.apache.commons.httpclient.params.HttpMethodParams} object in the client
     * request to be set on the HttpMethod request object
     */
    public static final String HTTP_PARAMS_PROPERTY = HTTP_PREFIX + "params";
    public static final String HTTP_GET_BODY_PARAM_PROPERTY = HTTP_PREFIX + "get.body.param";
    public static final String DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY = "body";
    public static final String HTTP_POST_BODY_PARAM_PROPERTY = HTTP_PREFIX + "post.body.param";

    public static final String HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK = HTTP_PREFIX + "disable.status.code.exception.check";
    public static final String HTTP_ENCODE_PARAMVALUE = HTTP_PREFIX + "encode.paramvalue";

    public static final Set<String> HTTP_INBOUND_PROPERTIES;
    
    protected Map<OutboundEndpoint, MessageDispatcher> endpointDispatchers = new ConcurrentHashMap<OutboundEndpoint, MessageDispatcher>();

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
        props.add(HTTP_ENCODE_PARAMVALUE);
        HTTP_INBOUND_PROPERTIES = props;

        AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, NTLMScheme.class);
    }

    public static final String HTTP_COOKIE_SPEC_PROPERTY = "cookieSpec";
    public static final String HTTP_COOKIES_PROPERTY = "cookies";
    public static final String HTTP_ENABLE_COOKIES_PROPERTY = "enableCookies";

    public static final String COOKIE_SPEC_NETSCAPE = "netscape";
    public static final String COOKIE_SPEC_RFC2109 = "rfc2109";
    public static final String ROOT_PATH = "/";
    public static final int DEFAULT_CONNECTION_TIMEOUT = 2000;

    private String proxyHostname = null;

    private int proxyPort = HttpConstants.DEFAULT_HTTP_PORT;

    private String proxyUsername = null;

    private String proxyPassword = null;

    private boolean proxyNtlmAuthentication;

    private String cookieSpec;

    private boolean enableCookies = false;

    protected HttpConnectionManager clientConnectionManager;

    private IdleConnectionTimeoutThread connectionCleaner;

    private boolean disableCleanupThread;

    private org.mule.transport.http.HttpConnectionManager connectionManager;
    
    private boolean singleDispatcherPerEndpoint = false;

    public HttpConnector(MuleContext context)
    {
        super(context);
        singleDispatcherPerEndpoint = BooleanUtils.toBoolean(System.getProperty(SINGLE_DISPATCHER_PER_ENDPOINT_SYSTEM_PROPERTY));
        
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if (clientConnectionManager == null)
        {
            clientConnectionManager = new MultiThreadedHttpConnectionManager();
            String prop = System.getProperty("mule.http.disableCleanupThread");
            disableCleanupThread = prop != null && prop.equals("true");
            if (!disableCleanupThread)
            {
                connectionCleaner = new IdleConnectionTimeoutThread();
                connectionCleaner.setName("HttpClient-connection-cleaner-" + getName());
                connectionCleaner.addConnectionManager(clientConnectionManager);
                connectionCleaner.start();
            }

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
            params.setMaxTotalConnections(dispatchers.getMaxTotal());
            params.setDefaultMaxConnectionsPerHost(dispatchers.getMaxTotal());
            params.setStaleCheckingEnabled(!BooleanUtils.toBoolean(System.getProperty(DISABLE_STALE_CONNECTION_CHECK_SYSTEM_PROPERTY)));

            if (getConnectionTimeout() != INT_VALUE_NOT_SET)
            {
                params.setConnectionTimeout(getConnectionTimeout());
            }
            else
            {
                params.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
            }

            clientConnectionManager.setParams(params);
        }
        //connection manager must be created during initialization due that devkit requires the connection manager before start phase.
        //That's why it not manager only during stop/start phases and must be created also here.
        if (connectionManager == null)
        {
            try
            {
                connectionManager = new org.mule.transport.http.HttpConnectionManager(this, getReceiverWorkManager());
            }
            catch (MuleException e)
            {
                throw new InitialisationException(CoreMessages.createStaticMessage("failed creating http connection manager"),this);
            }
        }
    }

    @Override
    protected void doDispose()
    {
        if (!disableCleanupThread)
        {
            connectionCleaner.shutdown();

            if (!muleContext.getConfiguration().isStandalone())
            {
                MultiThreadedHttpConnectionManager.shutdownAll();
            }
        }
        if (this.connectionManager != null)
        {
            connectionManager.dispose();
            connectionManager = null;
        }
        super.doDispose();
    }

    @Override
    protected void doStop() throws MuleException
    {
        this.connectionManager.dispose();
        this.connectionManager = null;
    }

    @Override
    protected void doStart() throws MuleException
    {
        super.doStart();
        if (this.connectionManager == null)
        {
            this.connectionManager = new org.mule.transport.http.HttpConnectionManager(this,getReceiverWorkManager());
        }
    }

    @Override
    public void registerListener(InboundEndpoint endpoint, MessageProcessor listener, FlowConstruct flowConstruct) throws Exception
    {
        if (endpoint != null)
        {
            Map endpointProperties = endpoint.getProperties();
            if (endpointProperties != null)
            {
                // normalize properties for HTTP
                Map newProperties = new HashMap(endpointProperties.size());
                for (Iterator entries = endpointProperties.entrySet().iterator(); entries.hasNext(); )
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
        super.registerListener(endpoint, listener, flowConstruct);
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param endpoint the endpoint being registered for the service
     * @return the key to store the newly created receiver against
     */
    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
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

    protected HttpClient doClientConnect() throws Exception
    {
        HttpState state = new HttpState();

        if (getProxyUsername() != null)
        {
            Credentials credentials;
            if (isProxyNtlmAuthentication())
            {
                credentials = new NTCredentials(getProxyUsername(), getProxyPassword(), getProxyHostname(), "");
            }
            else
            {
                credentials = new UsernamePasswordCredentials(getProxyUsername(), getProxyPassword());
            }

            AuthScope authscope = new AuthScope(getProxyHostname(), getProxyPort());

            state.setProxyCredentials(authscope, credentials);
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
        client.getParams().setAuthenticationPreemptive(true);

        if (event != null && event.getCredentials() != null)
        {
            MuleMessage msg = event.getMessage();
            String authScopeHost = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.host", event.getMessageSourceURI().getHost());
            int authScopePort = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.port", event.getMessageSourceURI().getPort());
            String authScopeRealm = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.realm", AuthScope.ANY_REALM);
            String authScopeScheme = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.scheme", AuthScope.ANY_SCHEME);
            client.getState().setCredentials(
                    new AuthScope(authScopeHost, authScopePort, authScopeRealm, authScopeScheme),
                    new UsernamePasswordCredentials(event.getCredentials().getUsername(), new String(
                            event.getCredentials().getPassword())));
        }
        else if (endpoint.getEndpointURI().getUserInfo() != null
                 && endpoint.getProperty(HttpConstants.HEADER_AUTHORIZATION) == null)
        {
            // Add User Creds
            StringBuilder header = new StringBuilder(128);
            header.append("Basic ");
            header.append(new String(Base64.encodeBase64(endpoint.getEndpointURI().getUserInfo().getBytes(
                    endpoint.getEncoding()))));
            httpMethod.addRequestHeader(HttpConstants.HEADER_AUTHORIZATION, header.toString());
        }
        //TODO MULE-4501 this sohuld be removed and handled only in the ObjectToHttpRequest transformer
        else if (event != null && event.getMessage().getOutboundProperty(HttpConstants.HEADER_AUTHORIZATION) != null &&
                 httpMethod.getRequestHeader(HttpConstants.HEADER_AUTHORIZATION) == null)
        {
            String auth = event.getMessage().getOutboundProperty(HttpConstants.HEADER_AUTHORIZATION);
            httpMethod.addRequestHeader(HttpConstants.HEADER_AUTHORIZATION, auth);
        }
        else if (StringUtils.isEmpty(getProxyUsername()))
        {
            // don't use preemptive if there are no user or proxy credentials to send
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

    public boolean isProxyNtlmAuthentication()
    {
        return proxyNtlmAuthentication;
    }

    public void setProxyNtlmAuthentication(boolean proxyNtlmAuthentication)
    {
        this.proxyNtlmAuthentication = proxyNtlmAuthentication;
    }

    public void connect(EndpointURI endpointURI)
    {
        connectionManager.addConnection(endpointURI);
    }

    public void disconnect(EndpointURI endpointURI)
    {
        connectionManager.removeConnection(endpointURI);
    }


    public HttpMessageReceiver lookupReceiver(Socket socket, RequestLine requestLine) throws NoReceiverForEndpointException
    {
        int port = ((InetSocketAddress) socket.getLocalSocketAddress()).getPort();
        String host = null;
        for (MessageReceiver messageReceiver : receivers.values())
        {
            if (messageReceiver.getEndpointURI().getPort() == port)
            {
                host = messageReceiver.getEndpointURI().getHost();
                break;
            }
        }
        if (host == null)
        {
            String url = requestLine.getUrlWithoutParams();
            throw new NoReceiverForEndpointException(HttpMessages.noReceiverFoundForUrl(url));
        }

        String requestUriWithoutParams = requestLine.getUrlWithoutParams();
        StringBuilder requestUri = new StringBuilder(80);
        if (requestUriWithoutParams.indexOf("://") == -1)
        {
            requestUri.append(getProtocol()).append("://").append(host).append(':').append(port);
            if (!ROOT_PATH.equals(requestUriWithoutParams))
            {
                requestUri.append(requestUriWithoutParams);
            }
        }

        String uriStr = requestUri.toString();
        // first check that there is a receiver on the root address
        if (logger.isTraceEnabled())
        {
            logger.trace("Looking up receiver on connector: " + getName() + " with URI key: "
                         + requestUri.toString());
        }

        HttpMessageReceiver receiver = (HttpMessageReceiver) lookupReceiver(uriStr);

        // If no receiver on the root and there is a request path, look up the
        // received based on the root plus request path
        if (receiver == null && !ROOT_PATH.equals(requestUriWithoutParams))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Secondary lookup of receiver on connector: " + getName()
                             + " with URI key: " + requestUri.toString());
            }

            receiver = (HttpMessageReceiver) findReceiverByStem(getReceivers(), uriStr);

            if (receiver == null && logger.isWarnEnabled())
            {
                logger.warn("No receiver found with secondary lookup on connector: " + getName()
                            + " with URI key: " + requestUri.toString());
                logger.warn("Receivers on connector are: "
                            + MapUtils.toString(getReceivers(), true));
            }
        }
        if (receiver == null)
        {
            throw new NoReceiverForEndpointException(HttpMessages.noReceiverFoundForUrl(requestUriWithoutParams));
        }
        return receiver;
    }

    //Leave for backward compatibility
    @Deprecated
    public HttpMessageReceiver lookupReceiver(Socket socket, HttpRequest request)
    {
        try
        {
            return this.lookupReceiver(socket, request.getRequestLine());
        }
        catch (NoReceiverForEndpointException e)
        {
            logger.debug("No receiver found: " + e.getMessage());
            return null;
        }
    }

    public static MessageReceiver findReceiverByStem(Map<Object, MessageReceiver> receivers, String uriStr)
    {
        int match = 0;
        MessageReceiver receiver = null;
        for (Map.Entry<Object, MessageReceiver> e : receivers.entrySet())
        {
            String key = (String) e.getKey();
            MessageReceiver candidate = e.getValue();
            if (uriStr.startsWith(key) && match < key.length())
            {
                match = key.length();
                receiver = candidate;
            }
        }
        return receiver;
    }

    @Override
    protected ServerSocket getServerSocket(URI uri) throws IOException
    {
        return super.getServerSocket(uri);
    }

    /**
     * @deprecated Use keepAlive property in the outbound endpoint.
     */
    @Override
    @Deprecated
    public boolean isKeepSendSocketOpen()
    {
        return super.isKeepSendSocketOpen();
    }

    /**
     * @deprecated Use keepAlive property in the outbound endpoint.
     */
    @Override
    @Deprecated
    public void setKeepSendSocketOpen(boolean keepSendSocketOpen)
    {
        logger.warn("keepSendSocketOpen attribute is deprecated, use keepAlive in the outbound endpoint instead");
        super.setKeepSendSocketOpen(keepSendSocketOpen);
    }
    
    @Override
    public MessageProcessor createDispatcherMessageProcessor(OutboundEndpoint endpoint) throws MuleException
    {
        if (singleDispatcherPerEndpoint)
        {
            // Avoid lazy initialization of dispatcher in borrow method which would be less performant by
            // creating the dispatcher instance when DispatcherMessageProcessor is created.
            MessageDispatcher dispatcher = dispatcherFactory.create(endpoint);
            applyDispatcherLifecycle(dispatcher);
            endpointDispatchers.put(endpoint, dispatcher);
        }
        return super.createDispatcherMessageProcessor(endpoint);
    }

    @Override
    protected MessageDispatcher borrowDispatcher(OutboundEndpoint endpoint) throws MuleException
    {
        if (singleDispatcherPerEndpoint)
        {
            return endpointDispatchers.get(endpoint);
        }
        else
        {
            return super.borrowDispatcher(endpoint);
        }
    }

    @Override
    protected void returnDispatcher(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        if (singleDispatcherPerEndpoint)
        {
            // Nothing to do because implementation of borrowDispatcher doesn't use dispatcher pool
        }
        else
        {
            super.returnDispatcher(endpoint, dispatcher);
        }
    }

    protected void applyDispatcherLifecycle(MessageDispatcher dispatcher) throws MuleException
    {
        MessageDispatcherUtils.applyLifecycle(dispatcher);
    }
}
