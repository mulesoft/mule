/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.client;

import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.MuleSessionHandler;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.security.MuleCredentials;
import org.mule.providers.AbstractConnector;
import org.mule.providers.service.TransportFactory;
import org.mule.transformers.wire.SerializationWireFormat;
import org.mule.transformers.wire.WireFormat;
import org.mule.umo.FutureMessageResult;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.security.UMOCredentials;
import org.mule.util.MuleObjectHelper;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>RemoteDispatcher</code> is used to make and receive requests to a remote
 * Mule instance. It is used to proxy requests to Mule using the Server URL as the
 * transport channel.
 */

public class RemoteDispatcher implements Disposable
{

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(RemoteDispatcher.class);

    /**
     * dispatch destination
     */
    private UMOEndpoint serverEndpoint;
    private UMOCredentials credentials = null;

    /**
     * an ExecutorService for async messages (optional)
     */
    private Executor asyncExecutor;

    /**
     * calls made to a remote server are serialised using a wireformat
     */
    private WireFormat wireFormat;

    protected RemoteDispatcher(String endpoint, UMOCredentials credentials) throws UMOException
    {
        this(endpoint);
        this.credentials = credentials;
    }

    protected RemoteDispatcher(String endpoint) throws UMOException
    {
        serverEndpoint = new MuleEndpoint(endpoint, true);
        wireFormat = new SerializationWireFormat();
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
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public void dispatchToRemoteComponent(String component, Object payload, Map messageProperties)
        throws UMOException
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
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public UMOMessage sendToRemoteComponent(String component, Object payload, Map messageProperties)
        throws UMOException
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
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public FutureMessageResult sendAsyncToRemoteComponent(final String component,
                                                          String transformers,
                                                          final Object payload,
                                                          final Map messageProperties) throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return doToRemoteComponent(component, payload, messageProperties, true);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);

        if (asyncExecutor != null)
        {
            result.setExecutor(asyncExecutor);
        }

        if (transformers != null)
        {
            result.setTransformer(MuleObjectHelper.getTransformer(transformers, ","));
        }

        result.execute();
        return result;
    }

    public UMOMessage sendRemote(String endpoint, Object payload, Map messageProperties, int timeout)
        throws UMOException
    {
        return doToRemote(endpoint, payload, messageProperties, true, timeout);
    }

    public UMOMessage sendRemote(String endpoint, Object payload, Map messageProperties) throws UMOException
    {
        return doToRemote(endpoint, payload, messageProperties, true, RegistryContext.getConfiguration()
            .getDefaultSynchronousEventTimeout());
    }

    public void dispatchRemote(String endpoint, Object payload, Map messageProperties) throws UMOException
    {
        doToRemote(endpoint, payload, messageProperties, false, -1);
    }

    public FutureMessageResult sendAsyncRemote(final String endpoint,
                                               final Object payload,
                                               final Map messageProperties) throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return doToRemote(endpoint, payload, messageProperties, true, -1);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);

        if (asyncExecutor != null)
        {
            result.setExecutor(asyncExecutor);
        }

        result.execute();
        return result;
    }

    public UMOMessage receiveRemote(String endpoint, int timeout) throws UMOException
    {
        AdminNotification action = new AdminNotification(null, AdminNotification.ACTION_RECEIVE, endpoint);
        action.setProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
        action.setProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, new Long(timeout));
        UMOMessage result = dispatchAction(action, true, timeout);
        return result;
    }

    public FutureMessageResult asyncReceiveRemote(final String endpoint, final int timeout)
        throws UMOException
    {
        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return receiveRemote(endpoint, timeout);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);

        if (asyncExecutor != null)
        {
            result.setExecutor(asyncExecutor);
        }

        result.execute();
        return result;
    }

    protected UMOMessage doToRemoteComponent(String component,
                                             Object payload,
                                             Map messageProperties,
                                             boolean synchronous) throws UMOException
    {
        UMOMessage message = new MuleMessage(payload, messageProperties);
        message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, synchronous);
        setCredentials(message);
        AdminNotification action = new AdminNotification(message, AdminNotification.ACTION_INVOKE,
            "mule://" + component);
        UMOMessage result = dispatchAction(action, synchronous, RegistryContext.getConfiguration()
            .getDefaultSynchronousEventTimeout());
        return result;
    }

    protected UMOMessage doToRemote(String endpoint,
                                    Object payload,
                                    Map messageProperties,
                                    boolean synchronous,
                                    int timeout) throws UMOException
    {
        UMOMessage message = new MuleMessage(payload, messageProperties);
        message.setProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, String.valueOf(synchronous));
        setCredentials(message);
        AdminNotification action = new AdminNotification(message, (synchronous
                        ? AdminNotification.ACTION_SEND : AdminNotification.ACTION_DISPATCH), endpoint);

        UMOMessage result = dispatchAction(action, synchronous, timeout);
        return result;
    }

    protected UMOMessage dispatchAction(AdminNotification action, boolean synchronous, int timeout)
        throws UMOException
    {

        UMOEndpoint endpoint = TransportFactory.createEndpoint(serverEndpoint.getEndpointURI(),
            UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.setRemoteSync(synchronous);
        updateContext(new MuleMessage(action), endpoint, synchronous);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wireFormat.write(out, action, serverEndpoint.getEncoding());
        byte[] payload = out.toByteArray();

        UMOMessage message = action.getMessage();

        if (message == null)
        {
            message = new MuleMessage(payload);
        }
        else
        {
            message = new MuleMessage(payload, message);
        }

        message.addProperties(action.getProperties());
        MuleSession session = new MuleSession(message,
            ((AbstractConnector)endpoint.getConnector()).getSessionHandler());

        UMOEvent event = new MuleEvent(message, endpoint, session, true);
        event.setTimeout(timeout);
        if (logger.isDebugEnabled())
        {
            logger.debug("MuleClient sending remote call to: " + action.getResourceIdentifier() + ". At "
                         + serverEndpoint.toString() + " . Event is: " + event);
        }

        UMOMessage result;

        try
        {
            if (synchronous)
            {
                result = endpoint.send(event);
            }
            else
            {
                endpoint.dispatch(event);
                return null;
            }

            if (result != null)
            {
                if (result.getPayload() != null)
                {
                    Object response;
                    if (result.getPayload() instanceof InputStream)
                    {
                        response = wireFormat.read((InputStream)result.getPayload());
                    }
                    else
                    {
                        ByteArrayInputStream in = new ByteArrayInputStream(result.getPayloadAsBytes());
                        response = wireFormat.read(in);
                    }

                    if (response instanceof AdminNotification)
                    {
                        response = ((AdminNotification)response).getMessage();
                    }
                    return (UMOMessage)response;
                }
            }
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
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

    protected void setCredentials(UMOMessage message)
    {
        if (credentials != null)
        {
            message.setProperty(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader(
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

    protected void updateContext(UMOMessage message, UMOEndpoint endpoint, boolean synchronous)
        throws UMOException
    {

        RequestContext.setEvent(new MuleEvent(message, endpoint, new MuleSession(message,
            new MuleSessionHandler()), synchronous));
    }
}
