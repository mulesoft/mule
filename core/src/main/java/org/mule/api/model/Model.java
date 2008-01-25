/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.model;

import org.mule.api.NamedObject;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleAdapterFactory;

import java.beans.ExceptionListener;

/**
 * The <code>Model</code> encapsulates and manages the runtime behaviour of a
 * Mule Server instance. It is responsible for maintaining the UMOs instances and
 * their configuration.
 */
public interface Model extends Lifecycle, MuleContextAware, NamedObject
{
    /**
     * Returns the model type name. This is a friendly identifier that is used to
     * look up the SPI class for the model
     *
     * @return the model type
     */
    String getType();

    /**
     * The entry point resolver is used to determine the method to be called on a
     * service when an event is received for it.
     *
     * @return Returns the entryPointResolver.
     */
    EntryPointResolverSet getEntryPointResolverSet();

    /**
     * This will be used to build entry points on the components registered with the
     * model.
     *
     * @param entryPointResolver The entryPointResolver to set. This will be used to
     *                           build entry points on the components registered with the model.
     */
    void setEntryPointResolverSet(EntryPointResolverSet entryPointResolver);

//    /**
//     * Registers a <code>UMODescriptor</code> with the <code>MuleManager</code>.
//     * The manager will take care of creating the Mule UMO and, it's service and
//     * proxies.
//     *
//     * @param descriptor the <code>UMODescriptor</code> to register
//     */
//    void registerComponent(UMODescriptor descriptor) throws MuleException;
//
//    /**
//     * Unregisters a service From the model
//     *
//     * @param descriptor the descriptor of the componnt to remove
//     * @throws MuleException if the service is not registered or it failed to be
//     *                      disposing or the descriptor is null
//     */
//    void unregisterComponent(UMODescriptor descriptor) throws MuleException;
//
//    /**
//     * Determines if a UMO service descriptor by the given name is regestered with
//     * the model
//     *
//     * @param name the name of the UMO
//     * @return true if the UMO's descriptor has benn registered with the model
//     * @see UMODescriptor
//     */
//    boolean isComponentRegistered(String name);

    /**
     * The lifecycle adapter is used by the model to translate Mule lifecycle event
     * to events that UMO components registered with the model understand. The
     * <code>LifecycleAdapterFactory</code> is used by the model to instanciate
     * LifecycleAdapters.
     *
     * @return Returns the lifecycleAdapterFactory used by this Model.
     * @see LifecycleAdapterFactory
     * @see org.mule.api.lifecycle.LifecycleAdapter
     */
    LifecycleAdapterFactory getLifecycleAdapterFactory();

    /**
     * Sets the lifecycleAdapterFactory on the model.
     *
     * @param lifecycleAdapterFactory The lifecycleAdapterFactory to set on this
     *                                model.
     * @see LifecycleAdapterFactory
     * @see org.mule.api.lifecycle.LifecycleAdapter
     */
    void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory);

//    /**
//     * Returns the Service for the given Mule name.
//     *
//     * @param muleName the Name of the Mule Service to obtain a session for
//     * @return a MuleSession for the given name or null if the service is not
//     *         registered
//     */
//    MuleSession getComponentSession(String muleName);
//
//    /**
//     * A convenience method to set a list of components on the model. This method
//     * will most likely be used when the model is being constructed from an IoC
//     * container
//     *
//     * @param descriptors
//     * @throws MuleException
//     */
//    void setServiceDescriptors(List descriptors) throws MuleException;

    /**
     * The exception strategy to use by components managed by the model. The
     * exception strategy is used when an exception occurs while processing the
     * current event for a service. A service can define it's own exception
     * strategy, but if it doesn't this implmentation will be used.
     *
     * @return the default exception strategy for this model.
     * @see ExceptionListener
     */
    ExceptionListener getExceptionListener();

    /**
     * The exception strategy to use by components managed by the model. The
     * exception strategy is used when an exception occurs while processing the
     * current event for a service. A service can define it's own exception
     * strategy, but if it doesn't this implmentation will be used.
     *
     * @param listener the default exception strategy for this model.
     * @see ExceptionListener
     */
    void setExceptionListener(ExceptionListener listener);

    /**
     * Returns a descriptor for the given service name
     *
     * @param name the name of the service
     * @return a descriptor for the given service name or null if there is no
     *         service registered by that name
     * @see UMODescriptor
     */
    //UMODescriptor getDescriptor(String name);

    /**
     * Returns the Service object for the given service name
     *
     * @param name the name of the service
     * @return the Service object for the given service name or null if there
     *         is no service registered by that name
     * @see Service
     */
    //Service getComponent(String name);

//    /**
//     * Stops a single Mule Service. This can be useful when stopping and starting
//     * some Mule UMOs while letting others continue. When a service is stopped all
//     * listeners for that service are unregistered.
//     *
//     * @param name the name of the Mule UMO to stop
//     * @throws MuleException if the MuleUMO is not registered or the service failed
//     *                      to stop
//     */
//    void stopComponent(String name) throws MuleException;
//
//    /**
//     * Starts a single Mule Service. This can be useful when stopping and starting
//     * some Mule UMOs while letting others continue.
//     *
//     * @param name the name of the Mule UMO to start
//     * @throws MuleException if the MuleUMO is not registered or the service failed
//     *                      to start
//     */
//    void startComponent(String name) throws MuleException;
//
//    /**
//     * Pauses event processing for a single Mule Service. Unlike stopComponent(), a
//     * paused service will still consume messages from the underlying transport,
//     * but those messages will be queued until the service is resumed. In order to
//     * persist these queued messages you can set the 'recoverableMode' property on
//     * the Muleconfiguration to true. this causes all internal queues to store their
//     * state.
//     *
//     * @param name the name of the Mule UMO to stop
//     * @throws MuleException if the MuleUMO is not registered or the service failed
//     *                      to pause.
//     * @see org.mule.config.MuleConfiguration
//     */
//    void pauseComponent(String name) throws MuleException;
//
//    /**
//     * Resumes a single Mule Service that has been paused. If the service is not
//     * paused nothing is executed.
//     *
//     * @param name the name of the Mule UMO to resume
//     * @throws MuleException if the MuleUMO is not registered or the service failed
//     *                      to resume
//     */
//    void resumeComponent(String name) throws MuleException;
//
//    /**
//     * Gets an iterator of all service names registered in the model
//     *
//     * @return an iterator of all service names
//     */
//    Iterator getComponentNames();
}
