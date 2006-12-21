/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.model.DynamicEntryPoint;
import org.mule.model.DynamicEntryPointResolver;
import org.mule.umo.ComponentException;
import org.mule.umo.Invocation;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.model.UMOEntryPointResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultLifecycleAdapter</code> provides lifecycle methods for all Mule
 * managed components. It's possible to plugin custom lifecycle adapters, this can
 * provide additional lifecycle methods triggered by an external source.
 */
public class DefaultLifecycleAdapter implements UMOLifecycleAdapter
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DefaultLifecycleAdapter.class);

    private Object component;
    private UMODescriptor descriptor;
    private boolean isStoppable = false;
    private boolean isStartable = false;
    private boolean isDisposable = false;

    private boolean started = false;
    private boolean disposed = false;

    private DynamicEntryPoint entryPoint;

    public DefaultLifecycleAdapter(Object component, UMODescriptor descriptor) throws UMOException
    {
        this(component, descriptor, new DynamicEntryPointResolver());
    }

    public DefaultLifecycleAdapter(Object component,
                                   UMODescriptor descriptor,
                                   UMOEntryPointResolver epResolver) throws UMOException
    {
        initialise(component, descriptor, epResolver);
    }

    protected void initialise(Object component, UMODescriptor descriptor, UMOEntryPointResolver epDiscovery)
        throws UMOException
    {
        if (component == null)
        {
            throw new IllegalArgumentException("Component cannot be null");
        }
        if (descriptor == null)
        {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }
        if (epDiscovery == null)
        {
            epDiscovery = new DynamicEntryPointResolver();
        }
        this.component = component;
        this.entryPoint = (DynamicEntryPoint)epDiscovery.resolveEntryPoint(descriptor);
        this.descriptor = descriptor;

        isStartable = Startable.class.isInstance(component);
        isStoppable = Stoppable.class.isInstance(component);
        isDisposable = Disposable.class.isInstance(component);

        if (component instanceof UMODescriptorAware)
        {
            ((UMODescriptorAware)component).setDescriptor(descriptor);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Startable#start()
     */
    public void start() throws UMOException
    {
        if (isStartable)
        {
            try
            {
                ((Startable)component).start();
            }
            catch (Exception e)
            {
                throw new MuleException(new Message(Messages.FAILED_TO_START_X, "UMO Component: "
                                                                                + descriptor.getName()), e);
            }
        }
        started = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Stoppable#stop()
     */
    public void stop() throws UMOException
    {
        if (isStoppable)
        {
            try
            {
                ((Stoppable)component).stop();
            }
            catch (Exception e)
            {
                throw new MuleException(new Message(Messages.FAILED_TO_STOP_X, "UMO Component: "
                                                                               + descriptor.getName()), e);
            }
        }
        started = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Disposable#dispose()
     */
    public void dispose()
    {
        if (isDisposable)
        {
            try
            {
                ((Disposable)component).dispose();
            }
            catch (Exception e)
            {
                logger.error("failed to dispose: " + descriptor.getName(), e);
            }
        }
        disposed = true;
    }

    /**
     * @return true if the component has been started
     */
    public boolean isStarted()
    {
        return started;
    }

    /**
     * @return whether the component managed by this lifecycle has been disposed
     */
    public boolean isDisposed()
    {
        return disposed;
    }

    public UMODescriptor getDescriptor()
    {
        return descriptor;
    }

    public void handleException(Object message, Exception e)
    {
        descriptor.getExceptionListener().exceptionThrown(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOInterceptor#intercept(org.mule.umo.UMOEvent)
     */
    public UMOMessage intercept(Invocation invocation) throws UMOException
    {
        // Invoke method
        Object result;
        UMOEvent event = RequestContext.getEvent();

        try
        {
            result = entryPoint.invoke(component, RequestContext.getEventContext());
        }
        catch (Exception e)
        {
            // should all Exceptions caught here be a ComponentException?!?
            throw new ComponentException(new Message(Messages.FAILED_TO_INVOKE_X, component.getClass()
                .getName()), invocation.getMessage(), event.getComponent(), e);
        }

        UMOMessage resultMessage = null;
        if (result == null && entryPoint.isVoid())
        {
            resultMessage = new MuleMessage(event.getTransformedMessage(), RequestContext.getEventContext()
                .getMessage());
        }
        else if (result != null)
        {
            if (result instanceof UMOMessage)
            {
                resultMessage = (UMOMessage)result;
            }
            else
            {
                resultMessage = new MuleMessage(result, event.getMessage());
            }
        }
        return resultMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException
    {
        if (Initialisable.class.isInstance(component))
        {
            ((Initialisable)component).initialise();
        }
    }
}
