/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.config.MuleConfiguration;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Collection;
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

    // Object lookupObject(String key, Class returnType);

    // Object lookupObject(String key, Class returnType, int scope);

    // TODO Not sure these methods are needed since the generic ones above can be used.

    UMOConnector lookupConnector(String name);

    /**
     * Looks up an returns endpoints registered in the registry by their idendifier (currently endpoint name)<br/><br/
     * <b>NOTE: This method does not create new endpoint instances, but rather returns existing endpoint
     * instances that have been registered. This lookup method should be avoided and the intelligent, role
     * specific endpoint lookup methods should be used instead.<br/><br/>
     * @param name the idendtifer/name used to register endpoint in registry
     * @see #lookupInboundEndpoint(String, org.mule.umo.UMOManagementContext)
     * @see #lookupResponseEndpoint(String, org.mule.umo.UMOManagementContext)
     */
    UMOImmutableEndpoint lookupEndpoint(String name, UMOManagementContext managementContext);

    /**
     * Deprecated. Use {@link #lookupEndpoint(String, UMOManagementContext)}
     * @deprecated
     */
    UMOImmutableEndpoint lookupEndpoint(String name);
    
    /**
     * Looks-up endpoint builders which can be used to repeatably create endpoints with the same configuration.
     * These endpoint builder are either global endpoints or they are builders used to create named
     * endpoints configured on routers and exception strategies.
     * 
     */
    UMOEndpointBuilder lookupEndpointBuilder(String name);
    
    UMOEndpointFactory lookupEndpointFactory();
    
    /**
     * Returns immutable endpoint instance with the "INBOUND" role. <br/><br/> The uri paramater can be one
     * of the following:
     * <li> A global endpoint name
     * <li> The name of a concrete endpoint configured with a name via the name attribute.
     * <li> A uri string as documented here :<a
     * href="http://mule.codehaus.org/display/MULE/Mule+Endpoint+URIs">http://mule.codehaus.org/display/MULE/Mule+Endpoint+URIs</a>
     * <br/><br/> The {@link UMOImmutableEndpoint} interface is currently used as the return type but this
     * will be replaces by and more specific interface. SEE MULE-2292
     * 
     */
    UMOImmutableEndpoint lookupInboundEndpoint(String uri, UMOManagementContext managementContext)
        throws UMOException;

    /**
     * Returns immutable endpoint instance with the "OUTBOUND" role. <br/><br/> The uri paramater can be one
     * of the following:
     * <li> A global endpoint name
     * <li> The name of a concrete endpoint configured with a name via the name attribute.
     * <li> A uri string as documented here :<a
     * href="http://mule.codehaus.org/display/MULE/Mule+Endpoint+URIs">http://mule.codehaus.org/display/MULE/Mule+Endpoint+URIs</a>
     * <br/><br/> The {@link UMOImmutableEndpoint} interface is currently used as the return type but this
     * will be replaces by and more specific interface. SEE MULE-2292
     * 
     */
    UMOImmutableEndpoint lookupOutboundEndpoint(String uri, UMOManagementContext managementContext)
        throws UMOException;

    /**
     * Returns immutable endpoint instance with the "RESPONSE" role. <br/><br/> The uri paramater can be one
     * of the following:
     * <li> A global endpoint name
     * <li> The name of a concrete endpoint configured with a name via the name attribute.
     * <li> A uri string as documented here :<a
     * href="http://mule.codehaus.org/display/MULE/Mule+Endpoint+URIs">http://mule.codehaus.org/display/MULE/Mule+Endpoint+URIs</a>
     * <br/><br/> The {@link UMOImmutableEndpoint} interface is currently used as the return type but this
     * will be replaces by and more specific interface. SEE MULE-2292
     * 
     */
    UMOImmutableEndpoint lookupResponseEndpoint(String uri, UMOManagementContext managementContext)
        throws UMOException;

    /**
     */
    UMOImmutableEndpoint createEndpoint(UMOEndpointURI endpointUri,
                                        String endpointType,
                                        UMOManagementContext managementContext)
        throws UMOException;

    UMOTransformer lookupTransformer(String name);

    UMOComponent lookupComponent(String component);

    Collection/*<UMOComponent>*/ lookupComponents(String model);

    UMOModel lookupModel(String name);

    UMOModel lookupSystemModel();

    UMOAgent lookupAgent(String agentName);

    // TODO MULE-2162 MuleConfiguration belongs in the ManagementContext rather than the Registry
    MuleConfiguration getConfiguration();

    /** @deprecated Use lookupModel() instead */
    Collection getModels();

    /** @deprecated Use lookupConnector() instead */
    Collection getConnectors();

    /** @deprecated Use lookupEndpoint() instead */
    Collection getEndpoints();

    /** @deprecated Use lookupAgent() instead */
    Collection getAgents();

    /** @deprecated Use lookupService() instead */
    Collection getServices();

    /** @deprecated Use lookupTransformer() instead */
    Collection getTransformers();

    // /////////////////////////////////////////////////////////////////////////
    // Registration methods
    // /////////////////////////////////////////////////////////////////////////

    void registerObject(String key, Object value) throws RegistrationException;

    void registerObject(String key, Object value, Object metadata) throws RegistrationException;

    void registerObject(String key, Object value, UMOManagementContext managementContext)
        throws RegistrationException;

    void registerObject(String key, Object value, Object metadata, UMOManagementContext managementContext)
        throws RegistrationException;

    void unregisterObject(String key);

    // TODO MULE-2139 The following methods are Mule-specific and should be split out into a separate class;
    // leave this one as a "pure" registry interface.

    // The deprecated registerXXX() methods below use MuleServer.getManagementContext() as a workaround
    // to get the managementContext. They should eventually be replaced by the equivalent method which
    // receives the managementContext as a parameter.

    void registerConnector(UMOConnector connector, UMOManagementContext managementContext)
        throws UMOException;

    /**
     * @deprecated Use registerConnector(UMOConnector connector, UMOManagementContext managementContext)
     *             instead.
     */
    void registerConnector(UMOConnector connector) throws UMOException;

    UMOConnector unregisterConnector(String connectorName) throws UMOException;

    void registerEndpoint(UMOImmutableEndpoint endpoint, UMOManagementContext managementContext)
        throws UMOException;

    /** @deprecated Use registerEndpoint(UMOEndpoint endpoint, UMOManagementContext managementContext) instead. */
    void registerEndpoint(UMOImmutableEndpoint endpoint) throws UMOException;

    public void registerEndpointBuilder(String name, UMOEndpointBuilder builder, UMOManagementContext managementContext) throws UMOException;
    
    UMOImmutableEndpoint unregisterEndpoint(String endpointName);

    void registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext)
        throws UMOException;

    /**
     * @deprecated Use registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext)
     *             instead.
     */
    void registerTransformer(UMOTransformer transformer) throws UMOException;

    UMOTransformer unregisterTransformer(String transformerName);

    void registerComponent(UMOComponent component, UMOManagementContext managementContext) throws UMOException;
    UMOComponent unregisterComponent(String componentName);

    void registerModel(UMOModel model, UMOManagementContext managementContext) throws UMOException;

    /** @deprecated Use registerModel(UMOModel model, UMOManagementContext managementContext) instead. */
    void registerModel(UMOModel model) throws UMOException;

    UMOModel unregisterModel(String modelName);

    void registerAgent(UMOAgent agent, UMOManagementContext managementContext) throws UMOException;

    /** @deprecated Use registerAgent(UMOAgent agent, UMOManagementContext managementContext) instead. */
    void registerAgent(UMOAgent agent) throws UMOException;

    UMOAgent unregisterAgent(String agentName) throws UMOException;

    // TODO MULE-2162 MuleConfiguration belongs in the ManagementContext rather than the Registry
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
