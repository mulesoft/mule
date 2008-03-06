/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.lifecycle;

import org.mule.RequestContext;
import org.mule.VoidResult;
import org.mule.api.interceptor.Invocation;
import org.mule.api.MuleException;
import org.mule.api.MuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleAdapter;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.routing.NestedRouter;
import org.mule.api.service.Service;
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

    private Object pojoService;
    private Service service;

    private boolean started = false;
    private boolean disposed = false;

    private EntryPointResolverSet entryPointResolver;

    public DefaultLifecycleAdapter(Object pojoService, Service service) throws MuleException
    {
        this(pojoService, service, new LegacyEntryPointResolverSet());
    }

    public DefaultLifecycleAdapter(Object pojoService,
                                   Service service,
                                   EntryPointResolverSet epResolver) throws MuleException
    {
        initialise(pojoService, service, epResolver);
    }

    protected void initialise(Object pojoService, Service service, EntryPointResolverSet entryPointResolver)
            throws MuleException
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
        this.service = service;
        this.entryPointResolver = entryPointResolver;
        if (pojoService instanceof ServiceAware)
        {
            ((ServiceAware) pojoService).setService(service);
        }
        configureNestedRouter();
    }

    public LifecycleTransitionResult start() throws MuleException
    {
        try
        {
            return LifecycleTransitionResult.startOrStopAll(service.start(), new LifecycleTransitionResult.Closure()
            {
                public LifecycleTransitionResult doContinue()
                {
                    started = true;
                    return LifecycleTransitionResult.OK;
                }});
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(
                    CoreMessages.failedToStart("UMO Service: " + service.getName()), e);
        }
    }

    public LifecycleTransitionResult stop() throws MuleException
    {
        try
        {
            return LifecycleTransitionResult.startOrStopAll(service.stop(), new LifecycleTransitionResult.Closure()
            {
                public LifecycleTransitionResult doContinue()
                {
                    started = false;
                    return LifecycleTransitionResult.OK;
                }});
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(
                    CoreMessages.failedToStop("UMO Service: " + service.getName()), e);
        }
    }

    public void dispose()
    {
        try
        {
            service.dispose();
        }
        catch (Exception e)
        {
            // TODO MULE-863: Handle or fail
            logger.error("failed to dispose: " + service.getName(), e);
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

    public void handleException(Object message, Exception e)
    {
        service.getExceptionListener().exceptionThrown(e);
    }

    // Note: Invocation argument is not even used!
    public MuleMessage intercept(Invocation invocation) throws MuleException
    {
        // Invoke method
        Object result;
        MuleEvent event = RequestContext.getEvent();

        try
        {
            //Use the overriding entrypoint resolver if one is set
            if (service.getEntryPointResolverSet() != null)
            {
                result = service.getEntryPointResolverSet().invoke(pojoService, RequestContext.getEventContext());

            }
            else
            {
                result = entryPointResolver.invoke(pojoService, RequestContext.getEventContext());
            }
        }
        catch (Exception e)
        {
            // should all Exceptions caught here be a ServiceException?!?
            // TODO MULE-863: See above
            throw new ServiceException(RequestContext.getEventContext().getMessage(), service, e);
        }

        MuleMessage resultMessage = null;
        if (result instanceof VoidResult)
        {
            //This will rewire the current message
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
                        Collections.singletonList(
                                new TransformerTemplate(
                                        new TransformerTemplate.OverwitePayloadCallback(result))));
                resultMessage = event.getMessage();
            }
        }
        return resultMessage;
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return service.initialise();
    }

    protected void configureNestedRouter() throws MuleException
    {
        // Initialise the nested router and bind the endpoints to the methods using a Proxy
        if (service.getNestedRouter() != null)
        {
            Map bindings = new HashMap();
            for (Iterator it = service.getNestedRouter().getRouters().iterator(); it.hasNext();)
            {
                NestedRouter nestedRouter = (NestedRouter) it.next();
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

    public Service getService()
    {
        return service;
    }
}
