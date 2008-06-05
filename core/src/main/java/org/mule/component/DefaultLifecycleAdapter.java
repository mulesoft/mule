/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.RequestContext;
import org.mule.VoidResult;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.interceptor.Invocation;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.routing.NestedRouter;
import org.mule.api.service.ServiceAware;
import org.mule.api.service.ServiceException;
import org.mule.config.i18n.CoreMessages;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.model.resolvers.NoSatisfiableMethodsException;
import org.mule.model.resolvers.TooManySatisfiableMethodsException;
import org.mule.routing.nested.NestedInvocationHandler;
import org.mule.transformer.TransformerTemplate;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
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
public class DefaultLifecycleAdapter implements LifecycleAdapter
{
    /** logger used by this class */
    protected static final Log logger = LogFactory.getLog(DefaultLifecycleAdapter.class);

    protected Object componentObject;
    protected JavaComponent component;
    private boolean isStoppable = false;
    private boolean isStartable = false;
    private boolean isDisposable = false;

    private boolean started = false;
    private boolean disposed = false;

    private EntryPointResolverSet entryPointResolver;

    public DefaultLifecycleAdapter(Object componentObject, JavaComponent component) throws MuleException
    {
        this(componentObject, component, new LegacyEntryPointResolverSet());
    }

    public DefaultLifecycleAdapter(Object componentObject,
                                   JavaComponent component,
                                   EntryPointResolverSet entryPointResolver) throws MuleException
    {

        if (componentObject == null)
        {
            throw new IllegalArgumentException("POJO Service cannot be null");
        }
        if (entryPointResolver == null)
        {
            entryPointResolver = new LegacyEntryPointResolverSet();
        }
        this.componentObject = componentObject;
        this.component = component;
        this.entryPointResolver = entryPointResolver;

        isStartable = Startable.class.isInstance(componentObject);
        isStoppable = Stoppable.class.isInstance(componentObject);
        isDisposable = Disposable.class.isInstance(componentObject);

        if (componentObject instanceof ServiceAware)
        {
            ((ServiceAware) componentObject).setService(component.getService());
        }
        configureNestedRouter();
    }

    /**
     * Propagates start() life-cycle to component object implementations if they
     * implement the mule {@link Startable} interface. NOT: It is up to component
     * implementations to ensure their implementation of start() is thread-safe.
     */
    public void start() throws MuleException
    {
        if (isStartable)
        {
            try
            {
                ((Startable) componentObject).start();                
                started = true;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(CoreMessages.failedToStart("UMO Service: "
                                                                          + component.getService().getName()), e);
            }
        }
        else
        {
            started = true;
        }
    }

    /**
     * Propagates stop() life-cycle to component object implementations if they
     * implement the mule {@link Stoppable} interface. NOT: It is up to component
     * implementations to ensure their implementation of stop() is thread-safe.
     */
    public void stop() throws MuleException
    {
        if (isStoppable)
        {
            try
            {
                ((Stoppable) componentObject).stop();
                started = false;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(CoreMessages.failedToStop("UMO Service: "
                                                                         + component.getService().getName()), e);
            }
        }
        else
        {
            started = false;
        }
    }

    /**
     * Propagates dispose() life-cycle to component object implementations if they
     * implement the mule {@link Disposable} interface. NOT: It is up to component
     * implementations to ensure their implementation of dispose() is thread-safe.
     */
    public void dispose()
    {
        if (isDisposable)
        {
            try
            {
                ((Disposable) componentObject).dispose();
            }
            catch (Exception e)
            {
                // TODO MULE-863: Handle or fail
                logger.error("failed to dispose: " + component.getService().getName(), e);
            }
        }
        disposed = true;
    }

    /** @return true if the service has been started */
    public boolean isStarted()
    {
        return started;
    }

    /** @return whether the service managed by this lifecycle has been disposed */
    public boolean isDisposed()
    {
        return disposed;
    }

    // Note: Invocation argument is not even used!
    public MuleMessage intercept(Invocation invocation) throws MuleException
    {
        // Invoke method
        Object result;
        MuleEvent event = RequestContext.getEvent();

        try
        {
            // Use the overriding entrypoint resolver if one is set
            if (component.getEntryPointResolverSet() != null)
            {
                result = component.getEntryPointResolverSet().invoke(componentObject, RequestContext.getEventContext());

            }
            else
            {
                result = entryPointResolver.invoke(componentObject, RequestContext.getEventContext());
            }
        }
        catch (Exception e)
        {
            // should all Exceptions caught here be a ServiceException?!?
            // TODO MULE-863: See above
            throw new ServiceException(RequestContext.getEventContext().getMessage(), component.getService(), e);
        }

        MuleMessage resultMessage = null;
        if (result instanceof VoidResult)
        {
            // This will rewire the current message
            event.transformMessage();
            resultMessage = event.getMessage();
        }
        else if (result != null)
        {
            if (result instanceof MuleMessage)
            {
                resultMessage = (MuleMessage) result;
            }
            else
            {
                event.getMessage().applyTransformers(
                    Collections.singletonList(new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(
                        result))));
                resultMessage = event.getMessage();
            }
        }
        return resultMessage;
    }

    /**
     * Propagates initialise() life-cycle to component object implementations if they
     * implement the mule {@link Initialisable} interface.
     * <p/> 
     * <b>NOTE:</b> It is up to component implementations to ensure their implementation of 
     * <code>initialise()</code> is thread-safe.
     */
    public void initialise() throws InitialisationException
    {
        if (Initialisable.class.isInstance(componentObject))
        {
            ((Initialisable) componentObject).initialise();
        }
    }

    protected void configureNestedRouter() throws MuleException
    {
        // Initialise the nested router and bind the endpoints to the methods using a
        // Proxy
        if (component.getNestedRouter() != null)
        {
            Map bindings = new HashMap();
            for (Iterator it = component.getNestedRouter().getRouters().iterator(); it.hasNext();)
            {
                NestedRouter nestedRouter = (NestedRouter) it.next();
                Object proxy = bindings.get(nestedRouter.getInterface());

                if (proxy == null)
                {
                    // Create a proxy that implements this interface
                    // and just routes away using a mule client
                    // ( using the high level Mule client is probably
                    // a bit agricultural but this is just POC stuff )
                    proxy = nestedRouter.createProxy(componentObject);
                    bindings.put(nestedRouter.getInterface(), proxy);

                    // Now lets set the proxy on the Service object
                    Method setterMethod;

                    List methods = ClassUtils.getSatisfiableMethods(componentObject.getClass(),
                        new Class[]{nestedRouter.getInterface()}, true, false, null);
                    if (methods.size() == 1)
                    {
                        setterMethod = (Method) methods.get(0);
                    }
                    else if (methods.size() > 1)
                    {
                        throw new TooManySatisfiableMethodsException(componentObject.getClass(),
                            new Class[]{nestedRouter.getInterface()});
                    }
                    else
                    {
                        throw new NoSatisfiableMethodsException(componentObject.getClass(),
                            new Class[]{nestedRouter.getInterface()});
                    }

                    try
                    {
                        setterMethod.invoke(componentObject, new Object[]{proxy});
                    }
                    catch (Exception e)
                    {
                        throw new InitialisationException(CoreMessages.failedToSetProxyOnService(nestedRouter,
                            componentObject.getClass()), e, this);
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
