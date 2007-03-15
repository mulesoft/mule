/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.config.MuleConfiguration;
import org.mule.registry.Registry;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceException;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

/**
 * TODO
 */

public interface RegistryFacade extends Initialisable, Registry
{
    MuleConfiguration getConfiguration();

    void setConfiguration(MuleConfiguration config);

    Object getProperty(Object key);

    Map getProperties();

    TransactionManager getTransactionManager();

    UMOConnector lookupConnector(String name);

    UMOEndpoint lookupEndpoint(String logicalName);

    UMOTransformer lookupTransformer(String name);

    void registerConnector(UMOConnector connector) throws UMOException;

    void unregisterConnector(String connectorName) throws UMOException;

    void registerEndpoint(UMOEndpoint endpoint)  throws UMOException;;

    void unregisterEndpoint(String endpointName);

    void registerTransformer(UMOTransformer transformer) throws UMOException;

    void unregisterTransformer(String transformerName);

    void setProperty(Object key, Object value);

    void addProperties(Map props);

    void setTransactionManager(TransactionManager newManager) throws UMOException;

    void start(String serverUrl) throws UMOException;

    UMOModel lookupModel(String name);

    void registerModel(UMOModel model) throws UMOException;

    void unregisterModel(String name);

    Map getModels();

    Map getConnectors();

    Map getEndpoints();

    Map getTransformers();

    boolean isStarted();

    boolean isInitialised();

    boolean isInitialising();

    boolean isStopping();

    void registerAgent(UMOAgent agent) throws UMOException;

    UMOAgent lookupAgent(String name);

    UMOAgent unregisterAgent(String name) throws UMOException;

    void registerContainerContext(UMOContainerContext container) throws UMOException;

    UMOContainerContext getContainerContext();

    ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException;

    Object lookupObject(Object key) throws ObjectNotFoundException;

    public UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException;

    public UMOEndpoint getEndpointFromUri(String uri) throws ObjectNotFoundException;

    public UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException;

    public UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException;

    public UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException;

}
