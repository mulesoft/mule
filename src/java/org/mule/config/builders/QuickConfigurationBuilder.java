/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config.builders;

import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleModel;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManager;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.model.UMOContainerContext;
import org.mule.umo.provider.UMOConnector;

import java.util.Map;

/**
 * <code>QuickConfigurationBuilder</code> is a configuration helper that can be
 * used by clients, configuration scripts or test cases to quickly configure a
 * manager
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class QuickConfigurationBuilder
{
    private UMOManager manager;

    /**
     * Constructs a default builder
     */
    public QuickConfigurationBuilder()
    {
        manager = MuleManager.getInstance();
    }

    /**
     * Will construct a new Quick Config builder with the option
     * of disposing of the current Manager if one exists
     * @param disposeCurrent true to dispose the current manager
     * @throws UMOException if the manager throws an exception when
     * disposing
     */
    public QuickConfigurationBuilder(boolean disposeCurrent) throws UMOException
    {
        if(disposeCurrent) {
            disposeCurrent();
        }

        manager = MuleManager.getInstance();
    }

    /**
     * Disposes the current MuleManager if there is one.
     * @throws UMOException if there is a current Manager and it fails to shutdown
     */
    public void disposeCurrent() throws UMOException
    {
        if(MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }
    }

    /**
     * Configures a started manager.  This method will throw InitialisationException
     * if the current manager is already started
     * @param synchronous whether to start the manager in synchronous mode
     * @param serverUrl the url used to receive client requests, or null if the server
     * listening components should not be set up
     * @return the configured manager
     * @throws UMOException if the manager is already started or it fails to start
     */
    public UMOManager createStartedManager(boolean synchronous, String serverUrl) throws UMOException
    {
        if(manager.isStarted()) {
            throw new InitialisationException("Manager already started");
        }
        if(serverUrl==null) serverUrl = "";
        MuleManager.getConfiguration().setServerUrl(serverUrl);
        MuleManager.getConfiguration().setSynchronous(synchronous);
        manager.setModel(new MuleModel());
        manager.start();
        return manager;
    }

    /**
     * Configures a started manager.  This method will throw InitialisationException
     * if the current manager is already started
     * @param synchronous whether to start the manager in synchronous mode
     * @param serverUrl the url used to receive client requests, or null if the server
     * listening components should not be set up
     * @param serverConnector The server connector to use for the serverUrl
     * @return the configured manager
     * @throws UMOException if the manager is already started or it fails to start
     */
    public UMOManager createStartedManager(boolean synchronous, String serverUrl, UMOConnector serverConnector) throws UMOException
    {
        if(serverConnector != null) {
            manager.registerConnector(serverConnector);
        }
        //set the connector on th endpointUri
        int param = serverUrl.indexOf("?");
        if(param==-1) {
            serverUrl += "?";
        } else {
            serverUrl += "&";
        }
        serverUrl += UMOEndpointURI.PROPERTY_CREATE_CONNECTOR  + "=" + serverConnector.getName();

        return createStartedManager(synchronous, serverUrl);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     * @param component any java object, Mule will it's endpointUri discovery to determine
     * which event to invoke based on the evnet payload type
     * @param name The identifying name of the components.  This can be used to later unregister it
     * @param listenerEndpointUri The url endpointUri to listen to
     * @throws org.mule.umo.UMOException
     */
    public UMODescriptor registerComponentInstance(Object component, String name, UMOEndpointURI listenerEndpointUri) throws UMOException
    {
        return registerComponentInstance(component, name, listenerEndpointUri, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events on the
     * given urls. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     * @param component any java object, Mule will it's endpointUri discovery to determine
     * which event to invoke based on the evnet payload type
     * @param name The identifying name of the components.  This can be used to later unregister it
     * @param listenerEndpointUri The url endpointUri to listen to
     * @param sendEndpointUri The url endpointUri to dispatch to
     * @throws UMOException
     */
    public UMODescriptor registerComponentInstance(Object component, String name, UMOEndpointURI listenerEndpointUri, UMOEndpointURI sendEndpointUri) throws UMOException
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setImplementationInstance(component);
        descriptor.setName(name);

        //Create the endpoints
        UMOEndpoint inboundProvider = null;
        UMOEndpoint outboundProvider = null;
        if(listenerEndpointUri != null) {
            inboundProvider = ConnectorFactory.createEndpoint(listenerEndpointUri, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        }
        if(sendEndpointUri!=null) {
            outboundProvider = ConnectorFactory.createEndpoint(sendEndpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        descriptor.setInboundEndpoint(inboundProvider);
        descriptor.setOutboundEndpoint(outboundProvider);

        //set the threading and pooling profile for a single object instance
//        ThreadingProfile tp = new ThreadingProfile(1, 1, -1, ThreadingProfile.WHEN_EXHAUSTED_WAIT, null,null);
//        PoolingProfile pp = new PoolingProfile(1, 1, -1, ObjectPool.WHEN_EXHAUSTED_BLOCK, PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);
//        descriptor.setThreadingProfile(tp);
//        descriptor.setPoolingProfile(pp);

        //register the components descriptor
        manager.getModel().registerComponent(descriptor);
        return descriptor;
    }

    /**
     * Registers a user configured MuleDescriptor of a components to the server.
     * If users want to register object instances with the server rather than
     * class names that get created at runtime or reference to objects in the
     * container, the user must call the descriptors setImplementationInstance() method -
     * <code>
     *     MyBean implementation = new MyBean();
     *     descriptor.setImplementationInstance(implementation);
     * </code>
     * Calling this method is equivilent to calling UMOModel.registerComponent(..)
     *
     * @param descriptor the componet descriptor to register
     * @throws UMOException the descriptor is invalid or cannot be initialised or started
     * @see org.mule.umo.model.UMOModel
     */
    public void registerComponent(UMODescriptor descriptor) throws UMOException
    {
        manager.getModel().registerComponent(descriptor);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     * @param implementation either a container refernece to an object or a fully qualified class name
     * to use as the component implementation
     * @param name The identifying name of the components.  This can be used to later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @throws org.mule.umo.UMOException
     */
    public UMOComponent registerComponent(String implementation, String name, UMOEndpointURI inboundEndpointUri) throws UMOException
    {
        return registerComponent(implementation, name, inboundEndpointUri, null, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     * @param implementation either a container refernece to an object or a fully qualified class name
     * to use as the component implementation
     * @param name The identifying name of the components.  This can be used to later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param properties properties to set on the component
     * @throws org.mule.umo.UMOException
     */
    public UMOComponent registerComponent(String implementation, String name, UMOEndpointURI inboundEndpointUri, Map properties) throws UMOException
    {
        return registerComponent(implementation, name, inboundEndpointUri, null, properties);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events on the
     * given urls. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     * @param implementation either a container refernece to an object or a fully qualified class name
     * to use as the component implementation
     * which event to invoke based on the evnet payload type
     * @param name The identifying name of the components.  This can be used to later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param outboundEndpointUri The url endpointUri to dispatch to
     *
     * @throws UMOException
     */
    public UMOComponent registerComponent(String implementation, String name, UMOEndpointURI inboundEndpointUri, UMOEndpointURI outboundEndpointUri) throws UMOException
    {
        return registerComponent(implementation, name, inboundEndpointUri, outboundEndpointUri, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events on the
     * given urls. By default the ThreadingProfile for the components will be set so that
     * there will only be one thread of execution.
     * @param implementation either a container refernece to an object or a fully qualified class name
     * to use as the component implementation
     * which event to invoke based on the evnet payload type
     * @param name The identifying name of the components.  This can be used to later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param outboundEndpointUri The url endpointUri to dispatch to
     * @param properties properties to set on the component
     * @throws UMOException
     */
    public UMOComponent registerComponent(String implementation, String name, UMOEndpointURI inboundEndpointUri, UMOEndpointURI outboundEndpointUri, Map properties) throws UMOException
    {
        UMODescriptor d = createDescriptor(implementation, name, inboundEndpointUri, outboundEndpointUri, properties);
        return manager.getModel().registerComponent(d);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling class before
     * registering it with the UMOModel
     * @param implementation either a container refernece to an object or a fully qualified class name
     * to use as the component implementation
     * which event to invoke based on the evnet payload type
     * @param name The identifying name of the component.  This can be used to later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to. Can be null
     * @param outboundEndpointUri The url endpointUri to dispatch to. Can be null
     * @param properties properties to set on the component. Can be null
     * @throws UMOException
     */
    public UMODescriptor createDescriptor(String implementation, String name, String inboundEndpointUri, String outboundEndpointUri, Map properties) throws UMOException
    {
        UMOEndpointURI inEndpointUri = null;
        UMOEndpointURI outEndpointUri = null;
        if(inboundEndpointUri!=null) {
            inEndpointUri = new MuleEndpointURI(inboundEndpointUri);
        }
        if(outboundEndpointUri!=null) {
            outEndpointUri = new MuleEndpointURI(outboundEndpointUri);
        }

        return createDescriptor(implementation, name, inEndpointUri, outEndpointUri, properties);
    }
    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling class before
     * registering it with the UMOModel
     * @param implementation either a container refernece to an object or a fully qualified class name
     * to use as the component implementation
     * which event to invoke based on the evnet payload type
     * @param name The identifying name of the component.  This can be used to later unregister it
     * @param inboundEndpointUri The url endpointUri to listen to. Can be null
     * @param outboundEndpointUri The url endpointUri to dispatch to. Can be null
     * @param properties properties to set on the component. Can be null
     * @throws UMOException
     */
    public UMODescriptor createDescriptor(String implementation, String name, UMOEndpointURI inboundEndpointUri, UMOEndpointURI outboundEndpointUri, Map properties) throws UMOException
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setImplementation(implementation);
        descriptor.setName(name);
        if(properties!=null) {
            descriptor.getProperties().putAll(properties);
        }

        //Create the endpoints
        UMOEndpoint inboundProvider = null;
        UMOEndpoint outboundProvider = null;
        if(inboundEndpointUri != null) {
            inboundProvider = ConnectorFactory.createEndpoint(inboundEndpointUri, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        }
        if(outboundEndpointUri!=null) {
            outboundProvider = ConnectorFactory.createEndpoint(outboundEndpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        descriptor.setInboundEndpoint(inboundProvider);
        descriptor.setOutboundEndpoint(outboundProvider);

        return descriptor;
    }

    /**
     * Sets the component resolver on the model.  Component resolver is used
     * to look up components in an external container such as Spring or Pico
     * @param ctx
     * @throws UMOException
     */
    public void setContainerContext(UMOContainerContext ctx) throws UMOException
    {
        manager.setContainerContext(ctx);
    }

    /**
     * Unregisters a previously register components.  This will also unregister any listeners
     * for the components
     * Calling this method is equivilent to calling UMOModel.unregisterComponent(..)
     *
     * @param name the name of the componet to unregister
     * @throws UMOException if unregistering the components fails, i.e.  The underlying
     * transport fails to unregister a listener.  If the components does not exist, this
     * method should not throw an exception.
     * @see org.mule.umo.model.UMOModel
     */
    public void unregisterComponent(String name) throws UMOException
    {
        UMODescriptor descriptor = manager.getModel().getDescriptor(name);
        if(descriptor!=null) {
            manager.getModel().unregisterComponent(descriptor);
        }
    }
}
