/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.persistence.PersistenceManager;
import org.mule.persistence.PersistenceNotificationListener;
import org.mule.persistence.manager.ObjectPersistenceManager;
import org.mule.registry.ComponentReference;
import org.mule.registry.ComponentReferenceFactory;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.registry.RegistryStore;
import org.mule.registry.ReregistrationException;
import org.mule.registry.impl.store.InMemoryStore;
import org.mule.umo.UMOException;

/**
 * The MuleRegistry implements the Registry interface
 */
public class MuleRegistry implements Registry {

    private long counter = 0L;
    private RegistryStore registryStore;
    private PersistenceManager persistenceManager;
    private ComponentReferenceFactory referenceFactory;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(MuleRegistry.class);

    private String registryId = null;

    /**
     * 
     */
    public MuleRegistry() 
    {
        persistenceManager = new ObjectPersistenceManager();
        registryStore = new InMemoryStore();
        referenceFactory = new ComponentReferenceFactoryImpl();
        PersistenceNotificationListener listener = 
            new PersistenceNotificationListener(persistenceManager);

        try {
            persistenceManager.initialise();
        } catch (Exception e) { 
            logger.info("Unable to initialize PersistenceManager: " + 
                    e.toString());
        }

        try {
            registryStore.initialise();
            registryStore.registerPersistenceRequestListener(listener);
        } catch (Exception e) { 
            logger.info(e);
        }

        // The registry will, for now, be the "root" component reference
        // and will have an id of 0
        try
        {
            ComponentReference ref = getComponentReferenceInstance();
            ref.setParentId(null);
            ref.setType("Registry");
            ref.setComponent(this);
            ref.setId("0");
            registryStore.registerComponent(ref);
        }
        catch (Exception e)
        {
            logger.error("Unable to store the registry ComponentReference - no data can be persisted");
        }

    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#getRegistryStore
     */
    public RegistryStore getRegistryStore() 
    {
        return registryStore;
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#registerComponent
     */
    public String registerComponent(ComponentReference component) throws RegistrationException 
    {
        String newId = "" + getNewId();
        component.setId(newId);
        registryStore.registerComponent(component);
        return newId;
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#deregisterComponent
     */
    public void deregisterComponent(ComponentReference component) throws DeregistrationException
    {
        registryStore.deregisterComponent(component);
    }

    public void deregisterComponent(String registryId) throws DeregistrationException
    {
        registryStore.deregisterComponent(registryId);
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#reregisterComponent
     */
    public void reregisterComponent(ComponentReference component) throws ReregistrationException
    {
        registryStore.reregisterComponent(component);
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#getRegisteredComponents
     */
    public Map getRegisteredComponents(String parentId)
    {
        return registryStore.getRegisteredComponents(parentId);
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#getRegisteredComponents
     */
    public Map getRegisteredComponents(String parentId, String type)
    {
        return registryStore.getRegisteredComponents(parentId, type);
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#getRegisteredComponent
     */
    public ComponentReference getRegisteredComponent(String id)
    {
        return registryStore.getRegisteredComponent(id);
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#start
     */
    public void start() throws UMOException
    {
        counter = System.currentTimeMillis();
        persistenceManager.start();
        registryStore.start();

        logger.info("Started");
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#stop
     */
    public void stop() throws UMOException
    {
        // Stop the persistence manager first, in case it is
        // in the middle of requesting something from the store
        persistenceManager.stop();
        registryStore.stop();
        logger.info("Stopped");
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#dispose
     */
    public void dispose()
    {
        registryStore.dispose();
        persistenceManager.dispose();
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#notifyStateChange
     */
    public void notifyStateChange(String id, int state)
    {
        logger.info("Component " + id + " has state changed to " + state);

        // This is for testing only
        if (state == 206)
        {
            registryStore.persist();
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see org.mule.registry.Registry#notifyPropertyChange
     */
    public void notifyPropertyChange(String id, String propertyName, Object propertyValue)
    {
    }

    private long getNewId()
    {
        synchronized (this)
        {
            counter++;
            return counter;
        }
    }

    public ComponentReference getComponentReferenceInstance()
    {
        return referenceFactory.getInstance();
    }

    public ComponentReference getComponentReferenceInstance(String referenceType)
    {
        return referenceFactory.getInstance(referenceType);
    }

    public String getPersistenceMode()
    {
        if (persistenceManager == null) return "NONE";
        return persistenceManager.getStoreType();
    }
}
