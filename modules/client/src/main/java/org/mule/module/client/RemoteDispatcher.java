/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.FutureMessageResult;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.security.Credentials;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.api.transport.DispatchException;
import org.mule.client.DefaultLocalMuleClient.MuleClientFlowConstruct;
import org.mule.module.client.i18n.ClientMessages;
import org.mule.module.client.remoting.RemoteDispatcherException;
import org.mule.module.client.remoting.ServerHandshake;
import org.mule.module.client.remoting.UnsupportedWireFormatException;
import org.mule.module.client.remoting.notification.RemoteDispatcherNotification;
import org.mule.security.MuleCredentials;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>RemoteDispatcher</code> is used to make and receive requests to a remote
 * Mule instance. It is used to proxy requests to Mule using the Server URL as the
 * transport channel.
 *
 * <b>Deprecated as of 3.6.0</b>
 */
@Deprecated
public class RemoteDispatcher implements Disposable
{

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(RemoteDispatcher.class);

    /**
     * dispatch destination
     */
    private OutboundEndpoint asyncServerEndpoint;
    private OutboundEndpoint syncServerEndpoint;
    private Credentials credentials = null;
    private MuleContext muleContext;
    private ObjectSerializer serializer;

    /**
     * an ExecutorService for async messages (optional)
     */
    private Executor asyncExecutor;

    /**
     * calls made to a remote server are serialised using a wireformat
     */
    private WireFormat wireFormat;

    protected RemoteDispatcher(String endpoint, Credentials credentials, MuleContext muleContext) throws MuleException
    {
        this(endpoint, muleContext);
        this.credentials = credentials;
    }

    protected RemoteDispatcher(String endpoint, MuleContext muleContext) throws MuleException
    {
        this.muleContext = muleContext;
        serializer = muleContext.getObjectSerializer();
        EndpointFactory endpointFactory = muleContext.getEndpointFactory();
        asyncServerEndpoint = endpointFactory.getOutboundEndpoint(endpoint);

        EndpointBuilder endpointBuilder = endpointFactory.getEndpointBuilder(endpoint);
        endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        syncServerEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
            endpointBuilder);

        wireFormat = requestWireFormat();
    }

    protected WireFormat requestWireFormat() throws MuleException
    {
        MuleMessage msg = new DefaultMuleMessage(ServerHandshake.SERVER_HANDSHAKE_PROPERTY, muleContext);
        MuleMessage result = null;

        MuleEvent resultEvent = syncServerEndpoint.process(new DefaultMuleEvent(msg,
            MessageExchangePattern.REQUEST_RESPONSE, new MuleClientFlowConstruct(muleContext)));
        if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
        {
            result = resultEvent.getMessage();
        }

        if(result==null)
        {
            throw new RemoteDispatcherException(ClientMessages.failedToDispatchActionNoResponseFromServer("request wire format", 5000));
        }

        ServerHandshake handshake;
        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(result.getPayloadAsBytes());
            handshake = serializer.deserialize(in);
        }
        catch (Exception e)
        {
            throw new RemoteDispatcherException(ClientMessages.failedToDeserializeHandshakeFromServer(), e);
        }

        try
        {
            WireFormat wf = (WireFormat)ClassUtils.instanciateClass(handshake.getWireFormatClass(),
                    ClassUtils.NO_ARGS, getClass());

            wf.setMuleContext(muleContext);
            return wf;
        }
        catch (Exception e)
        {
            throw new UnsupportedWireFormatException(handshake.getWireFormatClass(), e);
        }
    }

    protected void setExecutor(Executor e)
    {
        this.asyncExecutor = e;
    }

    /**
     * Dispatcher an event asynchronously to a components on a remote Mule instance.
     * Users can endpoint a url to a remote Mule server in the constructor of a Mule
     * client, by default the default Mule server url tcp://localhost:60504 is used.
     *
     * @param component the name of the Mule components to dispatch to
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public void dispatchToRemoteComponent(String component, Object payload, Map messageProperties)
        throws MuleException
    {
        doToRemoteComponent(component, payload, messageProperties, false);
    }

    /**
     * sends an event synchronously to a components on a remote Mule instance. Users
     * can endpoint a url to a remote Mule server in the constructor of a Mule
     * client, by default the default Mule server url tcp://localhost:60504 is used.
     *
     * @param component the name of the Mule components to send to
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public MuleMessage sendToRemoteComponent(String component, Object payload, Map messageProperties)
        throws MuleException
    {
        return doToRemoteComponent(component, payload, messageProperties, true);
    }

    /**
     * sends an event to a components on a remote Mule instance, while making the
     * result of the event trigger available as a Future result that can be accessed
     * later by client code. Users can endpoint a url to a remote Mule server in the
     * constructor of a Mule client, by default the default Mule server url
     * tcp://localhost:60504 is used.
     *
     * @param component the name of the Mule components to send to
     * @param transformers a comma separated list of transformers to apply to the
     *            result message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public FutureMessageResult sendAsyncToRemoteComponent(final String component,
                                                          String transformers,
                                                          final Object payload,
                                                          final Map messageProperties) throws MuleException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return doToRemoteComponent(component, payload, messageProperties, true);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable, muleContext);

        if (asyncExecutor != null)
        {
            result.setExecutor(asyncExecutor);
        }

        if (transformers != null)
        {
            result.setTransformers(TransformerUtils.getTransformers(transformers, muleContext));
        }

        result.execute();
        return result;
    }

    public MuleMessage sendRemote(String endpoint, Object payload, Map messageProperties, int timeout)
        throws MuleException
    {
        return doToRemote(endpoint, payload, messageProperties, true, timeout);
    }

    public MuleMessage sendRemote(String endpoint, Object payload, Map messageProperties) throws MuleException
    {
        return doToRemote(endpoint, payload, messageProperties, true,
            muleContext.getConfiguration().getDefaultResponseTimeout());
    }

    public void dispatchRemote(String endpoint, Object payload, Map messageProperties) throws MuleException
    {
        doToRemote(endpoint, payload, messageProperties, false, -1);
    }

    public FutureMessageResult sendAsyncRemote(final String endpoint,
                                               final Object payload,
                                               final Map messageProperties) throws MuleException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return doToRemote(endpoint, payload, messageProperties, true, -1);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable, muleContext);

        if (asyncExecutor != null)
        {
            result.setExecutor(asyncExecutor);
        }

        result.execute();
        return result;
    }

    public MuleMessage receiveRemote(String endpoint, int timeout) throws MuleException
    {
        RemoteDispatcherNotification action = new RemoteDispatcherNotification(null, RemoteDispatcherNotification.ACTION_RECEIVE, endpoint);
        action.setProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
        if (timeout != MuleEvent.TIMEOUT_NOT_SET_VALUE)
        {
            action.setProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, new Long(timeout));
        }
        return dispatchAction(action, true, timeout);
    }

    public FutureMessageResult asyncReceiveRemote(final String endpoint, final int timeout)
        throws MuleException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return receiveRemote(endpoint, timeout);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable, muleContext);

        if (asyncExecutor != null)
        {
            result.setExecutor(asyncExecutor);
        }

        result.execute();
        return result;
    }

    protected MuleMessage doToRemoteComponent(String component,
                                             Object payload,
                                             Map messageProperties,
                                             boolean synchronous) throws MuleException
    {
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);
        message.setOutboundProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, synchronous);
        setCredentials(message);
        RemoteDispatcherNotification action = new RemoteDispatcherNotification(message, RemoteDispatcherNotification.ACTION_INVOKE,
            "mule://" + component);
        return dispatchAction(action, synchronous,
            muleContext.getConfiguration().getDefaultResponseTimeout());
    }

    protected MuleMessage doToRemote(String endpoint,
                                    Object payload,
                                    Map messageProperties,
                                    boolean synchronous,
                                    int timeout) throws MuleException
    {
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);
        message.setOutboundProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, String.valueOf(synchronous));
        setCredentials(message);
        RemoteDispatcherNotification action = new RemoteDispatcherNotification(message, (synchronous
                        ? RemoteDispatcherNotification.ACTION_SEND : RemoteDispatcherNotification.ACTION_DISPATCH), endpoint);

        return dispatchAction(action, synchronous, timeout);
    }

    protected MuleMessage dispatchAction(RemoteDispatcherNotification action, boolean synchronous, int timeout)
        throws MuleException
    {
        OutboundEndpoint serverEndpoint;
        if (synchronous)
        {
            serverEndpoint = syncServerEndpoint;
        }
        else
        {
            serverEndpoint = asyncServerEndpoint;
        }
        MuleMessage serializeMessage = new DefaultMuleMessage(action, muleContext);

        updateContext(serializeMessage, serverEndpoint, synchronous);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wireFormat.write(out, serializeMessage, serverEndpoint.getEncoding());
        byte[] payload = out.toByteArray();

        MuleMessage message = action.getMessage();

        if (message == null)
        {
            message = new DefaultMuleMessage(payload, muleContext);
        }
        else
        {
            message = new DefaultMuleMessage(payload, message, muleContext);
        }

        message.addProperties(action.getProperties());

        MuleEvent event = new DefaultMuleEvent(message, serverEndpoint.getExchangePattern(),(FlowConstruct) null);
        event.setTimeout(timeout);
        if (logger.isDebugEnabled())
        {
            logger.debug("MuleClient sending remote call to: " + action.getResourceIdentifier() + ". At "
                         + serverEndpoint.toString() + " . Event is: " + event);
        }


        MuleMessage result = null;

        try
        {
            MuleEvent resultEvent = serverEndpoint.process(event);
            if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
            {
                result = resultEvent.getMessage();
            }

            if (result != null && result.getPayload() != null)
            {
                if (result.getPayload() instanceof NullPayload)
                {
                    return null;
                }

                Object response;
                if (result.getPayload() instanceof InputStream)
                {
                    byte[] b = IOUtils.toByteArray((InputStream)result.getPayload());
                    if(b.length==0) return null;
                    ByteArrayInputStream in = new ByteArrayInputStream(b);
                    response = wireFormat.read(in);
                }
                else
                {
                    ByteArrayInputStream in = new ByteArrayInputStream(result.getPayloadAsBytes());
                    response = wireFormat.read(in);
                }

                if (response instanceof RemoteDispatcherNotification)
                {
                    response = ((RemoteDispatcherNotification)response).getMessage();
                }
                return (MuleMessage)response;
            }
        }
        catch (Exception e)
        {
            throw new DispatchException(event, serverEndpoint, e);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Result of MuleClient remote call is: "
                         + (result == null ? "null" : result.getPayload()));
        }

        return result;
    }

    public void dispose()
    {
        // nothing to do here
    }

    protected void setCredentials(MuleMessage message)
    {
        if (credentials != null)
        {
            message.setOutboundProperty(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader(
                    credentials.getUsername(), credentials.getPassword()));
        }
    }

    public WireFormat getWireFormat()
    {
        return wireFormat;
    }

    public void setWireFormat(WireFormat wireFormat)
    {
        this.wireFormat = wireFormat;
    }

    protected void updateContext(MuleMessage message, ImmutableEndpoint endpoint, boolean synchronous)
        throws MuleException
    {
        RequestContext.setEvent(new DefaultMuleEvent(message, endpoint.getExchangePattern(),
            (FlowConstruct) null));
    }
}
