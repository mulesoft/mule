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
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

public interface Registry extends Initialisable, Disposable
{
    public static final int SCOPE_IMMEDIATE = 0;
    public static final int SCOPE_LOCAL = 1;
    public static final int SCOPE_REMOTE = 2;
    
    public static final int DEFAULT_SCOPE = SCOPE_REMOTE;

    ///////////////////////////////////////////////////////////////////////////
    // Lookup methods - these should NOT create a new object, only return existing ones
    ///////////////////////////////////////////////////////////////////////////

    /** Look up a single object by name. */
    Object lookupObject(String key);

    /** Look up a single object by name. */
    Object lookupObject(String key, int scope);

    /** Look up all objects of a given type. */
    Collection lookupObjects(Class type);

    /** Look up all objects of a given type. */
    Collection lookupObjects(Class type, int scope);

    //Object lookupObject(String key, Class returnType);

    //Object lookupObject(String key, Class returnType, int scope);

    // TODO MULE-2200 
    Object lookupProperty(String key);

    // TODO MULE-2200 
    Object lookupProperty(String key, int scope);

    /** @deprecated Use lookupProperty() instead */    
    Map lookupProperties();

    /** @deprecated Use lookupProperty() instead */    
    Map lookupProperties(int scope);

    // TODO Not sure these methods are needed since the generic ones above can be used.
    
    UMOConnector lookupConnector(String name);

    UMOEndpoint lookupEndpoint(String logicalName);

    UMOTransformer lookupTransformer(String name);

    public UMODescriptor lookupService(String serviceName);

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

    ///////////////////////////////////////////////////////////////////////////
    // Registration methods
    ///////////////////////////////////////////////////////////////////////////

    void registerObject(String key, Object value) throws RegistrationException;

    void registerObject(String key, Object value, Object metadata) throws RegistrationException;

    void registerObject(String key, Object value, UMOManagementContext managementContext) throws RegistrationException;

    void registerObject(String key, Object value, Object metadata, UMOManagementContext managementContext) throws RegistrationException;

    void unregisterObject(String key);

    // TODO MULE-2200 
    void registerProperty(String key, Object value) throws RegistrationException;

    // TODO MULE-2200 
    void unregisterProperty(String key);

    /** @deprecated Use registerProperty() instead */    
    void registerProperties(Map props) throws RegistrationException;

    // TODO MULE-2139 The following methods are Mule-specific and should be split out into a separate class; 
    // leave this one as a "pure" registry interface.
    
    // The deprecated registerXXX() methods below use MuleServer.getManagementContext() as a workaround 
    // to get the managementContext.  They should eventually be replaced by the equivalent method which 
    // receives the managementContext as a parameter.
    
    void registerConnector(UMOConnector connector, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use registerConnector(UMOConnector connector, UMOManagementContext managementContext) instead. */
    void registerConnector(UMOConnector connector) throws UMOException;

    UMOConnector unregisterConnector(String connectorName) throws UMOException;

    void registerEndpoint(UMOEndpoint endpoint, UMOManagementContext managementContext)  throws UMOException;
    /** @deprecated Use registerEndpoint(UMOEndpoint endpoint, UMOManagementContext managementContext) instead. */
    void registerEndpoint(UMOEndpoint endpoint)  throws UMOException;;

    UMOImmutableEndpoint unregisterEndpoint(String endpointName);

    void registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use registerTransformer(UMOTransformer transformer, UMOManagementContext managementContext) instead. */
    void registerTransformer(UMOTransformer transformer) throws UMOException;

    UMOTransformer unregisterTransformer(String transformerName);

    public void registerService(UMODescriptor service, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use registerService(UMODescriptor service, UMOManagementContext managementContext) instead. */
    public void registerService(UMODescriptor service) throws UMOException;

    public UMODescriptor unregisterService(String serviceName);

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

    ///////////////////////////////////////////////////////////////////////////
    // Creation methods
    ///////////////////////////////////////////////////////////////////////////

    // TODO These methods are a mess (they blur lookup with creation, uris with names).  Need to clean this up.
    
    ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException;

    UMOEndpoint getEndpointFromName(String name) throws ObjectNotFoundException;
    UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException;

    UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use getOrCreateEndpointForUri(String uriIdentifier, String type, UMOManagementContext managementContext) instead. */
    UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException;

    UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use getOrCreateEndpointForUri(UMOEndpointURI uri, String type, UMOManagementContext managementContext) instead. */
    UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException;

    UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use createEndpointFromUri(UMOEndpointURI uri, String type, UMOManagementContext managementContext) instead. */
    UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException;

    UMOEndpoint createEndpointFromUri(String uri, String type, UMOManagementContext managementContext) throws UMOException;
    /** @deprecated Use createEndpointFromUri(String uri, String type, UMOManagementContext managementContext) instead. */
    UMOEndpoint createEndpointFromUri(String uri, String type) throws UMOException;

    /** @deprecated Use {@link #getEndpointFromUri(org.mule.umo.endpoint.UMOEndpointURI)}
     *  or {@link #getEndpointFromName(String)} instead  **/
    UMOEndpoint getEndpointFromUri(String uri) throws ObjectNotFoundException;

    ///////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    ///////////////////////////////////////////////////////////////////////////

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
