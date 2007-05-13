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
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.model.resolvers.DynamicEntryPoint;
import org.mule.impl.model.resolvers.DynamicEntryPointResolver;
import org.mule.routing.nested.NestedInvocationHandler;
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
                throw new MuleException(
                    CoreMessages.failedToStart("UMO Component: " + descriptor.getName()), e);
            }
        }
        started = true;
    }

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
                throw new MuleException(
                    CoreMessages.failedToStop("UMO Component: " + descriptor.getName()), e);
            }
        }
        started = false;
    }

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
                // TODO MULE-863: Handle or fail
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
            // TODO MULE-863: See above
            throw new ComponentException(
                CoreMessages.failedToInvoke(component.getClass().getName()),
                invocation.getMessage(), event.getComponent(), e);
        }

        UMOMessage resultMessage = null;
        if (result instanceof VoidResult)
        {
            resultMessage = new MuleMessage(event.getTransformedMessage(), 
                RequestContext.getEventContext().getMessage());
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

    public void initialise() throws InitialisationException
    {
        if (Initialisable.class.isInstance(component))
        {
            ((Initialisable) component).initialise();
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


                    List methods = 
                        ClassUtils.getSatisfiableMethods(component.getClass(), 
                            new Class[]{nestedRouter.getInterface()}, true, false, null);
                    if (methods.size() == 1)
                    {
                        setterMethod = (Method) methods.get(0);
                    }
                    else if (methods.size() > 1)
                    {
                        throw new TooManySatisfiableMethodsException(
                                component.getClass(), new Class[]{nestedRouter.getInterface()});
                    }
                    else
                    {
                        throw new NoSatisfiableMethodsException(
                                component.getClass(), nestedRouter.getInterface());
                    }

                    try
                    {
                        setterMethod.invoke(component, new Object[]{proxy});
                    }
                    catch (Exception e)
                    {
                        throw new InitialisationException(
                            CoreMessages.failedToSetProxyOnService(nestedRouter, 
                                component.getClass()), e, this);
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
