/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.MuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.routing.nested.NestedInvocationHandler;
import org.mule.umo.ComponentException;
import org.mule.umo.Invocation;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.model.UMOEntryPointResolverSet;
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
    /** logger used by this class */
    protected static final Log logger = LogFactory.getLog(DefaultLifecycleAdapter.class);

    private Object pojoService;
    private UMOComponent component;
    private boolean isStoppable = false;
    private boolean isStartable = false;
    private boolean isDisposable = false;

    private boolean started = false;
    private boolean disposed = false;

    private UMOEntryPointResolverSet entryPointResolver;

    public DefaultLifecycleAdapter(Object pojoService, UMOComponent component) throws UMOException
    {
        this(pojoService, component, new LegacyEntryPointResolverSet());
    }

    public DefaultLifecycleAdapter(Object pojoService,
                                   UMOComponent component,
                                   UMOEntryPointResolverSet epResolver) throws UMOException
    {
        initialise(pojoService, component, epResolver);
    }

    protected void initialise(Object pojoService, UMOComponent component, UMOEntryPointResolverSet entryPointResolver)
            throws UMOException
    {
        if (pojoService == null)
        {
            throw new IllegalArgumentException("POJO Service cannot be null");
        }
        if (entryPointResolver == null)
        {
            entryPointResolver = new LegacyEntryPointResolverSet();
        }
        this.pojoService = pojoService;
        this.component = component;
        this.entryPointResolver = entryPointResolver;

        isStartable = Startable.class.isInstance(component);
        isStoppable = Stoppable.class.isInstance(component);
        isDisposable = Disposable.class.isInstance(component);

        if (pojoService instanceof UMOComponentAware)
        {
            ((UMOComponentAware) pojoService).setComponent(component);
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
                    CoreMessages.failedToStart("UMO Component: " + component.getName()), e);
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
                    CoreMessages.failedToStop("UMO Component: " + component.getName()), e);
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
                logger.error("failed to dispose: " + component.getName(), e);
            }
        }
        disposed = true;
    }

    /** @return true if the component has been started */
    public boolean isStarted()
    {
        return started;
    }

    /** @return whether the component managed by this lifecycle has been disposed */
    public boolean isDisposed()
    {
        return disposed;
    }

    public void handleException(Object message, Exception e)
    {
        component.getExceptionListener().exceptionThrown(e);
    }

    // Note: Invocation argument is not even used!
    public UMOMessage intercept(Invocation invocation) throws UMOException
    {
        // Invoke method
        Object result;
        UMOEvent event = RequestContext.getEvent();   // new copy here?

        try
        {
            //Use the overriding entrypoint resolver if one is set
            if (component.getEntryPointResolverSet() != null)
            {
                result = component.getEntryPointResolverSet().invoke(pojoService, RequestContext.getEventContext());

            }
            else
            {
                result = entryPointResolver.invoke(pojoService, RequestContext.getEventContext());
            }
        }
        catch (Exception e)
        {
            // should all Exceptions caught here be a ComponentException?!?
            // TODO MULE-863: See above
            throw new ComponentException(RequestContext.getEventContext().getMessage(), component, e);
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
        if (component.getNestedRouter() != null)
        {
            Map bindings = new HashMap();
            for (Iterator it = component.getNestedRouter().getRouters().iterator(); it.hasNext();)
            {
                UMONestedRouter nestedRouter = (UMONestedRouter) it.next();
                Object proxy = bindings.get(nestedRouter.getInterface());

                if (proxy == null)
                {
                    // Create a proxy that implements this interface
                    // and just routes away using a mule client
                    // ( using the high level Mule client is probably
                    // a bit agricultural but this is just POC stuff )
                    proxy = nestedRouter.createProxy(pojoService);
                    bindings.put(nestedRouter.getInterface(), proxy);

                    //Now lets set the proxy on the Service object
                    Method setterMethod;


                    List methods =
                            ClassUtils.getSatisfiableMethods(pojoService.getClass(),
                                    new Class[]{nestedRouter.getInterface()}, true, false, null);
                    if (methods.size() == 1)
                    {
                        setterMethod = (Method) methods.get(0);
                    }
                    else if (methods.size() > 1)
                    {
                        throw new TooManySatisfiableMethodsException(
                            pojoService.getClass(), new Class[]{nestedRouter.getInterface()});
                    }
                    else
                    {
                        throw new NoSatisfiableMethodsException(
                            pojoService.getClass(), new Class[]{nestedRouter.getInterface()});
                    }

                    try
                    {
                        setterMethod.invoke(pojoService, new Object[]{proxy});
                    }
                    catch (Exception e)
                    {
                        throw new InitialisationException(
                                CoreMessages.failedToSetProxyOnService(nestedRouter,
                                    pojoService.getClass()), e, this);
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

    public UMOComponent getComponent()
    {
        return component;
    }
}
