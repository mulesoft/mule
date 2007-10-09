/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.model;

import org.mule.impl.ManagementContextAware;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;

import java.beans.ExceptionListener;

/**
 * The <code>UMOModel</code> encapsulates and manages the runtime behaviour of a
 * Mule Server instance. It is responsible for maintaining the UMOs instances and
 * their configuration.
 */
public interface UMOModel extends Lifecycle, Initialisable, ManagementContextAware
{
    /**
     * Sets the model's name. It is poosible to configure more than one model in a
     * config file. The name can then be used to reference the Model use when
     * starting the server
     *
     * @param name the model's name
     */
    void setName(String name);

    /**
     * The model's name. It is poosible to configure more than one model in a config
     * file. The name can then be used to reference the Model use when starting the
     * server
     *
     * @return the model's name
     */
    String getName();

    /**
     * Returns the model type name. This is a friendly identifier that is used to
     * look up the SPI class for the model
     *
     * @return the model type
     */
    String getType();

    /**
     * The entry point resolver is used to determine the method to be called on a
     * component when an event is received for it.
     *
     * @return Returns the entryPointResolver.
     */
    UMOEntryPointResolverSet getEntryPointResolverSet();

    /**
     * This will be used to build entry points on the components registered with the
     * model.
     *
     * @param entryPointResolver The entryPointResolver to set. This will be used to
     *                           build entry points on the components registered with the model.
     */
    void setEntryPointResolverSet(UMOEntryPointResolverSet entryPointResolver);

//    /**
//     * Registers a <code>UMODescriptor</code> with the <code>MuleManager</code>.
//     * The manager will take care of creating the Mule UMO and, it's component and
//     * proxies.
//     *
//     * @param descriptor the <code>UMODescriptor</code> to register
//     */
//    void registerComponent(UMODescriptor descriptor) throws UMOException;
//
//    /**
//     * Unregisters a component From the model
//     *
//     * @param descriptor the descriptor of the componnt to remove
//     * @throws UMOException if the component is not registered or it failed to be
//     *                      disposing or the descriptor is null
//     */
//    void unregisterComponent(UMODescriptor descriptor) throws UMOException;
//
//    /**
//     * Determines if a UMO component descriptor by the given name is regestered with
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
     * <code>UMOLifecycleAdapterFactory</code> is used by the model to instanciate
     * LifecycleAdapters.
     *
     * @return Returns the lifecycleAdapterFactory used by this Model.
     * @see UMOLifecycleAdapterFactory
     * @see org.mule.umo.lifecycle.UMOLifecycleAdapter
     */
    UMOLifecycleAdapterFactory getLifecycleAdapterFactory();

    /**
     * Sets the lifecycleAdapterFactory on the model.
     *
     * @param lifecycleAdapterFactory The lifecycleAdapterFactory to set on this
     *                                model.
     * @see UMOLifecycleAdapterFactory
     * @see org.mule.umo.lifecycle.UMOLifecycleAdapter
     */
    void setLifecycleAdapterFactory(UMOLifecycleAdapterFactory lifecycleAdapterFactory);

//    /**
//     * Returns the Component for the given Mule name.
//     *
//     * @param muleName the Name of the Mule Component to obtain a session for
//     * @return a UMOSession for the given name or null if the component is not
//     *         registered
//     */
//    UMOSession getComponentSession(String muleName);
//
//    /**
//     * A convenience method to set a list of components on the model. This method
//     * will most likely be used when the model is being constructed from an IoC
//     * container
//     *
//     * @param descriptors
//     * @throws UMOException
//     */
//    void setServiceDescriptors(List descriptors) throws UMOException;

    /**
     * The exception strategy to use by components managed by the model. The
     * exception strategy is used when an exception occurs while processing the
     * current event for a component. A component can define it's own exception
     * strategy, but if it doesn't this implmentation will be used.
     *
     * @return the default exception strategy for this model.
     * @see ExceptionListener
     */
    ExceptionListener getExceptionListener();

    /**
     * The exception strategy to use by components managed by the model. The
     * exception strategy is used when an exception occurs while processing the
     * current event for a component. A component can define it's own exception
     * strategy, but if it doesn't this implmentation will be used.
     *
     * @param listener the default exception strategy for this model.
     * @see ExceptionListener
     */
    void setExceptionListener(ExceptionListener listener);

    /**
     * Returns a descriptor for the given component name
     *
     * @param name the name of the component
     * @return a descriptor for the given component name or null if there is no
     *         component registered by that name
     * @see UMODescriptor
     */
    //UMODescriptor getDescriptor(String name);

    /**
     * Returns the UMOComponent object for the given component name
     *
     * @param name the name of the component
     * @return the UMOComponent object for the given component name or null if there
     *         is no component registered by that name
     * @see UMOComponent
     */
    //UMOComponent getComponent(String name);

//    /**
//     * Stops a single Mule Component. This can be useful when stopping and starting
//     * some Mule UMOs while letting others continue. When a component is stopped all
//     * listeners for that component are unregistered.
//     *
//     * @param name the name of the Mule UMO to stop
//     * @throws UMOException if the MuleUMO is not registered or the component failed
//     *                      to stop
//     */
//    void stopComponent(String name) throws UMOException;
//
//    /**
//     * Starts a single Mule Component. This can be useful when stopping and starting
//     * some Mule UMOs while letting others continue.
//     *
//     * @param name the name of the Mule UMO to start
//     * @throws UMOException if the MuleUMO is not registered or the component failed
//     *                      to start
//     */
//    void startComponent(String name) throws UMOException;
//
//    /**
//     * Pauses event processing for a single Mule Component. Unlike stopComponent(), a
//     * paused component will still consume messages from the underlying transport,
//     * but those messages will be queued until the component is resumed. In order to
//     * persist these queued messages you can set the 'recoverableMode' property on
//     * the Muleconfiguration to true. this causes all internal queues to store their
//     * state.
//     *
//     * @param name the name of the Mule UMO to stop
//     * @throws UMOException if the MuleUMO is not registered or the component failed
//     *                      to pause.
//     * @see org.mule.config.MuleConfiguration
//     */
//    void pauseComponent(String name) throws UMOException;
//
//    /**
//     * Resumes a single Mule Component that has been paused. If the component is not
//     * paused nothing is executed.
//     *
//     * @param name the name of the Mule UMO to resume
//     * @throws UMOException if the MuleUMO is not registered or the component failed
//     *                      to resume
//     */
//    void resumeComponent(String name) throws UMOException;
//
//    /**
//     * Gets an iterator of all component names registered in the model
//     *
//     * @return an iterator of all component names
//     */
//    Iterator getComponentNames();
}
