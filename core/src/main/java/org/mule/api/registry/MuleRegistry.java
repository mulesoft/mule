/*
 * $Id: Registry.java 10529 2008-01-25 05:58:36Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.registry;

import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Adds lookup/register/unregister methods for Mule-specific entities to the standard
 * Registry interface.
 */
public interface MuleRegistry extends Registry
{
    // /////////////////////////////////////////////////////////////////////////
    // Lookup methods - these should NOT create a new object, only return existing ones
    // /////////////////////////////////////////////////////////////////////////

    Connector lookupConnector(String name);

    /**
     * Looks up an returns endpoints registered in the registry by their idendifier (currently endpoint name)<br/><br/
     * <b>NOTE: This method does not create new endpoint instances, but rather returns existing endpoint
     * instances that have been registered. This lookup method should be avoided and the intelligent, role
     * specific endpoint lookup methods should be used instead.<br/><br/>
     *
     * @param name the idendtifer/name used to register endpoint in registry
     * @see #lookupInboundEndpoint(String, org.mule.api.MuleContext)
     * @see #lookupResponseEndpoint(String, org.mule.api.MuleContext)
     */
    ImmutableEndpoint lookupEndpoint(String name);

    /**
     * Looks-up endpoint builders which can be used to repeatably create endpoints with the same configuration.
     * These endpoint builder are either global endpoints or they are builders used to create named
     * endpoints configured on routers and exception strategies.
     */
    EndpointBuilder lookupEndpointBuilder(String name);

    EndpointFactory lookupEndpointFactory();

    Transformer lookupTransformer(String name);

    Service lookupService(String component);

    /**
     * This method will return a list of {@link org.mule.api.transformer.Transformer} objects that accept the given
     * input and return the given output type of object
     *
     * @param input  The  desiered input type for the transformer
     * @param output the desired output type for the transformer
     * @return a list of matching transformers. If there were no matchers an empty list is returned.
     */
    List lookupTransformers(Class input, Class output);

    /**
     * Will find a transformer that is the closest match to the desired input and output.
     *
     * @param input  The  desiered input type for the transformer
     * @param output the desired output type for the transformer
     * @return A transformer that exactly matches or the will accept the input and output parameters
     * @throws TransformerException will be thrown if there is more than one match
     */
    Transformer lookupTransformer(Class input, Class output) throws TransformerException;

    Collection/*<Service>*/ lookupComponents(String model);

    Collection/*<Service>*/ lookupComponents();

    Model lookupModel(String name);

    Model lookupSystemModel();

    Agent lookupAgent(String agentName);

    /** @deprecated Use lookupModel() instead */
    Collection getModels();

    /** @deprecated Use lookupConnector() instead */
    Collection getConnectors();

    /** @deprecated Use lookupEndpoint() instead */
    Collection getEndpoints();

    /** @deprecated Use lookupAgent() instead */
    Collection getAgents();

    /** @deprecated Use lookupTransformer() instead */
    Collection getTransformers();

    // /////////////////////////////////////////////////////////////////////////
    // Registration methods
    // /////////////////////////////////////////////////////////////////////////

    void registerConnector(Connector connector) throws MuleException;

    void unregisterConnector(String connectorName) throws MuleException;

    //TODO MULE-2494
    void registerEndpoint(ImmutableEndpoint endpoint) throws MuleException;

    //TODO MULE-2494
    void unregisterEndpoint(String endpointName) throws MuleException;

    public void registerEndpointBuilder(String name, EndpointBuilder builder) throws MuleException;
    
    void registerTransformer(Transformer transformer) throws MuleException;

    void unregisterTransformer(String transformerName) throws MuleException;

    void registerService(Service service) throws MuleException;

    void unregisterComponent(String componentName) throws MuleException;

    void registerModel(Model model) throws MuleException;

    void unregisterModel(String modelName) throws MuleException;

    void registerAgent(Agent agent) throws MuleException;

    void unregisterAgent(String agentName) throws MuleException;

    // /////////////////////////////////////////////////////////////////////////
    // Creation methods
    // /////////////////////////////////////////////////////////////////////////

    // TODO These methods are a mess (they blur lookup with creation, uris with names). Need to clean this up.

    ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides)
            throws ServiceException;
}
