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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.registry.*;
import org.mule.umo.UMOException;

/**
 * This dummy registry just reports on registration events but
 * doesn't store anything
 *
 * @version $Revision: $
 */
public class DummyRegistry implements Registry {

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

    public String registerComponent(ComponentReference component) throws RegistrationException {
        String newId = "" + getRandomId();
        component.setId(newId);
        logger.info("Received registration of " + component.getType() + "/" + component.getId() + " under parent " + component.getParentId());
        return newId;
    }

    public void deregisterComponent(ComponentReference component) throws DeregistrationException 
    {
        logger.info("Received deregistration of " + component.getType() + "/" + component.getId());
    }

    public void deregisterComponent(String registryId) throws DeregistrationException 
    {
        logger.info("Received deregistration of " + registryId);
    }

    public void reregisterComponent(ComponentReference component) throws ReregistrationException {
        logger.info("Received reregistration of " + component.getType() + "/" + component.getId());
    }

    public Map getRegisteredComponents(String parentId) 
    {
        return null;
    }

    public Map getRegisteredComponents(String parentId, String type) 
    {
        return null;
    }

    public ComponentReference getRegisteredComponent(String id) {
        return null;
    }

    public void start() throws UMOException
    {
        logger.info("Starting");
    }

    public void stop() throws UMOException
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

    public ComponentReference getComponentReferenceInstance()
    {
        return new BasicComponentReference();
    }

    public ComponentReference getComponentReferenceInstance(String referenceType)
    {
        return new BasicComponentReference();
    }

    public String getPersistenceMode()
    {
        return new String("NONE");
    }
}
