/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.model.Model;
import org.mule.api.schedule.Scheduler;
import org.mule.api.service.Service;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.util.Predicate;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Adds lookup/register/unregister methods for Mule-specific entities to the standard
 * Registry interface.
 */
public interface MuleRegistry extends LifecycleRegistry
{

    /**
     * Pass this flag as metadata of the {@link Registry#registerObject(String, Object, Object)}  method to have lifecycle
     * method calls on the registered objects omitted. Unless extending Mule, one will
     * probably never have a use for this.
     *
     * @see Registry#registerObject(String, Object, Object)
     */
    public static final int LIFECYCLE_BYPASS_FLAG = 0x01;

    /**
     * Determines whether Inject processors should get executed on an object added to the registry
     * Inject processors are responsible for processing inject interfaces such as {@link org.mule.api.context.MuleContextAware}
     */
    public static final int INJECT_PROCESSORS_BYPASS_FLAG = 0x02;

    /**
     * Determines whether pre-init processors should get executed on an object added to the registry.
     * Pre init processors are basically object processors that do not inject members into objects.  These
     * processors happen after the inject processors
     */
    public static final int PRE_INIT_PROCESSORS_BYPASS_FLAG = 0x04;

    // /////////////////////////////////////////////////////////////////////////
    // Lookup methods - these should NOT create a new object, only return existing ones
    // /////////////////////////////////////////////////////////////////////////

    Connector lookupConnector(String name);

    /**
     * Looks-up endpoint builders which can be used to repeatably create endpoints with the same configuration.
     * These endpoint builder are either global endpoints or they are builders used to create named
     * endpoints configured on routers and exception strategies.
     *
     * @param name the name of the endpointBuilder to find
     * @return An endpointBuilder with the name specified or null if there is no endpoint builder with that name
     */
    EndpointBuilder lookupEndpointBuilder(String name);

    /**
     * @deprecated use {@link MuleContext#getEndpointFactory()} instead
     */
    @Deprecated
    EndpointFactory lookupEndpointFactory();

    Transformer lookupTransformer(String name);

    @Deprecated
    Service lookupService(String name);

    FlowConstruct lookupFlowConstruct(String name);

    /**
     * This method will return a list of {@link org.mule.api.transformer.Transformer} objects that accept the given
     * input and return the given output type of object
     *
     * @param input  The  desiered input type for the transformer
     * @param output the desired output type for the transformer
     * @return a list of matching transformers. If there were no matchers an empty list is returned.
     * @deprecated use {@link #lookupTransformers(org.mule.api.transformer.DataType, org.mule.api.transformer.DataType)} instead
     */
    @Deprecated
    List<Transformer> lookupTransformers(Class<?> input, Class<?> output);

    /**
     * This method will return a list of {@link org.mule.api.transformer.Transformer} objects that accept the given
     * input and return the given output type of object
     *
     * @param source The  desired input type for the transformer
     * @param result the desired output type for the transformer
     * @return a list of matching transformers. If there were no matchers an empty list is returned.
     * @since 3.0.0
     */
    List<Transformer> lookupTransformers(DataType<?> source, DataType<?> result);

    /**
     * Will find a transformer that is the closest match to the desired input and output.
     *
     * @param input  The  desiered input type for the transformer
     * @param output the desired output type for the transformer
     * @return A transformer that exactly matches or the will accept the input and output parameters
     * @throws TransformerException will be thrown if there is more than one match
     * @deprecated use {@link #lookupTransformer(org.mule.api.transformer.DataType, org.mule.api.transformer.DataType)} instead
     */
    @Deprecated
    Transformer lookupTransformer(Class<?> input, Class<?> output) throws TransformerException;

    /**
     * Will find a transformer that is the closest match to the desired input and output.
     *
     * @param source The  desiered input type for the transformer
     * @param result the desired output type for the transformer
     * @return A transformer that exactly matches or the will accept the input and output parameters
     * @throws TransformerException will be thrown if there is more than one match
     * @since 3.0.0
     */
    Transformer lookupTransformer(DataType<?> source, DataType<?> result) throws TransformerException;

    @Deprecated
    Collection<Service> lookupServices(String model);

    @Deprecated
    Collection<Service> lookupServices();

    Collection<FlowConstruct> lookupFlowConstructs();

    @Deprecated
    Model lookupModel(String name);

    @Deprecated
    Model lookupSystemModel();

    Agent lookupAgent(String agentName);

    /**
     * @deprecated Use lookupModel() instead
     */
    @Deprecated
    Collection<Model> getModels();

    /**
     * @deprecated Use lookupConnector() instead
     */
    @Deprecated
    Collection<Connector> getConnectors();

    /**
     * @deprecated Use {@link org.mule.api.endpoint.EndpointFactory} for creation/lookup of individual endpoints instead
     */
    @Deprecated
    Collection<ImmutableEndpoint> getEndpoints();

    /**
     * @deprecated Use lookupAgent() instead
     */
    @Deprecated
    Collection<Agent> getAgents();

    /**
     * @deprecated Use lookupTransformer() instead
     */
    @Deprecated
    Collection<Transformer> getTransformers();

    // /////////////////////////////////////////////////////////////////////////
    // Registration methods
    // /////////////////////////////////////////////////////////////////////////

    void registerConnector(Connector connector) throws MuleException;

    @Deprecated
    void unregisterConnector(String connectorName) throws MuleException;

    //TODO MULE-2494
    void registerEndpoint(ImmutableEndpoint endpoint) throws MuleException;

    //TODO MULE-2494
    @Deprecated
    void unregisterEndpoint(String endpointName) throws MuleException;

    public void registerEndpointBuilder(String name, EndpointBuilder builder) throws MuleException;

    void registerTransformer(Transformer transformer) throws MuleException;

    void unregisterTransformer(String transformerName) throws MuleException;

    @Deprecated
    void registerService(Service service) throws MuleException;

    @Deprecated
    void unregisterService(String serviceName) throws MuleException;

    void registerFlowConstruct(FlowConstruct flowConstruct) throws MuleException;

    @Deprecated
    void unregisterFlowConstruct(String flowConstructName) throws MuleException;

    @Deprecated
    void registerModel(Model model) throws MuleException;

    @Deprecated
    void unregisterModel(String modelName) throws MuleException;

    void registerAgent(Agent agent) throws MuleException;

    void unregisterAgent(String agentName) throws MuleException;


    void registerScheduler(Scheduler scheduler) throws MuleException;

    void unregisterScheduler(Scheduler scheduler) throws MuleException;

    Collection<Scheduler> lookupScheduler(Predicate<String> schedulerNamePredicate);

    /**
     * Will execute any processors on an object and fire any lifecycle methods according to the current lifecycle without actually
     * registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.  Not that this will only be applied to Mule registies.  Thrid party registries
     * such as Guice support wiring, but you need to get a reference to the container/context to call the method.  This is so that
     * wiring mechanisms dont trip over each other.
     *
     * @param object the object to process
     * @return the same object with any processors and lifecycle methods called
     * @throws org.mule.api.MuleException if the registry fails to perform the lifecycle change or process object processors for the object.
     */
    Object applyProcessorsAndLifecycle(Object object) throws MuleException;

    /**
     * Will execute any processors on an object without actually registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.  Not that this will only be applied to Mule registries.  Third party registries
     * such as Guice support wiring, but you need to get a reference to the container/context to call the method.  This is so that
     * wiring mechanisms dont trip over each other.
     *
     * @param object the object to process
     * @return the same object with any processors called
     * @throws org.mule.api.MuleException if the registry fails to process object processors for the object.
     */
    Object applyProcessors(Object object) throws MuleException;

    /**
     * Will execute any processors on an object without actually registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.  Not that this will only be applied to Mule registries.  Third party registries
     * such as Guice support wiring, but you need to get a reference to the container/context to call the method.  This is so that
     * wiring mechanisms don't trip over each other.
     *
     * @param object the object to process
     * @param flags {@link org.mule.api.registry.MuleRegistry} flags which control which injectors will be applied
     * @return the same object with any processors called
     * @throws org.mule.api.MuleException if the registry fails to process object processors for the object.
     * @since 3.0
     * @deprecated as of 3.7.0. Use {@link #applyProcessors(Object)} instead.
     */
    @Deprecated
    Object applyProcessors(Object object, int flags) throws MuleException;

    // /////////////////////////////////////////////////////////////////////////
    // Creation methods
    // /////////////////////////////////////////////////////////////////////////

    // TODO These methods are a mess (they blur lookup with creation, uris with names). Need to clean this up.

    ServiceDescriptor lookupServiceDescriptor(ServiceType type, String name, Properties overrides)
            throws ServiceException;
}
