/*
 * $Id$
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
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.config.MuleConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface Registry extends Initialisable, Disposable
{
    public static final int SCOPE_IMMEDIATE = 0;
    public static final int SCOPE_LOCAL = 1;
    public static final int SCOPE_REMOTE = 2;

    public static final int DEFAULT_SCOPE = SCOPE_REMOTE;

    // /////////////////////////////////////////////////////////////////////////
    // Lookup methods - these should NOT create a new object, only return existing ones
    // /////////////////////////////////////////////////////////////////////////

    /** Look up a single object by name. */
    Object lookupObject(String key);

    /** Look up a single object by name. */
    Object lookupObject(String key, int scope);

    /** Look up all objects of a given type. */
    Collection lookupObjects(Class type);

    /** Look up all objects of a given type. */
    Collection lookupObjects(Class type, int scope);

    /** Look up a single object by type. */
    Object lookupObject(Class type) throws RegistrationException;

    /** Look up a single object by type. */
    Object lookupObject(Class type, int scope) throws RegistrationException;
    
    // TODO Not sure these methods are needed since the generic ones above can be used.

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

    // TODO MULE-2162 MuleConfiguration belongs in the MuleContext rather than the Registry
    MuleConfiguration getConfiguration();

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

    void registerObject(String key, Object value) throws RegistrationException;

    void registerObject(String key, Object value, Object metadata) throws RegistrationException;

    void registerObjects(Map objects) throws RegistrationException;

    void unregisterObject(String key) throws MuleException;

    // TODO MULE-2139 The following methods are Mule-specific and should be split out into a separate class;
    // leave this one as a "pure" registry interface.

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

    // TODO MULE-2162 MuleConfiguration belongs in the MuleContext rather than the Registry
    void setConfiguration(MuleConfiguration config);

    // /////////////////////////////////////////////////////////////////////////
    // Creation methods
    // /////////////////////////////////////////////////////////////////////////

    // TODO These methods are a mess (they blur lookup with creation, uris with names). Need to clean this up.

    ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides)
            throws ServiceException;

    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    Registry getParent();

    void setParent(Registry registry);

    String getRegistryId();

    boolean isReadOnly();

    boolean isRemote();

    void setDefaultScope(int scope);

    int getDefaultScope();

    boolean isInitialised();

    boolean isInitialising();

    boolean isDisposed();

    boolean isDisposing();
}
