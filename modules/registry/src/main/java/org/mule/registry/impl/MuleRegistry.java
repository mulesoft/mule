/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.persistence.PersistenceManager;
import org.mule.persistence.PersistenceNotificationListener;
import org.mule.persistence.manager.ObjectPersistenceManager;
import org.mule.registry.DeregistrationException;
import org.mule.registry.Registration;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.registry.RegistryStore;
import org.mule.registry.impl.store.InMemoryStore;
import org.mule.registry.metadata.MetadataStore;
import org.mule.registry.metadata.MissingMetadataException;
import org.mule.registry.metadata.ObjectMetadata;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The MuleRegistry implements the Registry interface
 */
public class MuleRegistry implements Registry {

    // Temporary
    protected static String[] GETTERS_TO_GET = {
        "java.lang.Boolean", "java.lang.Date", 
        "byte", "java.lang.Byte",
        "double", "java.lang.Double",
        "float", "java.lang.Float",
        "int", "java.lang.Integer",
        "long", "java.lang.Long", 
        "short", "java.lang.Short",
        "java.lang.String", "java.lang.StringBuffer"
    };

    private long counter = 0L;
    private RegistryStore registryStore;
    private HashMap metadata;
    private PersistenceManager persistenceManager;
    private RegistrationFactory registrationFactory;

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
        registryStore = new InMemoryStore(this);
        metadata = new HashMap();

        // TODO for testing only
        //metadata.put(om.getClassName(), om);

        registrationFactory = new RegistrationFactory();
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
        //
        // Really, this should be the ManagementContext
        try
        {
            Registration registration = 
                registrationFactory.getInstance(RegistrationFactory.REF_MULE_COMPONENT);
            registration.setId("0");
            registration.setProperty("sourceObjectClassName", this.getClass().getName());
            registration.setType("Registry");
            registryStore.registerComponent(registration);
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

    public Registration registerMuleObject(Registerable parent, Registerable object) throws RegistrationException
    {
        Registration registration = 
            registrationFactory.getInstance(RegistrationFactory.REF_MULE_COMPONENT);
        String newId = "" + getNewId();
        registration.setId(newId);
        String cn = object.getClass().getName();
        registration.setProperty("sourceObjectClassName", cn);
        int pos = cn.lastIndexOf(".");

        if (pos > -1)
        {
            registration.setType(cn.substring(pos+1));
        }
        else
        {
            registration.setType(cn);
        }

        if (parent != null)
        {
            if (parent.getRegistryId() == null) 
            {
                // We really should throw an exception here
                // but for now lets see where this happens
                logger.warn("Trying to register " + object.getClass().getName() + " but parent " + parent.getClass().getName() + " has no Registration record");
            }
            else 
            {
                registration.setParentId(parent.getRegistryId());
            }
        }

        ObjectMetadata om = null;

        try 
        {
            om = MetadataStore.getObjectMetadata(object.getClass().getName());
        }
        catch (MissingMetadataException mme)
        {
            // Don't throw this yet until we understand what we are 
            // missing
            //throw new RegistrationException(mme.getMessage());
            logger.warn("Trying to register " + object.getClass().getName() + " but no ObjectMetadata found");
        }

        try 
        {
            loadProperties(om, registration, object);
        }
        catch (Exception e)
        {
            logger.warn("Unable to load properties for object " + object.getClass().getName());
        }

        registryStore.registerComponent(registration);
        return registration;
    }

    public void deregisterComponent(String registryId) throws DeregistrationException
    {
        registryStore.deregisterComponent(registryId);
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
    public Registration getRegisteredComponent(String id)
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

    public String getPersistenceMode()
    {
        if (persistenceManager == null) return "NONE";
        return persistenceManager.getStoreType();
    }

    // This is temporary
    private void loadProperties(ObjectMetadata om, Registration registration, Object object) throws Exception
    {

        try {
            Method[] methods = object.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                // We only want getters
                if (!method.getName().startsWith("get")) continue;
                // We only can handle no argument getters
                if (method.getParameterTypes().length > 0) continue;
                // We don't want the registry ID (hasn't been set yet)
                if (method.getName().equals("getRegistryId")) continue;
                // We don't want the registration record
                if (method.getName().equals("getRegistration")) continue;

                String retType = method.getReturnType().getName();
                String name = method.getName().substring(3, 4).toLowerCase() +
                    method.getName().substring(4);

                if (doCapture(retType))
                {
                    Object value = method.invoke(object, null);
                    registration.setProperty(name, value);
                }
                else if (retType.equals("java.util.Map"))
                {
                    Map map = (Map)method.invoke(object, null);
                    Iterator iter = map.keySet().iterator();
                    while (iter.hasNext())
                    {
                        Object key = iter.next();
                        Object val = map.get(key);
                        if (doCapture(val.getClass().getName()))
                        {
                            registration.setProperty(key.toString(), val);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw (e);
        }
    }

    private boolean doCapture(String retType)
    {
        for (int i = 0; i < GETTERS_TO_GET.length; i++)
            if (StringUtils.equals(GETTERS_TO_GET[i], retType)) return true;
        return false;
    }
}
