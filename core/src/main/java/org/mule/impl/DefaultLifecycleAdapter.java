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
import org.mule.impl.model.resolvers.DynamicEntryPoint;
import org.mule.impl.model.resolvers.DynamicEntryPointResolver;
import org.mule.routing.nested.NestedInvocationHandler;
import org.mule.umo.ComponentException;
import org.mule.umo.Invocation;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.routing.UMONestedRouter;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        this.entryPoint = (DynamicEntryPoint) epDiscovery.resolveEntryPoint(descriptor);
        this.descriptor = descriptor;

        isStartable = Startable.class.isInstance(component);
        isStoppable = Stoppable.class.isInstance(component);
        isDisposable = Disposable.class.isInstance(component);

        if (component instanceof UMODescriptorAware)
        {
            ((UMODescriptorAware) component).setDescriptor(descriptor);
        }
        configureNestedRouter();
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
                ((Startable) component).start();
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
                ((Stoppable) component).stop();
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
                ((Disposable) component).dispose();
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
                resultMessage = (UMOMessage) result;
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
    public void initialise(UMOManagementContext managementContext) throws InitialisationException
    {
        if (Initialisable.class.isInstance(component))
        {
            ((Initialisable) component).initialise(managementContext);
        }
    }

    protected void configureNestedRouter() throws UMOException
    {
        // Initialise the nested router and bind the endpoints to the methods using a Proxy
        if (descriptor.getNestedRouter() != null)
        {
            Map bindings = new HashMap();
            for (Iterator it = descriptor.getNestedRouter().getRouters().iterator(); it.hasNext();)
            {
                UMONestedRouter nestedRouter = (UMONestedRouter) it.next();
                Object proxy = bindings.get(nestedRouter.getInterface());

                if (proxy == null)
                {
                    // Create a proxy that implements this interface
                    // and just routes away using a mule client
                    // ( using the high level Mule client is probably
                    // a bit agricultural but this is just POC stuff )
                    proxy = nestedRouter.createProxy(component);
                    bindings.put(nestedRouter.getInterface(), proxy);

                    //Now lets set the proxy on the Service object
                    Method setterMethod;


                    List methods = ClassUtils.getSatisfiableMethods(component.getClass(), new Class[]{nestedRouter.getInterface()}, true, false, null);
                    if(methods.size()==1)
                    {
                        setterMethod = (Method)methods.get(0);
                    }
                    else if(methods.size() > 1)
                    {
                        throw new TooManySatisfiableMethodsException(component.getClass(), new Class[]{nestedRouter.getInterface()});
                    }
                    else
                    {
                        throw new NoSatisfiableMethodsException(component.getClass(), nestedRouter.getInterface());
                    }

                    try
                    {
                        setterMethod.invoke(component, new Object[]{proxy});
                    }
                    catch (Exception e)
                    {
                        throw new InitialisationException(new Message(Messages.FAILED_TO_SET_PROXY_X_ON_SERVICE_X,
                                nestedRouter, component.getClass().getName()), this);
                    }

                }
                else
                {
                    NestedInvocationHandler handler = (NestedInvocationHandler) Proxy.getInvocationHandler(proxy);
                    handler.addRouterForInterface(nestedRouter);
                }
            }
        }
    }
}
