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
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Lifecycle;

import java.beans.ExceptionListener;

/**
 * The <code>Model</code> encapsulates and manages the runtime behaviour of a
 * Mule Server instance. It is responsible for maintaining the Service instances and
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

    /**
     * The lifecycle adapter is used by the model to translate Mule lifecycle event
     * to events that components registered with the model understand. The
     * <code>LifecycleAdapterFactory</code> is used by the model to instanciate
     * LifecycleAdapters.
     *
     * @return Returns the lifecycleAdapterFactory used by this Model.
     * @see LifecycleAdapterFactory
     * @see org.mule.api.component.LifecycleAdapter
     */
    LifecycleAdapterFactory getLifecycleAdapterFactory();

    /**
     * Sets the lifecycleAdapterFactory on the model.
     *
     * @param lifecycleAdapterFactory The lifecycleAdapterFactory to set on this
     *                                model.
     * @see LifecycleAdapterFactory
     * @see org.mule.api.component.LifecycleAdapter
     */
    void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory);

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

}
