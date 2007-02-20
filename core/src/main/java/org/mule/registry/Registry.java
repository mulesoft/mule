/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import java.util.Map;

import org.mule.registry.metadata.ObjectMetadata;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;

/**
 * The Registry interface represents a registry/repository for storing
 * configuration components that can be manipulated at runtime.
 *
 * This Registry is really a facade so that we can implement various
 * other types of registries in the future. 
 *
 * @version $Revision: $
 */
public interface Registry extends Startable, Stoppable, Disposable {

    /**
     * Returns a reference to the RegistryStore that is used
     * to do the actual store of component information.
     *
     * @return RegistryStore object
     */
    public RegistryStore getRegistryStore();

    /**
     * Register a component in the registry. The registry will 
     * return a unique id that will be assigned the component for
     * its lifetime. Ids cannot be reused.
     *
     * @return String ID
    String registerComponent(ComponentReference component) throws RegistrationException;
     */

    /**
     * New registration method - just pass in the object, the registry
     * will take care of the rest
     */
    Registration registerMuleObject(Registerable parent, Registerable object) throws RegistrationException;

    /**
     * Testing this - maybe this is a better way to register the Metadata
    Registration registerMuleObject(Registerable parent, Registerable object, ObjectMetadata metadata) throws RegistrationException;

    /**
     * Experimental OSGI registration - not yet tested
    Registrant registerOSGIBundle(Bundle bundle) throws RegistrationException;
     */

    /**
     * Unregister a component
    void deregisterComponent(ComponentReference component) throws DeregistrationException;
     */

    /**
     * Unregister a component by registry ID
     */
    void deregisterComponent(String registryId) throws DeregistrationException;

    /**
     * Re-register a component, but this might not be used. Not sure
     * at present.
    void reregisterComponent(ComponentReference component) throws ReregistrationException;
     */

    /**
     * Get a Map of all registered components of a certain type. 
     * For example, you could call getRegisteredComponents("descriptors")
     * to get a list of all routes
     */
    Map getRegisteredComponents(String parentId, String type);

    /**
     * Get a Map of all registered components that are children
     * of this ID.
     *
     * @param id the parent ID
     * @return Map of components
     */
    Map getRegisteredComponents(String parentId);

    /**
     * Get a specific registered component, based on ID
     *
     * @param id the reference ID
     * @return Registration
     */
    Registration getRegisteredComponent(String id);

    /**
     * Start the registry
     */
    void start() throws UMOException;

    /**
     * Stop the registry
     */
    void stop() throws UMOException;

    /**
     * Clean up and release any resources
     */
    void dispose();

    /**
     * Method to alert the registry when a component state has 
     * changed. This method should be called by listeners that have
     * already been registered to watch the component's state.
     */
    void notifyStateChange(String id, int state);

    /**
     * Method to alert the registry when a component property has
     * changed. Not used yet.
     */
    void notifyPropertyChange(String id, String propertyName, Object propertyValue);

    /**
     * Returns a Registration instance from the factory
     * Utility method so objects don't have to know what objects to
     * create.
    ComponentReference getComponentReferenceInstance();
     */

    /**
     * Returns a ComponentReference instance from the factory
     * based on the reference type.
     * Utility method so objects don't have to know what objects to
     * create.
    ComponentReference getComponentReferenceInstance(String referenceType);
     */

    /**
     * Returns the type of persistence store used by the Registry
     */
    String getPersistenceMode();

}
