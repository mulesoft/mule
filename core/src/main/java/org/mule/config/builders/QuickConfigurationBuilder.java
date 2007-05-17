/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.RegistryContext;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.ReaderResource;
import org.mule.config.spring.TransientRegistry;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.admin.MuleAdminAgent;
import org.mule.impl.model.ModelFactory;
import org.mule.impl.model.seda.SedaModel;
import org.mule.providers.service.TransportFactory;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;
import org.mule.util.ClassUtils;
import org.mule.util.MuleObjectHelper;

import java.util.Map;
import java.util.Properties;

/**
 * <code>QuickConfigurationBuilder</code> is a configuration helper that can be
 * used by clients, configuration scripts or test cases to quickly configure a
 * managementContext.
 * Note that all other builders create a initialised and started Mule instance, however the Quickconfiguration builder does not
 * since it is sometimes used by other builders to create specialised Mule instances and it more desirable to
 * return an non-initialised instance to allow for further customisation
 */
public class QuickConfigurationBuilder implements ConfigurationBuilder
{
    private static final String MODEL_NOT_SET = "not set";

    protected UMOManagementContext managementContext;

    private UMOModel model;


    public QuickConfigurationBuilder() throws UMOException
    {
        this("seda", true);
    }

    /**
     * Configures a configuration builder with a new ManagementContext.
     *
     * @param modeltype The type of component model to start with
     * @throws UMOException if the manager is already started or it fails to start
     */
    public QuickConfigurationBuilder(String modeltype) throws UMOException
    {
        this(modeltype, true);
    }

    public QuickConfigurationBuilder(boolean startManagementContext) throws UMOException
    {
        this("seda", startManagementContext);
    }

    public QuickConfigurationBuilder(String modeltype, boolean startManagementContext) throws UMOException
    {
        if(RegistryContext.getRegistry()==null)
        {
            TransientRegistry registry = TransientRegistry.createNew();
            RegistryContext.setRegistry(registry);
        }
        managementContext = RegistryContext.getRegistry().getManagementContext();


        String defaultModelName = UMOModel.DEFAULT_MODEL_NAME + "-qbuilder";
        if(managementContext.getRegistry().lookupModel(defaultModelName)==null)
        {
            if (!MODEL_NOT_SET.equals(modeltype))
            {
                model = ModelFactory.createModel(modeltype);
            }
            else
            {
                model = ModelFactory.createModel("seda");
            }
            model.setName(defaultModelName);
            managementContext.getRegistry().registerModel(model);
        }
        if(startManagementContext)
        {
            managementContext.start();
        }
    }

    /**
     * Will construct a new Quick Config builder with the option of disposing of the
     * current Manager if one exists
     * 
     * @param managementContext the management context to configure
     */
    public QuickConfigurationBuilder(UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;
    }


    public void disableAdminAgent()
    {
        try
        {
            managementContext.getRegistry().unregisterAgent(MuleAdminAgent.AGENT_NAME);
        }
        catch (UMOException e)
        {
            // ignore
        }
    }

    public void registerModel(String modelType, String name) throws UMOException
    {
        UMOModel model = ModelFactory.createModel(modelType);
        model.setName(name);
        managementContext.getRegistry().registerModel(model);
    }


    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so
     * that there will only be one thread of execution.
     * 
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param listenerEndpointUri The url endpointUri to listen to
     * @throws org.mule.umo.UMOException
     */
    public UMODescriptor registerComponentInstance(Object component,
                                                   String name,
                                                   UMOEndpointURI listenerEndpointUri) throws UMOException
    {
        return registerComponentInstance(component, name, listenerEndpointUri, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events
     * on the given urls. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     * 
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param listenerEndpointUri The url endpointUri to listen to
     * @param sendEndpointUri The url endpointUri to dispatch to
     * @throws UMOException
     */
    public UMODescriptor registerComponentInstance(Object component,
                                                   String name,
                                                   UMOEndpointURI listenerEndpointUri,
                                                   UMOEndpointURI sendEndpointUri) throws UMOException
    {
        // Create the endpoints
        UMOEndpoint inboundProvider = null;
        UMOEndpoint outboundProvider = null;
        if (listenerEndpointUri != null)
        {
            inboundProvider = TransportFactory.createEndpoint(listenerEndpointUri,
                UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        }
        if (sendEndpointUri != null)
        {
            outboundProvider = TransportFactory.createEndpoint(sendEndpointUri,
                UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        return registerComponentInstance(component, name, inboundProvider, outboundProvider);
    }


    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events
     * on the given urls. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     *
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param listenerEndpoint The url endpoint to listen to
     * @param sendEndpoint The url endpointUri to dispatch to
     * @throws UMOException
     */
    public UMODescriptor registerComponentInstance(Object component,
                                                   String name,
                                                   UMOEndpoint listenerEndpoint,
                                                   UMOEndpoint sendEndpoint) throws UMOException
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setName(name);
        descriptor.setService(component);


        descriptor.setOutboundRouter(new OutboundRouterCollection());
        OutboundPassThroughRouter router = new OutboundPassThroughRouter();
        router.addEndpoint(listenerEndpoint);
        descriptor.getOutboundRouter().addRouter(router);
        descriptor.setInboundRouter(new InboundRouterCollection());
        descriptor.getInboundRouter().addEndpoint(sendEndpoint);

        // register the components descriptor
        managementContext.getRegistry().registerService(descriptor);
        return descriptor;
    }

    public void registerComponent(String implementation,
                                          String name,
                                          String inboundEndpoint,
                                          String outboundEndpoint,
                                          Map properties) throws UMOException
    {
        UMOEndpoint inEndpoint = null;
        UMOEndpoint outEndpoint = null;
        if (inboundEndpoint != null)
        {
            inEndpoint = managementContext.getRegistry().lookupEndpoint(inboundEndpoint);
            if (inEndpoint == null)
            {
                inEndpoint = createEndpoint(inboundEndpoint, null, true);
            }
        }
        if (outboundEndpoint != null)
        {
            outEndpoint = managementContext.getRegistry().lookupEndpoint(outboundEndpoint);
            if (outEndpoint == null)
            {
                outEndpoint = createEndpoint(outboundEndpoint, null, false);
            }
        }
        UMODescriptor d = createDescriptor(implementation, name, inEndpoint, outEndpoint, properties);
        registerComponent(d);
    }

    public void registerComponent(String implementation,
                                          String name,
                                          UMOEndpoint inEndpoint,
                                          UMOEndpoint outEndpoint,
                                          Map properties) throws UMOException
    {
        UMODescriptor d = createDescriptor(implementation, name, inEndpoint, outEndpoint, properties);
        registerComponent(d);
    }

    /**
     * Registers a user configured MuleDescriptor of a components to the server. If
     * users want to register object instances with the server rather than class
     * names that get created at runtime or reference to objects in the container,
     * the user must call the descriptors setImplementationInstance() method - <code>
     *     MyBean implementation = new MyBean();
     *     descriptor.setImplementationInstance(implementation);
     * </code>
     * Calling this method is equivilent to calling UMOModel.registerComponent(..)
     * 
     * @param descriptor the componet descriptor to register
     * @throws UMOException the descriptor is invalid or cannot be initialised or
     *             started
     * @see org.mule.umo.model.UMOModel
     */
    public void registerComponent(UMODescriptor descriptor) throws UMOException
    {
         managementContext.getRegistry().registerService(descriptor);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so
     * that there will only be one thread of execution.
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @throws org.mule.umo.UMOException
     */
    public void registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri) throws UMOException
    {
        registerComponent(implementation, name, inboundEndpointUri, null, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for events on the
     * given url. By default the ThreadingProfile for the components will be set so
     * that there will only be one thread of execution.
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param properties properties to set on the component
     * @throws org.mule.umo.UMOException
     */
    public void registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          Map properties) throws UMOException
    {
        registerComponent(implementation, name, inboundEndpointUri, null, properties);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events
     * on the given urls. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation which
     *            event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param outboundEndpointUri The url endpointUri to dispatch to
     * @throws UMOException
     */
    public void registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          UMOEndpointURI outboundEndpointUri) throws UMOException
    {
        registerComponent(implementation, name, inboundEndpointUri, outboundEndpointUri, null);
    }

    /**
     * Registers a java object as a Umo pcomponent that listens for and sends events
     * on the given urls. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation which
     *            event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param inboundEndpointUri The url endpointUri to listen to
     * @param outboundEndpointUri The url endpointUri to dispatch to
     * @param properties properties to set on the component
     * @throws UMOException
     */
    public void registerComponent(String implementation,
                                          String name,
                                          UMOEndpointURI inboundEndpointUri,
                                          UMOEndpointURI outboundEndpointUri,
                                          Map properties) throws UMOException
    {
        UMODescriptor d = createDescriptor(implementation, name, inboundEndpointUri, outboundEndpointUri,
            properties);
        managementContext.getRegistry().registerService(d);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling
     * class before registering it with the UMOModel
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation which
     *            event to invoke based on the evnet payload type
     * @param name The identifying name of the component. This can be used to later
     *            unregister it
     * @param inboundEndpointUri The url endpointUri to listen to. Can be null
     * @param outboundEndpointUri The url endpointUri to dispatch to. Can be null
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
        if (inboundEndpointUri != null)
        {
            inEndpointUri = new MuleEndpointURI(inboundEndpointUri);
        }
        if (outboundEndpointUri != null)
        {
            outEndpointUri = new MuleEndpointURI(outboundEndpointUri);
        }

        return createDescriptor(implementation, name, inEndpointUri, outEndpointUri, properties);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling
     * class before registering it with the UMOModel
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation which
     *            event to invoke based on the evnet payload type
     * @param name The identifying name of the component. This can be used to later
     *            unregister it
     * @param inboundEndpointUri The url endpointUri to listen to. Can be null
     * @param outboundEndpointUri The url endpointUri to dispatch to. Can be null
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
        if (inboundEndpointUri != null)
        {
            inboundEndpoint = TransportFactory.createEndpoint(inboundEndpointUri,
                UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        }
        if (outboundEndpointUri != null)
        {
            outboundEndpoint = TransportFactory.createEndpoint(outboundEndpointUri,
                UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
        return createDescriptor(implementation, name, inboundEndpoint, outboundEndpoint, properties);
    }

    /**
     * Creates a Mule Descriptor that can be further maniputalted by the calling
     * class before registering it with the UMOModel
     * 
     * @param implementation either a container refernece to an object or a fully
     *            qualified class name to use as the component implementation which
     *            event to invoke based on the evnet payload type
     * @param name The identifying name of the component. This can be used to later
     *            unregister it
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
        try
        {
            MuleDescriptor descriptor = new MuleDescriptor();
            descriptor.setService(ClassUtils.instanciateClass(implementation, null));
            descriptor.setName(name);
            descriptor.setModelName(getModel().getName());
            if (properties != null)
            {
                descriptor.getProperties().putAll(properties);
            }
    
            descriptor.setOutboundRouter(new OutboundRouterCollection());
            if(outboundEndpoint!=null)
            {
                OutboundPassThroughRouter router = new OutboundPassThroughRouter();
                router.addEndpoint(outboundEndpoint);
                descriptor.getOutboundRouter().addRouter(router);
            }
            descriptor.setInboundRouter(new InboundRouterCollection());
            if(inboundEndpoint!=null)
            {
                descriptor.getInboundRouter().addEndpoint(inboundEndpoint);
            }
    
            return descriptor;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Unregisters a previously register components. This will also unregister any
     * listeners for the components Calling this method is equivilent to calling
     * UMOModel.unregisterComponent(..)
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
        managementContext.getRegistry().unregisterService(name);
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound) throws UMOException
    {
        return createEndpoint(uri, name, inbound, null, null);
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound, String transformers)
        throws UMOException
    {
        return createEndpoint(uri, name, inbound, transformers, null);
    }

    public UMOEndpoint createEndpoint(String uri, String name, boolean inbound, UMOFilter filter)
        throws UMOException
    {
        return createEndpoint(uri, name, inbound, null, filter);
    }

    public UMOEndpoint createEndpoint(String uri,
                                      String name,
                                      boolean inbound,
                                      String transformers,
                                      UMOFilter filter) throws UMOException
    {
        UMOEndpoint ep = managementContext.getRegistry().getOrCreateEndpointForUri(new MuleEndpointURI(uri), (inbound
                        ? UMOEndpoint.ENDPOINT_TYPE_RECEIVER : UMOEndpoint.ENDPOINT_TYPE_SENDER));
        ep.setName(name);
        if (transformers != null)
        {
            String delim = (transformers.indexOf(",") > -1 ? "," : " ");
            ep.setTransformer(MuleObjectHelper.getTransformer(transformers, delim));
        }
        ep.setFilter(filter);
        return ep;
    }

    public UMOEndpoint registerEndpoint(String uri, String name, boolean inbound) throws UMOException
    {
        UMOEndpoint ep = createEndpoint(uri, name, inbound);
        ep.initialise();
       managementContext.getRegistry().registerEndpoint(ep);
        return ep;
    }

    public UMOEndpoint registerEndpoint(String uri, String name, boolean inbound, Map properties)
        throws UMOException
    {
        UMOEndpoint ep = createEndpoint(uri, name, inbound);
        ep.getProperties().putAll(properties);
        ep.initialise();
       managementContext.getRegistry().registerEndpoint(ep);
        return ep;
    }

    public UMOEndpoint registerEndpoint(String uri,
                                        String name,
                                        boolean inbound,
                                        Map properties,
                                        UMOFilter filter) throws UMOException
    {
        UMOEndpoint ep = createEndpoint(uri, name, inbound);
        if (properties != null)
        {
            ep.getProperties().putAll(properties);
        }
        if (filter != null)
        {
            ep.setFilter(filter);
        }
        ep.initialise();
       managementContext.getRegistry().registerEndpoint(ep);
        return ep;
    }

    public void registerModel(UMOModel model) throws UMOException
    {
        this.model = model;
       managementContext.getRegistry().registerModel(model);
    }

    public UMOManagementContext getManagementContext()
    {
        return managementContext;
    }

    public UMOManagementContext configure(String configResources) throws ConfigurationException
    {
        return configure(configResources, null);
    }

    public UMOManagementContext configure(String configResources, String startupPropertiesFile)
        throws ConfigurationException
    {
        return configure(new ReaderResource[0], null);
    }

    public UMOManagementContext configure(ReaderResource[] configResources) throws ConfigurationException
    {
        return configure(configResources, null);
    }

    public UMOManagementContext configure(ReaderResource[] configResources, Properties startupProperties)
        throws ConfigurationException
    {
        try
        {
           managementContext.start();
        }
        catch (UMOException e)
        {
            throw new ConfigurationException(e);
        }
        return managementContext;
    }

    public boolean isConfigured()
    {
        return managementContext != null;
    }

    public UMOModel getModel() throws UMOException
    {
        if (model == null)
        {
            model = new SedaModel();
            model.setName("main");
            managementContext.getRegistry().registerModel(model);
        }
        return model;
    }
}
