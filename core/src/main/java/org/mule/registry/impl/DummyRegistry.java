/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.registry.Registration;
import org.mule.registry.RegistryException;
import org.mule.registry.RegistryStore;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceException;
import org.mule.registry.UMORegistry;
import org.mule.registry.metadata.ObjectMetadata;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This dummy registry just reports on registration events but
 * doesn't store anything
 * 
 * @deprecated LM: Is this still needed?
 */
public class DummyRegistry implements UMORegistry {

    private Random generator;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(DummyRegistry.class);

    public DummyRegistry() 
    {
        generator = new Random(new java.util.Date().getTime());
    }

    public RegistryStore getRegistryStore() 
    {
        return null;
    }

    /*
    public String registerComponent(ComponentReference component) throws RegistrationException {
        String newId = "" + getRandomId();
        component.setId(newId);
        logger.info("Received registration of " + component.getType() + "/" + component.getId() + " under parent " + component.getParentId());
        return newId;
    }
    */

    public Registration registerMuleObject(Registerable parent, Registerable object) throws RegistryException
    {
        return registerMuleObject(parent, object, null);
    }

    public Registration registerMuleObject(Registerable parent, Registerable object, ObjectMetadata metadata) throws RegistryException
    {
        String newId = "" + getRandomId();
        Registration registration = new MuleRegistration();
        registration.setId(newId);
        if (parent != null) registration.setParentId(parent.getId());
        return registration;
    }

    /*
    public void deregisterComponent(ComponentReference component) throws DeregistrationException 
    {
        logger.info("Received deregistration of " + component.getType() + "/" + component.getId());
    }
    */

    public void deregisterComponent(String registryId) throws RegistryException 
    {
        logger.info("Received deregistration of " + registryId);
    }

    /*
    public void reregisterComponent(ComponentReference component) throws ReregistrationException {
        logger.info("Received reregistration of " + component.getType() + "/" + component.getId());
    }
    */

    public Map getRegisteredComponents(String parentId) 
    {
        return null;
    }

    public Map getRegisteredComponents(String parentId, String type) 
    {
        return null;
    }

    public Registration getRegisteredComponent(String id) {
        return null;
    }

    public void start() throws LifecycleException
    {
        logger.info("Starting");
    }

    public void stop() throws LifecycleException
    {
        logger.info("Stopping");
    }

    public void dispose()
    {
        logger.info("Disposing of itself properly - bye bye!");
    }

    public void notifyStateChange(String id, int state) 
    {
        logger.info("Component " + id + " has state changed to " + state);
    }

    public void notifyPropertyChange(String id, String propertyName, Object propertyValue)
    {
    }

    private long getRandomId()
    {
        return generator.nextLong();
    }

    /*
    public ComponentReference getComponentReferenceInstance()
    {
        return new BasicComponentReference();
    }

    public ComponentReference getComponentReferenceInstance(String referenceType)
    {
        return new BasicComponentReference();
    }
    */

    public String getPersistenceMode()
    {
        return new String("NONE");
    }

    ///////////////////////////////////////////////////////////////////////////////////

    public Map getConnectors() throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getEndpointIdentifiers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getEndpoints() throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getModels() throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getTransformers() throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOAgent lookupAgent(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOConnector lookupConnector(String logicalName) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOEndpoint lookupEndpoint(String logicalName) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String lookupEndpointIdentifier(String logicalName, String defaultName) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOInterceptorStack lookupInterceptorStack(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOModel lookupModel(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOTransformer lookupTransformer(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void registerAgent(UMOAgent agent) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public UMOComponent registerComponent(UMODescriptor descriptor, String modelName) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void registerConnector(UMOConnector connector) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void registerEndpoint(UMOEndpoint endpoint) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void registerEndpointIdentifier(String logicalName, String endpoint) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void registerInterceptorStack(String name, UMOInterceptorStack stack) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void registerModel(UMOModel model) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void registerTransformer(UMOTransformer transformer) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void unregisterAgent(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
    }

    public void unregisterConnector(String connectorName) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void unregisterEndpoint(String endpointName) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void unregisterEndpointIdentifier(String logicalName) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void unregisterModel(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void unregisterTransformer(String transformerName) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public void initialise() throws InitialisationException
    {
        // TODO Auto-generated method stub
        
    }

    public void register(Registerable object) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public Map getAgents() throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void unregisterComponent(UMODescriptor descriptor, String modelName) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

    public UMOComponent lookupComponent(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public UMOComponent registerSystemComponent(UMODescriptor descriptor) throws RegistryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void unregisterComponent(String name) throws RegistryException
    {
        // TODO Auto-generated method stub
        
    }

}
