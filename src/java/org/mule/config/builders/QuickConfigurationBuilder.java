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
package org.mule.config.builders;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.ReaderResource;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.ModelFactory;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.MuleObjectHelper;

import java.util.Map;

/**
 * <code>QuickConfigurationBuilder</code> is a configuration helper that can
 * be used by clients, configuration scripts or test cases to quickly configure
 * a manager
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QuickConfigurationBuilder implements ConfigurationBuilder
{
    private static final String MODEL_NOT_SET = "not set";

    private UMOManager manager;

    /**
     * Constructs a default builder
     */
    public QuickConfigurationBuilder()
    {
        manager = MuleManager.getInstance();
    }

    /**
     * Will construct a new Quick Config builder with the option of disposing of
     * the current Manager if one exists
     * 
     * @param disposeCurrent true to dispose the current manager
     */
    public QuickConfigurationBuilder(boolean disposeCurrent)
    {
        if (disposeCurrent) {
            disposeCurrent();
        }

        manager = MuleManager.getInstance();
    }

    /**
     * Disposes the current MuleManager if there is one.
     */
    public void disposeCurrent()
    {
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }
    }

    public void setModel(String model) throws UMOException {
        manager.setModel(ModelFactory.createModel(model));
    }
    /**
     * Configures a started manager. This method will throw
     * InitialisationException if the current manager is already started
     * 
     * @param synchronous whether to start the manager in synchronous mode
     * @param serverUrl the url used to receive client requests, or null if the
     *            server listening components should not be set up
     * @return the configured manager
     * @throws UMOException if the manager is already started or it fails to
     *             start
     */
    public UMOManager createStartedManager(boolean synchronous, String serverUrl, String modeltype) throws UMOException
    {
        if (manager.isStarted()) {
            throw new InitialisationException(new Message(Messages.MANAGER_ALREADY_STARTED), this);
        }
        if (serverUrl == null) {
            serverUrl = "";
        }
        MuleManager.getConfiguration().setServerUrl(serverUrl);
        MuleManager.getConfiguration().setSynchronous(synchronous);
        if(!MODEL_NOT_SET.equals(modeltype)) {
            manager.setModel(ModelFactory.createModel(modeltype));
        }
        manager.start();
        return manager;
    }

    /**
     * Configures a started manager. This method will throw
     * InitialisationException if the current manager is already started
     *
     * @param synchronous whether to start the manager in synchronous mode
     * @param serverUrl the url used to receive client requests, or null if the
     *            server listening components should not be set up
     * @return the configured manager
     * @throws UMOException if the manager is already started or it fails to
     *             start
     */
    public UMOManager createStartedManager(boolean synchronous, String serverUrl) throws UMOException
    {
        return createStartedManager(synchronous, serverUrl, MODEL_NOT_SET);
    }
    /**
     * Configures a started manager. This method will throw
     * InitialisationException if the current manager is already started
     * 
     * @param synchronous whether to start the manager in synchronous mode
     * @param serverUrl the url used to receive client requests, or null if the
     *            server listening components should not be set up
     * @param serverConnector The server connector to use for the serverUrl
     * @return the configured manager
     * @throws UMOException if the manager is already started or it fails to
     *             start
     */
    public UMOManager createStartedManager(boolean synchronous, String serverUrl, UMOConnector serverConnector)
            throws UMOException
    {
        if (serverConnector != null) {
            manager.registerConnector(serverConnector);
        }
        // set the connector on th endpointUri
        int param = serverUrl.indexOf("?");
        if (param == -1) {
            serverUrl += "?";
        } else {
            serverUrl += "&";
        }
        serverUrl += UMOEndpointURI.PROPERTY_CREATE_CONNECTOR + "=" + serverConnector.getName();

        return createStartedManager(synchronous, serverUrl);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on
     * the given url. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     * 
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload
     *            type
     * @param name The identifying name of the components. This can be used to
     *            later unregister it
     * @param listenerEndpointUri The url endpointUri to listen to
     * @throws org.mule.umo.UMOException
     */
    public UMODescriptor registerComponentInstance(Object component, String name, UMOEndpointURI listenerEndpointUri)
            throws UMOException
    {
        return registerComponentInstance(component, name, listenerEndpointUri, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends
     * events on the given urls. By default the ThreadingProfile for the
     * components will be set so that there will only be one thread of
     * execution.
     * 
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload
     *            type
     * @param name The identifying name of the components. This can be used to
     *            later unregister it
     * @param listenerEndpointUri The url endpointUri to listen to
     * @param sendEndpointUri The url endpointUri to dispatch to
     * @throws UMOException
     */
    public UMODescriptor registerComponentInstance(Object component,
                                                   String name,
                                                   UMOEndpointURI listenerEndpointUri,
                                                   UMOEndpointURI sendEndpointUri) throws UMOException
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setImplementationInstance(component);
        descriptor.setName(name);

        // Create the endpoints
        UMOEndpoint inboundProvider = null;
        UMOEndpoint outboundProvider = null;
        if (listenerEndpointUri != null) {
            inboundProvider = ConnectorFactory.createEndpoint(listenerEndpointUri, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        }
        if (sendEndpointUri != null) {
            outboundProvider = ConnectorFactory.createEndpoint(sendEndpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        descriptor.setInboundEndpoint(inboundProvider);
        descriptor.setOutboundEndpoint(outboundProvider);

        // register the components descriptor
        manager.getModel().registerComponent(descriptor);
        return descriptor;
    }

    public UMOComponent registerComponent(String implementation,
                                          String name,
                                          String inboundEndpoint,
                                          String outboundEndpoint,
                                          Map properties) throws UMOException
    {
        UMOEndpoint inEndpoint = null;
        UMOEndpoint outEndpoint = null;
        if (inboundEndpoint != null) {
            inEndpoint = manager.lookupEndpoint(inboundEndpoint);
            if (inEndpoint == null) {
                inEndpoint = createEndpoint(inboundEndpoint, null, true);
            }
        }
        if (outboundEndpoint != null) {
            outEndpoint = manager.lookupEndpoint(outboundEndpoint);
            if (outEndpoint == null) {
                outEndpoint = createEndpoint(outboundEndpoint, null, false);
            }
        }
        UMODescriptor d = createDescriptor(implementation, name, inEndpoint, outEndpoint, properties);
        return registerComponent(d);
    }

    public UMOComponent registerComponent(String implementation,
                                          String name,
                                          UMOEndpoint inEndpoint,
                                          UMOEndpoint outEndpoint,
                                          Map properties) throws UMOException
    {
        UMODescriptor d = createDescriptor(implementation, name, inEndpoint, outEndpoint, properties);
        return registerComponent(d);
    }

    /**
     * Registers a user configured MuleDescriptor of a components to the server.
     * If users want to register object instances with the server rather than
     * class names that get created at runtime or reference to objects in the
     * container, the user must call the descriptors setImplementationInstance()
     * method - <code>
     *     MyBean implementation = new MyBean();
     *     descriptor.setImplementationInstance(implementation);
     * </code>
     * Calling this method is equivilent to calling
     * UMOModel.registerComponent(..)
     * 
     * @param descriptor the componet descriptor to register
     * @throws UMOException the descriptor is invalid or cannot be initialised
     *             or started
     * @see org.mule.umo.model.UMOModel
     */
    public UMOComponent registerComponent(UMODescriptor descriptor) throws UMOException
    {
        return manager.getModel().registerComponent(descriptor);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on
     * the given url. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation
     * @param name The identifying name of the components. This can be used to
     *            later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @throws org.mule.umo.UMOException
     */
    public UMOComponent registerComponent(String implementation, String name, UMOEndpointURI inboundEndpointUri)
            throws UMOException
    {
        return registerComponent(implementation, name, inboundEndpointUri, null, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on
     * the given url. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation
     * @param name The identifying name of the components. This can be used to
     *            later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param properties properties to set on the component
     * @throws org.mule.umo.UMOException
     */
    public UMOComponent registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          Map properties) throws UMOException
    {
        return registerComponent(implementation, name, inboundEndpointUri, null, properties);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends
     * events on the given urls. By default the ThreadingProfile for the
     * components will be set so that there will only be one thread of
     * execution.
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation which event to invoke based on the evnet
     *            payload type
     * @param name The identifying name of the components. This can be used to
     *            later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param outboundEndpointUri The url endpointUri to dispatch to
     * 
     * @throws UMOException
     */
    public UMOComponent registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          UMOEndpointURI outboundEndpointUri) throws UMOException
    {
        return registerComponent(implementation, name, inboundEndpointUri, outboundEndpointUri, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends
     * events on the given urls. By default the ThreadingProfile for the
     * components will be set so that there will only be one thread of
     * execution.
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation which event to invoke based on the evnet
     *            payload type
     * @param name The identifying name of the components. This can be used to
     *            later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param outboundEndpointUri The url endpointUri to dispatch to
     * @param properties properties to set on the component
     * @throws UMOException
     */
    public UMOComponent registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          UMOEndpointURI outboundEndpointUri,
                                          Map properties) throws UMOException
    {
        UMODescriptor d = createDescriptor(implementation, name, inboundEndpointUri, outboundEndpointUri, properties);
        return manager.getModel().registerComponent(d);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling
     * class before registering it with the UMOModel
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation which event to invoke based on the evnet
     *            payload type
     * @param name The identifying name of the component. This can be used to
     *            later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to. Can be null
     * @param outboundEndpointUri The url endpointUri to dispatch to. Can be
     *            null
     * @param properties properties to set on the component. Can be null
     * @throws UMOException
     */
    public UMODescriptor createDescriptor(String implementation,
                                          String name,
                                          String inboundEndpointUri,
                                          String outboundEndpointUri,
                                          Map properties) throws UMOException
    {
        UMOEndpointURI inEndpointUri = null;
        UMOEndpointURI outEndpointUri = null;
        if (inboundEndpointUri != null) {
            inEndpointUri = new MuleEndpointURI(inboundEndpointUri);
        }
        if (outboundEndpointUri != null) {
            outEndpointUri = new MuleEndpointURI(outboundEndpointUri);
        }

        return createDescriptor(implementation, name, inEndpointUri, outEndpointUri, properties);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling
     * class before registering it with the UMOModel
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation which event to invoke based on the evnet
     *            payload type
     * @param name The identifying name of the component. This can be used to
     *            later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to. Can be null
     * @param outboundEndpointUri The url endpointUri to dispatch to. Can be
     *            null
     * @param properties properties to set on the component. Can be null
     * @throws UMOException
     */
    public UMODescriptor createDescriptor(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          UMOEndpointURI outboundEndpointUri,
                                          Map properties) throws UMOException
    {
        // Create the endpoints
        UMOEndpoint inboundEndpoint = null;
        UMOEndpoint outboundEndpoint = null;
        if (inboundEndpointUri != null) {
            inboundEndpoint = ConnectorFactory.createEndpoint(inboundEndpointUri, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        }
        if (outboundEndpointUri != null) {
            outboundEndpoint = ConnectorFactory.createEndpoint(outboundEndpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        return createDescriptor(implementation, name, inboundEndpoint, outboundEndpoint, properties);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling
     * class before registering it with the UMOModel
     * 
     * @param implementation either a container refernece to an object or a
     *            fully qualified class name to use as the component
     *            implementation which event to invoke based on the evnet
     *            payload type
     * @param name The identifying name of the component. This can be used to
     *            later unregister it
     * @param inboundEndpoint The endpoint to listen to. Can be null
     * @param outboundEndpoint The endpoint to dispatch to. Can be null
     * @param properties properties to set on the component. Can be null
     * @throws UMOException
     */
    public UMODescriptor createDescriptor(String implementation,
                                          String name,
                                          UMOEndpoint inboundEndpoint,
                                          UMOEndpoint outboundEndpoint,
                                          Map properties) throws UMOException
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setImplementation(implementation);
        descriptor.setName(name);
        if (properties != null) {
            descriptor.getProperties().putAll(properties);
        }

        descriptor.setInboundEndpoint(inboundEndpoint);
        descriptor.setOutboundEndpoint(outboundEndpoint);

        return descriptor;
    }

    /**
     * Sets the component resolver on the model. Component resolver is used to
     * look up components in an external container such as Spring or Pico
     * 
     * @param ctx
     * @throws UMOException
     */
    public void setContainerContext(UMOContainerContext ctx) throws UMOException
    {
        manager.setContainerContext(ctx);
    }

    /**
     * Unregisters a previously register components. This will also unregister
     * any listeners for the components Calling this method is equivilent to
     * calling UMOModel.unregisterComponent(..)
     * 
     * @param name the name of the componet to unregister
     * @throws UMOException if unregistering the components fails, i.e. The
     *             underlying transport fails to unregister a listener. If the
     *             components does not exist, this method should not throw an
     *             exception.
     * @see org.mule.umo.model.UMOModel
     */
    public void unregisterComponent(String name) throws UMOException
    {
        UMODescriptor descriptor = manager.getModel().getDescriptor(name);
        if (descriptor != null) {
            manager.getModel().unregisterComponent(descriptor);
        }
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound) throws UMOException
    {
        return createEndpoint(uri, name, inbound, null, null);
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound, String transformers) throws UMOException
    {
        return createEndpoint(uri, name, inbound, transformers, null);
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound, UMOFilter filter) throws UMOException
    {
        return createEndpoint(uri, name, inbound, null, filter);
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound, String transformers, UMOFilter filter) throws UMOException
    {
        UMOEndpoint ep = MuleEndpoint.createEndpointFromUri(new MuleEndpointURI(uri), (inbound
                ? UMOEndpoint.ENDPOINT_TYPE_RECEIVER : UMOEndpoint.ENDPOINT_TYPE_SENDER));
        ep.setName(name);
        if(transformers!=null) {
            String delim = (transformers.indexOf(",") > - 1 ? "," : " ");
            ep.setTransformer(MuleObjectHelper.getTransformer(transformers, delim));
        }
        ep.setFilter(filter);
        return ep;
    }

    public UMOEndpoint registerEndpoint(String uri, String name, boolean inbound) throws UMOException
    {
        UMOEndpoint ep = createEndpoint(uri, name, inbound);
        ep.initialise();
        manager.registerEndpoint(ep);
        return ep;
    }

    public UMOEndpoint registerEndpoint(String uri, String name, boolean inbound, Map properties) throws UMOException
    {
        UMOEndpoint ep = createEndpoint(uri, name, inbound);
        ep.getProperties().putAll(properties);
        ep.initialise();
        manager.registerEndpoint(ep);
        return ep;
    }

    public UMOEndpoint registerEndpoint(String uri, String name, boolean inbound, Map properties, UMOFilter filter) throws UMOException
    {
        UMOEndpoint ep = createEndpoint(uri, name, inbound);
        if(properties!=null) ep.getProperties().putAll(properties);
        if(filter!=null) ep.setFilter(filter);
        ep.initialise();
        manager.registerEndpoint(ep);
        return ep;
    }

    public void registerModel(UMOModel model) throws UMOException {
        manager.setModel(model);
    }

    public UMOManager getManager(){
        return manager;
    }

    public UMOManager configure(String configResources) throws ConfigurationException {
        try {
            manager.start();
        } catch (UMOException e) {
            throw new ConfigurationException(e);
        }
        return manager;
    }

    public UMOManager configure(ReaderResource[] configResources) throws ConfigurationException {
        try {
            manager.start();
        } catch (UMOException e) {
            throw new ConfigurationException(e);
        }
        return manager;
    }

    public boolean isConfigured() {
        return manager!=null; 
    }
}
