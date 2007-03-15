/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl.store;

import org.mule.persistence.PersistenceHelper;
import org.mule.persistence.PersistenceNotification;
import org.mule.persistence.PersistenceNotificationListener;
import org.mule.registry.DeregistrationException;
import org.mule.registry.Registration;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.registry.RegistryStore;
import org.mule.registry.ReregistrationException;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The InMemoryStore represents an, well, in-memory store of 
 * Registrations.
 */
public class InMemoryStore implements RegistryStore
{
    /**
     * The store is basically a Map of Maps
     */
    private Map store = null;
    
    /**
     * The root allows traversal of a tree
     */
    private Registration root = null;

    /**
     * This is the listener for the persistence manager. When
     * the store wants to, it can alert the persistence manager
     * to schedule persistence of the store.
     */
    private PersistenceNotificationListener notificationListener = null;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(InMemoryStore.class);

    /**
     * This map keeps track of what types this registry stores
     */
    private Map typeList = null;

    /**
     * This is the persistence helper the store needs
     */
    private RegistryPersistenceHelper helper = null;

    public InMemoryStore(Registry registry) 
    {
        helper = new RegistryPersistenceHelper(registry);
        helper.setPersistAll(true);
    }

    public void setPersistAll(boolean persistAll)
    {
        helper.setPersistAll(persistAll);
    }

    public void registerComponent(Registration component) throws RegistrationException
    {
        if (component.getId().equals("0")) 
        {
            logger.info("Setting root");
            root = component;
        }
        else 
        {
            Registration parent = null;

            if (component.getParentId() != null) 
            {
                parent = (Registration)store.get(component.getParentId());
            }
            else
            {
                parent = root;
                component.setParentId(root.getId());
            }

            logger.info("About to add component " + component.getId() + 
                    " (" + component.getProperty("sourceObjectClassName") + ")" +
                    " to parent " + component.getParentId());

            if (parent != null)
            {
                parent.addChild(component);
            }

        }

        logger.info("Received registration of " + component.getType() + "/" + component.getId() + " under parent " + component.getParentId());
        store.put(component.getId(), component);
    }

    public void deregisterComponent(Registration component) throws DeregistrationException
    {
        logger.info("Received deregistration of " + component.getType() + "/" + component.getId());
        Registration ref = 
            (Registration)store.get(component.getId());
        // We will throw an exception here
        if (ref == null) return;
        store.remove(ref);
    }

    public void deregisterComponent(String registryId) throws DeregistrationException
    {
        Registration ref = (Registration)store.get(registryId);
        // We will throw an exception here
        if (ref == null) return;
        store.remove(ref);
    }

    public void reregisterComponent(Registration component) throws ReregistrationException
    {
        Registration ref = 
            (Registration)store.get(component.getId());
        // We will throw an exception here
        if (ref == null) return;
        store.put(ref.getId(), ref);
    }

    public Map getRegisteredComponents(String parentId, String type)
    {
        Map components = new HashMap();
        Iterator iter = store.keySet().iterator();
        while (iter.hasNext())
        {
            String id = (String)iter.next();
            Registration ref = (Registration)store.get(id);
            if (ref.getType().equals(type))
                store.put(ref.getId(), ref);
        }

        return components;
    }

    public Map getRegisteredComponents(String parentId)
    {
        return new HashMap();
    }

    public Registration getRegisteredComponent(String id)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException
    {
        store = new HashMap();
    }

    /**
     * Start the registry store
     */
    public void start() throws UMOException
    {
        logger.info("Started");
    }

    /**
     * Stop the registry store
     */
    public void stop() throws UMOException 
    {
        logger.info("Stopped");
    }

    /**
     * Clean up and release any resources
     */
    public void dispose() 
    {
        // TODO: delete stuff
    }

    public void persist()
    {
        logger.info("Got request to persist");

        if (notificationListener != null)
        {
            notificationListener.onNotification(new PersistenceNotification(this, PersistenceNotification.PERSISTABLE_READY));
        }
        else
        {
            logger.info("No persistence listener registered, so can't persist");
        }
    }

    public void registerPersistenceRequestListener(PersistenceNotificationListener listener) throws UMOException
    {
        logger.info("Registering request listener");
        if (listener instanceof PersistenceNotificationListener)
        {
            notificationListener = (PersistenceNotificationListener)listener;
            logger.info("Registered request listener");
        }
    }

    public Object getPersistableObject() throws UMOException
    {
        return root;
    }

    public PersistenceHelper getPersistenceHelper() 
    {
        return helper;
    }

    public Object getStorageKey() throws UMOException
    {
        return null;
    }

    public Registration getRootObject()
    { 
        return root;
    }
}

