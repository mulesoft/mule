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

import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.object.ObjectFactory;
import org.mule.api.routing.NestedRouterCollection;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.routing.nested.DefaultNestedRouterCollection;
import org.mule.transport.NullPayload;

import java.util.Collection;
import java.util.Iterator;

/**
 * Abstract implementation of JavaComponent adds JavaComponent specific's:
 * {@link EntryPointResolverSet}, {@link NestedRouterCollection} and
 * {@link ObjectFactory}. Provides default implementations of doOnCall and doOnEvent
 * and defines abstract template methods provided for obtaining and returning the
 * component object instance.
 */
public abstract class AbstractJavaComponent extends AbstractComponent implements JavaComponent
{

    protected EntryPointResolverSet entryPointResolverSet;

    protected NestedRouterCollection nestedRouter = new DefaultNestedRouterCollection();

    protected ObjectFactory objectFactory;

    protected LifecycleAdapterFactory lifecycleAdapterFactory;

    public AbstractJavaComponent()
    {
        // For Spring only
    }

    public AbstractJavaComponent(ObjectFactory objectFactory)
    {
        this(objectFactory, null, null);
    }

    public AbstractJavaComponent(ObjectFactory objectFactory,
                                 EntryPointResolverSet entryPointResolverSet,
                                 NestedRouterCollection nestedRouterCollection)
    {
        this.objectFactory = objectFactory;
        this.entryPointResolverSet = entryPointResolverSet;
        if (nestedRouterCollection != null)
        {
            this.nestedRouter = nestedRouterCollection;
        }
    }

    protected Object doOnCall(MuleEvent event)
    {
        MuleMessage returnMessage = null;
        try
        {
            InboundEndpoint endpoint = (InboundEndpoint) event.getEndpoint();
            event = OptimizedRequestContext.unsafeSetEvent(event);
            Object replyTo = event.getMessage().getReplyTo();
            ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), endpoint);

            // stats
            long startTime = 0;
            if (statistics.isEnabled())
            {
                startTime = System.currentTimeMillis();
            }

            returnMessage = invokeComponentInstance(event);

            // stats
            if (statistics.isEnabled())
            {
                statistics.addExecutionTime(System.currentTimeMillis() - startTime);
            }
            // this is the request event
            // event = RequestContext.getEvent();
            if (event.isStopFurtherProcessing())
            {
                logger.debug("MuleEvent stop further processing has been set, no outbound routing will be performed.");
            }
            if (returnMessage != null && !event.isStopFurtherProcessing())
            {
                if (service.getOutboundRouter().hasEndpoints())
                {
                    MuleMessage outboundReturnMessage = service.getOutboundRouter().route(returnMessage,
                        event.getSession(), event.isSynchronous());
                    if (outboundReturnMessage != null)
                    {
                        returnMessage = outboundReturnMessage;
                    }
                }
                else
                {
                    logger.debug("Outbound router on service '" + service.getName()
                                 + "' doesn't have any endpoints configured.");
                }
            }

            // Process Response Router
            // TODO Alan C. - responseRouter is initialized to empty (no
            // endpoints) in Mule 2.x, this line can be part of a solution
            // if (returnMessage != null && service.getResponseRouter() != null
            // && !service.getResponseRouter().getEndpoints().isEmpty())
            if (returnMessage != null && service.getResponseRouter() != null)
            {
                logger.debug("Waiting for response router message");
                returnMessage = service.getResponseRouter().getResponse(returnMessage);
            }

            // process replyTo if there is one
            if (returnMessage != null && replyToHandler != null)
            {
                String requestor = (String) returnMessage.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                if ((requestor != null && !requestor.equals(service.getName())) || requestor == null)
                {
                    replyToHandler.processReplyTo(event, returnMessage, replyTo);
                }
            }

            // stats
            if (statistics.isEnabled())
            {
                statistics.incSentEventSync();
            }
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);
            if (e instanceof MessagingException)
            {
                handleException(e);
            }
            else
            {
                handleException(new MessagingException(CoreMessages.eventProcessingFailedFor(service.getName()),
                    event.getMessage(), e));
            }

            if (returnMessage == null)
            {
                // important that we pull event from request context here as it may
                // have been modified
                // (necessary to avoid scribbling between thrreads)
                returnMessage = new DefaultMuleMessage(NullPayload.getInstance(), RequestContext.getEvent()
                    .getMessage());
            }
            ExceptionPayload exceptionPayload = returnMessage.getExceptionPayload();
            if (exceptionPayload == null)
            {
                exceptionPayload = new DefaultExceptionPayload(e);
            }
            returnMessage.setExceptionPayload(exceptionPayload);
        }
        return returnMessage;

    }

    protected void doOnEvent(MuleEvent event)
    {
        try
        {
            InboundEndpoint endpoint = (InboundEndpoint) event.getEndpoint();
            // dispatch the next receiver
            event = OptimizedRequestContext.criticalSetEvent(event);
            Object replyTo = event.getMessage().getReplyTo();
            ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(), endpoint);

            // do stats
            long startTime = 0;
            if (statistics.isEnabled())
            {
                startTime = System.currentTimeMillis();
            }

            MuleMessage result = invokeComponentInstance(event);

            if (statistics.isEnabled())
            {
                statistics.addExecutionTime(System.currentTimeMillis() - startTime);
            }
            // processResponse(result, replyTo, replyToHandler);
            event = RequestContext.getEvent();
            if (result != null && !event.isStopFurtherProcessing())
            {
                if (service.getOutboundRouter().hasEndpoints())
                {
                    service.getOutboundRouter().route(result, event.getSession(), event.isSynchronous());
                }
            }

            // process replyTo if there is one
            if (result != null && replyToHandler != null)
            {
                String requestor = (String) result.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                if ((requestor != null && !requestor.equals(service.getName())) || requestor == null)
                {
                    replyToHandler.processReplyTo(event, result, replyTo);
                }
            }

            if (statistics.isEnabled())
            {
                statistics.incSentEventASync();
            }
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);
            if (e instanceof MessagingException)
            {
                handleException(e);
            }
            else
            {
                handleException(new MessagingException(CoreMessages.eventProcessingFailedFor(service.getName()),
                    event.getMessage(), e));
            }
        }
    }

    protected MuleMessage invokeComponentInstance(MuleEvent event) throws Exception
    {
        LifecycleAdapter componentLifecycleAdapter = null;
        try
        {
            componentLifecycleAdapter = borrowComponentLifecycleAdaptor();
            return componentLifecycleAdapter.intercept(null);
        }
        finally
        {
            returnComponentLifecycleAdaptor(componentLifecycleAdapter);
        }
    }

    public Class getObjectType()
    {
        return objectFactory.getObjectClass();
    }

    /**
     * Creates and initialises a new LifecycleAdaptor instance wrapped the component
     * object instance obtained from the configured object factory.
     * 
     * @return
     * @throws MuleException
     * @throws Exception
     */
    protected LifecycleAdapter createLifeCycleAdaptor() throws MuleException, Exception
    {
        LifecycleAdapter lifecycleAdapter;
        if (lifecycleAdapterFactory != null)
        {
            // Custom lifecycleAdapterFactory set on component
            lifecycleAdapter = lifecycleAdapterFactory.create(objectFactory.getInstance(), this, entryPointResolverSet);
        }
        else
        {
            // Inherit lifecycleAdapterFactory from model
            lifecycleAdapter = service.getModel().getLifecycleAdapterFactory().create(objectFactory.getInstance(),
                this, entryPointResolverSet);
        }
        lifecycleAdapter.initialise();
        return lifecycleAdapter;
    }

    protected abstract LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception;

    protected abstract void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter) throws Exception;

    // @Override
    protected void doInitialise() throws InitialisationException
    {
        if (objectFactory == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("object factory"), this);
        }
        // If this component was configured with spring the objectFactory instance
        // has already been initialised, yet if this component was no configured with
        // spring then the objectFactory is still uninitialised so we need to
        // initialise it here.
        objectFactory.initialise();
    }

    // @Override
    protected void doStart() throws MuleException
    {
        // We need to resolve entry point resolvers here rather than in initialise()
        // because when configuring with spring, although the service has been
        // injected and is available the injected service construction has not been
        // completed and model is still in null.
        if (entryPointResolverSet == null)
        {
            entryPointResolverSet = service.getModel().getEntryPointResolverSet();
        }
    }

    // @Override
    protected void doStop() throws MuleException
    {
        // TODO no-op
    }

    // @Override
    protected void doDispose()
    {
        // TODO This can't be implemented currently because AbstractService allows
        // disposed services to be re-initialised, and re-use of a disposed object
        // factory is not possible
        // objectFactory.dispose();
    }

    public EntryPointResolverSet getEntryPointResolverSet()
    {
        return entryPointResolverSet;
    }

    public NestedRouterCollection getNestedRouter()
    {
        return nestedRouter;
    }

    public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet)
    {
        this.entryPointResolverSet = entryPointResolverSet;
    }

    public void setNestedRouter(NestedRouterCollection nestedRouter)
    {
        this.nestedRouter = nestedRouter;
    }

    /**
     * Allow for incremental addition of resolvers by for example the spring-config
     * module
     * 
     * @param entryPointResolvers Resolvers to add
     */
    public void setEntryPointResolvers(Collection entryPointResolvers)
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = new DefaultEntryPointResolverSet();
        }
        for (Iterator resolvers = entryPointResolvers.iterator(); resolvers.hasNext();)
        {
            entryPointResolverSet.addEntryPointResolver((EntryPointResolver) resolvers.next());
        }
    }

    public ObjectFactory getObjectFactory()
    {
        return objectFactory;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public LifecycleAdapterFactory getLifecycleAdapterFactory()
    {
        return lifecycleAdapterFactory;
    }

    public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory)
    {
        this.lifecycleAdapterFactory = lifecycleAdapterFactory;
    }

}
