/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.client;

import EDU.oswego.cs.dl.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.security.MuleCredentials;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.*;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;

import java.util.*;

/**
 * <code>MuleClient</code> is a simple interface for Mule clients to send and receive
 * events from a Mule Server. In most Mule applications events are triggered by
 * some external occurrence such as a messge being received on a queue or file being copied
 * to a directory.  The Mule client allows the user to send and receive events programmatically
 * through its Api.
 * <p/>
 * The client defines a UMOEndpointURI which is used to determine how a message is sent of received.
 * The url defines the protocol, the endpointUri destination of the message and optionally the endpoint to
 * use when dispatching the event. For example -
 * <p/>
 * vm://my.object
 * dispatches to a my.object destination using the VM endpoint. There needs to be a global Vm endpoint
 * registered for the message to be sent
 * <p/>
 * jms://jmsProvider/orders.topic
 * dispatches a jms message via the globally registered jmsProvider over a topic destination called orders.topic.
 * <p/>
 * jms://orders.topic
 * Is equivilent to the above except that the endpoint is determined by the protocol, so the first jms endpoint
 * is used.
 * <p/>
 * <p/>
 * Note that there must be a configured MuleManager for this client to work. It will use the one available
 * using <code>MuleManager.getInstance()</code>
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see org.mule.impl.endpoint.MuleEndpointURI
 */
public class MuleClient implements Disposable
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleClient.class);

    /**
     * the local UMOManager instance
     */
    private UMOManager manager;

    //private Set connectors = new HashSet();
    private List dispatchers = new ArrayList();

    //configuration helper for the client
    QuickConfigurationBuilder builder = null;

    private MuleCredentials user;

    /**
     * Creates a default Mule client that will use the default serverEndpoint
     * to connect to a remote server instance.
     *
     * @throws UMOException
     */
    public MuleClient() throws UMOException
    {
        init();
    }

    /**
     * Configures a Mule CLient instance using the the default MuleXmlConfigurationBuilder
     * to parse the config resources
     * @param configResources a config resource location to configure this client with
     * @throws ConfigurationException is there is a MuleManager instance already
     * running in this JVM or if the builder fails to configure the Manager
     */
    public MuleClient(String configResources) throws UMOException
    {
        this(configResources, new MuleXmlConfigurationBuilder());
    }

    /**
     * Configures a new MuleClient and either uses an existing Manager running in this
     * JVM or creates a new empty manager
     * @param user the username to use when connecting to a remote server instance
     * @param password the password for the user
     * @throws UMOException
     */
    public MuleClient(String user, String password) throws UMOException
    {
        init();
        this.user = new MuleCredentials(user, password.toCharArray());
    }

    /**
     * Configures a Mule CLient instance
     * @param configResources a config resource location to configure this client with
     * @param builder the configuration builder to use
     * @throws ConfigurationException is there is a MuleManager instance already
     * running in this JVM or if the builder fails to configure the Manager
     */
    public MuleClient(String configResources, ConfigurationBuilder builder) throws ConfigurationException
    {
        if(MuleManager.isInstanciated()) {
            throw new ConfigurationException(new Message(Messages.MANAGER_IS_ALREADY_CONFIGURED));
        }
        if(builder==null) {
            logger.info("Builder passed in was null, using default builder: " +  MuleXmlConfigurationBuilder.class.getName());
            builder = new MuleXmlConfigurationBuilder();
        }
        manager = builder.configure(configResources);
    }


    /**
     * Configures a Mule CLient instance
     * @param configResources a config resource location to configure this client with
     * @param builder the configuration builder to use
     * @param user the username to use when connecting to a remote server instance
     * @param password the password for the user
     * @throws ConfigurationException is there is a MuleManager instance already
     * running in this JVM or if the builder fails to configure the Manager
     */
    public MuleClient(String configResources, ConfigurationBuilder builder, String user, String password) throws ConfigurationException
    {
        this(configResources, builder);
        this.user = new MuleCredentials(user, password.toCharArray());
    }

    /**
     * Initialises a default MuleManager for use by the client.
     *
     * @throws UMOException
     */
    private void init() throws UMOException
    {
        //if we are creating a server for this client then set client mode
        //this will disable Admin connections by default;
        MuleManager.getConfiguration().setClientMode(
            !MuleManager.isInstanciated());

        manager = MuleManager.getInstance();
        builder = new QuickConfigurationBuilder();
        //If there is no local manager present create a default manager
        if (!manager.isInitialised())
        {
            logger.info("Initialising a new Mule Manager");
            ((MuleManager) manager).start();
        }
    }

    /**
     * Dispatches an event asynchronously to a endpointUri via a mule server. the Url determines where to dispathc
     * the event to, this can be in the form of
     *
     * @param url               the Mule url used to determine the destination and transport of the message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.  In the case of Jms you could
     *                          set the JMSReplyTo property in these properties.
     * @throws org.mule.umo.UMOException
     */
    public void dispatch(String url, Object payload, Map messageProperties) throws UMOException
    {
        UMOEndpointURI muleEndpoint = new MuleEndpointURI(url);
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOEvent event = getEvent(message, muleEndpoint, false);
        try
        {
            event.getSession().dispatchEvent(event);
        } catch (UMOException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new DispatchException(new Message("client", 1), event.getMessage(), event.getEndpoint(), e);
        }
    }

    /**
     * sends an event synchronously to a components
     *
     * @param component         the name of the Mule components to send to
     * @param transformers      a comma separated list of transformers to apply to the result message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.
     *                          as null
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or transfromers cannot be found
     */
    public UMOMessage sendDirect(String component, String transformers, Object payload, Map messageProperties) throws UMOException
    {
        boolean compregistered = getManager().getModel().isComponentRegistered(component);
//        if(!compregistered && forwardDirectRequests) {
//           logger.info("Component '" + components + "' not found in local server, forwarding remote request to " + serverEndpoint);
//           sendRemote(components, transformers, payload, messageProperties);
//        } else
        if (!compregistered)
        {
            throw new MessagingException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Component '" + component + "'"), payload, null);
        }
        UMOTransformer trans = null;
        if (transformers != null)
        {
            trans = MuleObjectHelper.getTransformer(transformers, ",");
        }

        if (!MuleManager.getConfiguration().isSynchronous())
        {
            logger.warn("The mule manager is running synchronously, a null message payload will be returned");
        }
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOSession session = getManager().getModel().getComponentSession(component);
        UMOEndpoint endpoint = getDefaultClientEndpoint(session.getComponent().getDescriptor(), payload);
        UMOEvent event = new MuleEvent(message, endpoint, session, true);

        logger.debug("MuleClient sending event direct to: " + component + ". Event is: " + event);
        UMOMessage result = event.getComponent().sendEvent(event);

        logger.debug("Result of MuleClient sendDirect is: " + (result == null ? "null" : result.getPayload()));
        if (result != null && trans != null)
        {
            return new MuleMessage(trans.transform(result.getPayload()), null);
        } else
        {
            return result;
        }
    }

    /**
     * dispatches an event asynchronously to the components
     *
     * @param component         the name of the Mule components to dispatch to
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.
     *                          as null
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or transfromers cannot be found
     */
    public void dispatchDirect(String component, Object payload, Map messageProperties) throws UMOException
    {
        boolean compregistered = getManager().getModel().isComponentRegistered(component);

//        if(!compregistered && forwardDirectRequests) {
//           logger.info("Component '" + components + "' not found in local server, forwarding remote request to " + serverEndpoint);
//           dispatchRemote(components, payload, messageProperties);
//        } else
        if (!compregistered)
        {
            throw new MessagingException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Component '" + component + "'"), payload, null);
        }
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOSession session = getManager().getModel().getComponentSession(component);
        UMOEndpoint endpoint = getDefaultClientEndpoint(session.getComponent().getDescriptor(), payload);
        UMOEvent event = new MuleEvent(message, endpoint, session, true);

        logger.debug("MuleClient dispatching event direct to: " + component + ". Event is: " + event);
        event.getComponent().dispatchEvent(event);
    }

    /**
     * sends an event request to a Url, making the result of
     * the event trigger available as a Future result that can be accessed later by client code.
     *
     * @param url               the url to make a request on
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.
     *                          as null
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or transfromers cannot be found
     */
    public FutureMessageResult sendAsync(final String url, final Object payload, final Map messageProperties) throws UMOException
    {
        return sendAsync(url, payload, messageProperties, 0);
    }

    /**
     * sends an event request to a Url, making the result of
     * the event trigger available as a Future result that can be accessed later by client code.
     *
     * @param url               the url to make a request on
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.
     *                          as null
     * @param timeout           how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or transfromers cannot be found
     */
    public FutureMessageResult sendAsync(final String url, final Object payload, final Map messageProperties, final int timeout) throws UMOException
    {
        FutureMessageResult result = new FutureMessageResult();

        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return send(url, payload, messageProperties, timeout);
            }
        };

        result.execute(callable);
        return result;
    }

    /**
     * sends an event to a components on a local Mule instance, while making the result of
     * the event trigger available as a Future result that can be accessed later by client code.
     * If forwardDirectRequests flag s set and the components is not found on the local Mule instance
     * it will forward to a remote server.
     * Users can endpoint a url to a remote Mule server in the constructor of a Mule client,  by
     * default the default Mule server url tcp://localhost:60504 is used.
     *
     * @param component         the name of the Mule components to send to
     * @param transformers      a comma separated list of transformers to apply to the result message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.
     *                          as null
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components or transfromers cannot be found
     */
    public FutureMessageResult sendDirectAsync(final String component, String transformers, final Object payload, final Map messageProperties) throws UMOException
    {
        UMOTransformer trans = null;
        FutureMessageResult result = null;
        if (transformers != null)
        {
            trans = MuleObjectHelper.getTransformer(transformers, ",");
            result = new FutureMessageResult(trans);
        } else
        {
            result = new FutureMessageResult();
        }

        Callable callable = new Callable()
        {
            public Object call() throws Exception
            {
                return sendDirect(component, null, payload, messageProperties);
            }
        };

        result.execute(callable);
        return result;
    }

    /**
     * Sends an event synchronously to a endpointUri via a mule server and a resulting message is returned.
     *
     * @param url               the Mule url used to determine the destination and transport of the message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.  In the case of Jms you could
     *                          set the JMSReplyTo property in these properties.
     * @return A return message, this could be null if the the components invoked explicitly sets a return
     *         as null
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage send(String url, Object payload, Map messageProperties) throws UMOException
    {
        return send(url, payload, messageProperties, 0);
    }

    /**
     * Sends an event synchronously to a endpointUri via a mule server and a resulting message is returned.
     *
     * @param url               the Mule url used to determine the destination and transport of the message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.  In the case of Jms you could
     *                          set the JMSReplyTo property in these properties.
     * @param timeout           The time in milliseconds the the call should block waiting for a response
     * @return A return message, this could be null if the the components invoked explicitly sets a return
     *         as null
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage send(String url, Object payload, Map messageProperties, int timeout) throws UMOException
    {
        UMOEndpointURI muleEndpoint = new MuleEndpointURI(url);
        if (messageProperties == null) messageProperties = new HashMap();
        if(messageProperties.get(MuleProperties.MULE_SYNCHRONOUS_RECEIVE_PROPERTY)==null) {
            messageProperties.put(MuleProperties.MULE_SYNCHRONOUS_RECEIVE_PROPERTY, "true");
        }
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOEvent event = getEvent(message, muleEndpoint, true);
        event.setTimeout(timeout);
        UMOMessage result = null;
        try
        {
            result = event.getSession().sendEvent(event);
        } catch (UMOException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new DispatchException(new Message("client", 1), event.getMessage(), event.getEndpoint(), e);
        }
        return result;
    }

    /**
     * Will receive an event from an endpointUri determined by the url
     *
     * @param url     the Mule url used to determine the destination and transport of the message
     * @param timeout how long to block waiting to receive the event, if set to 0 the receive will
     *                not wait at all and if set to -1 the receive will wait forever
     * @return the message received or null if no message was received
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage receive(String url, long timeout) throws UMOException
    {
        MuleEndpointURI muleEndpoint = new MuleEndpointURI(url);

        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(muleEndpoint, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        try
        {
            UMOMessage message = endpoint.getConnector().getDispatcher(muleEndpoint.getAddress()).receive(muleEndpoint, timeout);
            return message;
        } catch (Exception e)
        {
            throw new ReceiveException(muleEndpoint, timeout, e);
        }
    }

    /**
     * Will receive an event from an endpointUri determined by the url
     *
     * @param url          the Mule url used to determine the destination and transport of the message
     * @param transformers A comma separated list of transformers used to apply to the result message
     * @param timeout      how long to block waiting to receive the event, if set to 0 the receive will
     *                     not wait at all and if set to -1 the receive will wait forever
     * @return the message received or null if no message was received
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage receive(String url, String transformers, long timeout) throws UMOException
    {
        return receive(url, MuleObjectHelper.getTransformer(transformers, ","), timeout);
    }

    /**
     * Will receive an event from an endpointUri determined by the url
     *
     * @param url         the Mule url used to determine the destination and transport of the message
     * @param transformer A transformer used to apply to the result message
     * @param timeout     how long to block waiting to receive the event, if set to 0 the receive will
     *                    not wait at all and if set to -1 the receive will wait forever
     * @return the message received or null if no message was received
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage receive(String url, UMOTransformer transformer, long timeout) throws UMOException
    {

        UMOMessage message = receive(url, timeout);
        if (message != null && transformer != null)
        {
            return new MuleMessage(transformer.transform(message.getPayload()), null);
        } else
        {
            return message;
        }
    }

    /**
     * Packages a mule event for the current request
     *
     * @param message     the event payload
     * @param uri         the destination endpointUri
     * @param synchronous whether the event will be synchronously processed
     * @return the UMOEvent
     * @throws UMOException
     */
    protected UMOEvent getEvent(UMOMessage message, UMOEndpointURI uri, boolean synchronous) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(uri, UMOEndpoint.ENDPOINT_TYPE_SENDER);

        if (!endpoint.getConnector().isStarted() && manager.isStarted())
        {
            endpoint.getConnector().start();
        }

        try
        {
            MuleSession session = new MuleSession();
            if(user!=null) {
                message.setProperty(MuleProperties.MULE_USER_PROPERTY, "Plain " + user.getToken());
            }
            MuleEvent event = new MuleEvent(message, endpoint, session, synchronous);

            return event;
        } catch (Exception e)
        {
            throw new DispatchException(new Message(Messages.FAILED_TO_CREATE_X, "Client event"), message, endpoint, e);
        }
    }

    protected UMOEndpoint getDefaultClientEndpoint(UMODescriptor descriptor, Object payload) throws UMOException
    {
        //as we are bypassing the message transport layer we need to check that
        UMOEndpoint endpoint = descriptor.getInboundEndpoint();
        if (endpoint != null)
        {
            if (endpoint.getTransformer() != null)
            {
                if (endpoint.getTransformer().isSourceTypeSupported(payload.getClass()))
                {
                    return endpoint;
                } else
                {
                    endpoint = new MuleEndpoint(endpoint);
                    endpoint.setTransformer(null);
                    return endpoint;
                }
            } else
            {
                return endpoint;
            }
        } else
        {
            UMOConnector connector = null;
            UMOEndpointURI defaultEndpointUri = new MuleEndpointURI("vm://localhost/mule.client");
            connector = ConnectorFactory.createConnector(defaultEndpointUri);
            connector.initialise();
            manager.registerConnector(connector);
            connector.start();
            endpoint = new MuleEndpoint("muleClientProvider",
                    defaultEndpointUri, connector, null,
                    UMOEndpoint.ENDPOINT_TYPE_RECEIVER, 0, null);
        }
        if (endpoint != null)
        {
            manager.registerEndpoint(endpoint);
        }
        return endpoint;
    }


    /**
     * Sends an event synchronously to a endpointUri via a mule server
     * without waiting for the result.
     *
     * @param url               the Mule url used to determine the destination and transport of the message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In the case of Jms you could
     *                          set the JMSReplyTo property in these properties.
     * @throws org.mule.umo.UMOException
     */
    public void sendNoReceive(String url, Object payload, Map messageProperties) throws UMOException
    {
        UMOEndpointURI muleEndpoint = new MuleEndpointURI(url);
        if (messageProperties == null) messageProperties = new HashMap();
        messageProperties.put(MuleProperties.MULE_SYNCHRONOUS_RECEIVE_PROPERTY, "false");
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOEvent event = getEvent(message, muleEndpoint, true);
        try
        {
            event.getSession().sendEvent(event);
        } catch (UMOException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new DispatchException(new Message("client", 1), event.getMessage(), event.getEndpoint(), e);
        }
    }

    /**
     * Overriding methods may want to return a custom manager here
     *
     * @return the UMOManager to use
     */
    public UMOManager getManager()
    {
        return MuleManager.getInstance();
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     *
     * @param component        any java object, Mule will it's endpointUri discovery to determine
     *                         which event to invoke based on the evnet payload type
     * @param name             The identifying name of the components.  This can be used to later unregister it
     * @param listenerEndpoint The url endpointUri to listen to
     * @throws UMOException
     */
    public void registerComponent(Object component, String name, MuleEndpointURI listenerEndpoint) throws UMOException
    {
        builder.registerComponentInstance(component, name, listenerEndpoint, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events on the
     * given urls. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     *
     * @param component        any java object, Mule will it's endpointUri discovery to determine
     *                         which event to invoke based on the evnet payload type
     * @param name             The identifying name of the components.  This can be used to later unregister it
     * @param listenerEndpoint The url endpointUri to listen to
     * @param sendEndpoint     The url endpointUri to dispatch to
     * @throws UMOException
     */
    public void registerComponent(Object component, String name, MuleEndpointURI listenerEndpoint, MuleEndpointURI sendEndpoint) throws UMOException
    {
        builder.registerComponentInstance(component, name, listenerEndpoint, sendEndpoint);
    }

    /**
     * Registers a user configured MuleDescriptor of a components to the server.
     * If users want to register object instances with the server rather than
     * class names that get created at runtime or reference to objects in the
     * container, the user must call the descriptors setImplementationInstance() method -
     * <code>
     * MyBean implementation = new MyBean();
     * descriptor.setImplementationInstance(implementation);
     * </code>
     * Calling this method is equivilent to calling UMOModel.registerComponent(..)
     *
     * @param descriptor the componet descriptor to register
     * @throws UMOException the descriptor is invalid or cannot be initialised or started
     * @see org.mule.umo.model.UMOModel
     */
    public void registerComponent(UMODescriptor descriptor) throws UMOException
    {
        builder.registerComponent(descriptor);
    }

    /**
     * Unregisters a previously register components.  This will also unregister any listeners
     * for the components
     * Calling this method is equivilent to calling UMOModel.unregisterComponent(..)
     *
     * @param name the name of the componet to unregister
     * @throws UMOException if unregistering the components fails, i.e.  The underlying
     *                      transport fails to unregister a listener.  If the components does not exist, this
     *                      method should not throw an exception.
     * @see org.mule.umo.model.UMOModel
     */
    public void unregisterComponent(String name) throws UMOException
    {
        builder.unregisterComponent(name);
    }

    public RemoteDispatcher getRemoteDispatcher(String serverEndpoint) throws MalformedEndpointException
    {
        RemoteDispatcher rd = new RemoteDispatcher(this, serverEndpoint);
        dispatchers.add(rd);
        return rd;
    }

    public RemoteDispatcher getRemoteDispatcher(String serverEndpoint, String user, String password) throws MalformedEndpointException
    {
        RemoteDispatcher rd = new RemoteDispatcher(this, serverEndpoint, user, password);
        dispatchers.add(rd);
        return rd;
    }

    /**
     * Will dispose the MUleManager instance *IF* a new instance was created for
     * this client.  Otherwise this method only cleans up resources no longer needed
     */
    public void dispose()
    {
        synchronized (dispatchers)
        {
            for (Iterator iterator = dispatchers.iterator(); iterator.hasNext();)
            {
                RemoteDispatcher remoteDispatcher = (RemoteDispatcher) iterator.next();
                remoteDispatcher.dispose();
                dispatchers.remove(remoteDispatcher);
                remoteDispatcher = null;
            }
        }
        //Dispose the manager only if the manager was created for this client
        if(MuleManager.getConfiguration().isClientMode()) {
            manager.dispose();
        }
    }

    public void setProperty(Object key, Object value) {
        manager.setProperty(key, value);
    }

    public Object getProperty(Object key) {
        return manager.getProperty(key);
    }
}
