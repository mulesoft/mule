/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;
import java.util.Properties;

/**
 * The Registry interface represents a registry/repository for storing
 * configuration components that can be manipulated at runtime.
 *
 * This Registry is really a facade so that we can implement various
 * other types of registries in the future. 
 */
public interface UMORegistry extends Initialisable, Disposable {

    /**
     * Register a generic object with the registry.
     */
    void register(Registerable object) throws RegistryException;
    
    /**
     * @param logicalName the name of the endpoint to retrieve
     * @return the endpoint instnace if it exists
     */
    UMOConnector lookupConnector(String logicalName) throws RegistryException;

    /**
     * @param logicalName the logical mapping name for an endpointUri i.e. rather
     *            than specifing an endpointUri to be someone@my.com you can supply a
     *            more descriptive name such as <i>The System Administrator</i>
     * @param defaultName
     * @return the actual endpointUri value or null if it is not found
     */
    String lookupEndpointIdentifier(String logicalName, String defaultName) throws RegistryException;

    /**
     * Getter for a global endpoint. Any endpoints returned from this method can be
     * modified, as they are clones of the registered endpoints.
     * 
     * @param logicalName the name of the endpoint
     * @return the <code>UMOEndpoint</code> or null if it doesn't exist
     */
    UMOEndpoint lookupEndpoint(String logicalName) throws RegistryException;

    /**
     * Getter method for a Transformer.
     * 
     * @param name the name of the transformer
     * @return the Transformer instance if found, otherwise null
     */
    UMOTransformer lookupTransformer(String name) throws RegistryException;

    /**
     * Registers a <code>UMOConnector</code> with the <code>MuleManager</code>.
     * 
     * @param connector the <code>UMOConnector</code> to register
     */
    void registerConnector(UMOConnector connector) throws RegistryException;

    /**
     * UnRegisters a <code>UMOConnector</code> with the <code>MuleManager</code>.
     * 
     * @param connectorName the name of the <code>UMOConnector</code> to unregister
     */
    void unregisterConnector(String connectorName) throws RegistryException;

    /**
     * Gets an unmodifiable collection of Connectors registered with the UMOManager
     * 
     * @return All connectors registered on the Manager
     * @see UMOConnector
     */
    Map getConnectors() throws RegistryException;

    /**
     * Registers an endpointUri with a logical name
     * 
     * @param logicalName the name of the endpointUri
     * @param endpoint the physical endpointUri value
     */
    void registerEndpointIdentifier(String logicalName, String endpoint) throws RegistryException;

    /**
     * unregisters an endpointUri with a logical name
     * 
     * @param logicalName the name of the endpointUri
     */
    void unregisterEndpointIdentifier(String logicalName) throws RegistryException;

    /**
     * Registers a shared/global endpoint with the <code>MuleManager</code>.
     * 
     * @param endpoint the <code>UMOEndpoint</code> to register.
     */
    void registerEndpoint(UMOEndpoint endpoint) throws RegistryException;

    /**
     * unregisters a shared/global endpoint with the <code>MuleManager</code>.
     * 
     * @param endpointName the <code>UMOEndpoint</code> name to unregister.
     */
    void unregisterEndpoint(String endpointName) throws RegistryException;

    /**
     * Gets an unmodifiable collection of endpoints registered with the UMOManager
     * 
     * @return All endpoints registered on the Manager
     */
    Map getEndpointIdentifiers();

    /**
     * Registers a transformer with the <code>MuleManager</code>.
     * 
     * @param transformer the <code>UMOTransformer</code> to register.
     */
    void registerTransformer(UMOTransformer transformer) throws RegistryException;

    /**
     * UnRegisters a transformer with the <code>MuleManager</code>.
     * 
     * @param transformerName the <code>UMOTransformer</code> name to register.
     */
    void unregisterTransformer(String transformerName) throws RegistryException;

    /**
     * Gets an unmodifiable collection of transformers registered with the UMOManager
     * 
     * @return All transformers registered on the Manager
     * @see UMOTransformer
     */
    Map getTransformers() throws RegistryException;

    /**
     * The model used for managing components for this server
     * 
     * @return The model used for managing components for this server
     */
    UMOModel lookupModel(String name) throws RegistryException;

    void registerModel(UMOModel model) throws RegistryException;

    void unregisterModel(String name) throws RegistryException;

    /**
     * Will register an agent object on this model. Agents can be server plugins such
     * as Jms support
     * 
     * @param agent
     */
    void registerAgent(UMOAgent agent) throws RegistryException;

    /**
     * Will find a registered agent using its name, which is unique for all
     * registered agents
     * 
     * @param name the name of the Agent to find
     * @return the Agent or null if there is not agent registered with the given name
     */
    UMOAgent lookupAgent(String name) throws RegistryException;

    /**
     * Removes and destroys a registered agent
     * 
     * @param name the agent name
     * @return the destroyed agent or null if the agent doesn't exist
     */
    void unregisterAgent(String name) throws RegistryException;

    /**
     * Gets an unmodifiable collection of agents registered with the UMOManager
     * 
     * @return All agents registered on the Manager
     */
    Map getAgents() throws RegistryException;
    
    /**
     * Gets an unmodifiable collection of models registered with the UMOManager
     * 
     * @return All models registered on the Manager
     */
     Map getModels() throws RegistryException;
     
     /**
      * Gets an unmodifiable collection of endpoints registered with the UMOManager
      * 
      * @return All endpoints registered on the Manager
      * @see org.mule.umo.endpoint.UMOEndpoint
      */
     Map getEndpoints() throws RegistryException;

     /**
      * Registers an Component with the specified Model.
      */
     UMOComponent registerComponent(UMODescriptor descriptor, String modelName) throws RegistryException;
     
     /**
      * Registers an Component with the System model.
      */
     UMOComponent registerSystemComponent(UMODescriptor descriptor) throws RegistryException;
     
     /**
      * Unregisters a Component.
      */
     void unregisterComponent(String name) throws RegistryException;

     UMOComponent lookupComponent(String name) throws RegistryException;
     
     /**
      * registers a interceptor stack list that can be referenced by other components
      * 
      * @param name the referenceable name for this stack
      * @param stack a List of interceptors
      * @see org.mule.umo.UMOInterceptor
      * @deprecated Interceptors are going away for Mule 2.0
      */
     void registerInterceptorStack(String name, UMOInterceptorStack stack) throws RegistryException;

     /**
      * Retrieves a configured interceptor stack.
      * 
      * @param name the name of the stack
      * @return the interceptor stack requested or null if there wasn't one configured
      *         for the given name
      * @deprecated Interceptors are going away for Mule 2.0
      */
     UMOInterceptorStack lookupInterceptorStack(String name) throws RegistryException;

     /**
      * Searches for and returns the ServiceDescriptor for a transport, model, or any other entity.
      * 
      * @return ServiceDescriptor or null if ServiceDescriptor not found.
      */
     ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException;

    ///////////////////////////////////////////////////////////////////////////////

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
    Registration registerMuleObject(Registerable parent, Registerable object) throws RegistryException;

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
    void deregisterComponent(String registryId) throws RegistryException;

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
