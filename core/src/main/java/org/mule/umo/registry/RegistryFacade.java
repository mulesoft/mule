/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.registry;

import org.mule.config.MuleConfiguration;
import org.mule.registry.Registry;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceException;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;
import java.util.Properties;

/**
 * TODO
 */

public interface RegistryFacade extends Initialisable, Registry
{
    public static final int SCOPE_IMMEDIATE = 0;
    public static final int SCOPE_LOCAL = 1;
    public static final int SCOPE_REMOTE = 2;
    
    public static final int DEFAULT_SCOPE = SCOPE_REMOTE;

    MuleConfiguration getConfiguration();

    void setConfiguration(MuleConfiguration config);

    UMOConnector lookupConnector(String name);

    UMOEndpoint lookupEndpoint(String logicalName);

    UMOTransformer lookupTransformer(String name);

    void registerConnector(UMOConnector connector) throws UMOException;

    UMOConnector unregisterConnector(String connectorName) throws UMOException;

    void registerEndpoint(UMOEndpoint endpoint)  throws UMOException;;

    UMOImmutableEndpoint unregisterEndpoint(String endpointName);

    void registerTransformer(UMOTransformer transformer) throws UMOException;

    UMOTransformer unregisterTransformer(String transformerName);

    public void registerService(UMODescriptor service) throws UMOException;

    public UMODescriptor lookupService(String serviceName);

    public UMODescriptor unregisterService(String serviceName);

    void registerProperty(Object key, Object value);

    void registerProperties(Map props);

    UMOModel lookupModel(String name);

    void registerModel(UMOModel model) throws UMOException;

    UMOModel unregisterModel(String modelName);

    Map getModels();

    Map getConnectors();

    Map getEndpoints();

    Map getAgents();

    Map getServices();

    Map getTransformers();

    boolean isInitialised();

    boolean isInitialising();

    boolean isDisposed();

    boolean isDisposing();

    void registerAgent(UMOAgent agent) throws UMOException;

    //void registerObject(Class type, Object key, Object value);

    void registerObject(Object key, Object value);

    Object unregisterObject(String key);


    UMOAgent lookupAgent(String agentName);

    UMOAgent unregisterAgent(String agentName) throws UMOException;

    ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException;

    Object lookupObject(Object key, Class returnType);

    Object lookupObject(Object key, Class returnType, int scope);

    Object lookupObject(Object key);

    Object lookupObject(Object key, int scope);

    Map lookupCollection(Class returntype);

    Map lookupCollection(Class returntype, int scope);

     Object lookupProperty(Object key);

    Object lookupProperty(Object key, int scope);

    Map lookupProperties();

    Map lookupProperties(int scope);

    UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException;

    UMOEndpoint getEndpointFromUri(String uri) throws ObjectNotFoundException;

    UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException;

    UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException;

    UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException;

    UMOManagementContext getManagementContext();

    RegistryFacade getParent();

    void setParent(RegistryFacade registry);

    String getRegistryId();

    boolean isReadOnly();

    boolean isRemote();

    void setDefaultScope(int scope);

    int getDefaultScope();

}
