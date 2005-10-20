/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.impl.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.*;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.management.stats.ComponentStatistics;
import org.mule.providers.AbstractConnector;
import org.mule.providers.ReplyToHandler;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.util.ObjectPool;
import org.mule.util.queue.QueueSession;

import javax.resource.spi.work.Work;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>MuleProxy</code> is a proxy to a UMO. It is a poolable object that
 * that can be executed in it's own thread.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class DefaultMuleProxy implements MuleProxy
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(DefaultMuleProxy.class);

    /**
     * Holds the current event being processed
     */
    private UMOEvent event;

    /**
     * Holds the actual UMO
     */
    private UMOLifecycleAdapter umo;

    /**
     * holds the UMO descriptor
     */
    private ImmutableMuleDescriptor descriptor;

    /**
     * Determines if the proxy is suspended
     */
    private boolean suspended = true;

    private List interceptorList;

    private ObjectPool proxyPool;

    private ComponentStatistics stat = null;

    private QueueSession queueSession = null;

    /**
     * Constructs a Proxy using the UMO's AbstractMessageDispatcher and the UMO
     * itself
     *
     * @param component the underlying object that with receive events
     * @param descriptor the UMOComponent descriptor associated with the
     *            component
     */
    public DefaultMuleProxy(Object component, MuleDescriptor descriptor, ObjectPool proxyPool) throws UMOException
    {
        this.descriptor = new ImmutableMuleDescriptor(descriptor);
        this.proxyPool = proxyPool;

        UMOModel model = MuleManager.getInstance().getModel();

        UMOEntryPointResolver resolver = model.getEntryPointResolver();
        umo = model.getLifecycleAdapterFactory().create(component, descriptor, resolver);

        interceptorList = new ArrayList(descriptor.getInterceptors().size() + 1);
        interceptorList.addAll(descriptor.getInterceptors());
        interceptorList.add(umo);

        for (Iterator iter = interceptorList.iterator(); iter.hasNext();) {
            UMOInterceptor interceptor = (UMOInterceptor) iter.next();
            if (interceptor instanceof Initialisable) {
                try {
                    ((Initialisable) interceptor).initialise();
                } catch (Exception e) {
                    throw new ModelException(new Message(Messages.X_FAILED_TO_INITIALISE, "Component '"
                            + descriptor.getName() + "'"), e);
                }
            }
        }
    }

    public void start() throws UMOException
    {
        checkDisposed();
        if (!umo.isStarted()) {
            try {
                umo.start();
            } catch (Exception e) {
                throw new ModelException(new Message(Messages.FAILED_TO_START_X, "Component '" + descriptor.getName()
                        + "'"), e);
            }
        }

    }

    public boolean isStarted()
    {
        return umo.isStarted();
    }

    public void stop() throws UMOException
    {
        checkDisposed();
        if (umo.isStarted()) {
            try {
                umo.stop();
            } catch (Exception e) {
                throw new ModelException(new Message(Messages.FAILED_TO_STOP_X, "Component '" + descriptor.getName()
                        + "'"), e);
            }
        }
    }

    public void dispose()
    {
        checkDisposed();
        for (Iterator iter = interceptorList.iterator(); iter.hasNext();) {
            UMOInterceptor interceptor = (UMOInterceptor) iter.next();
            if (interceptor instanceof Disposable) {
                try {
                    ((Disposable) interceptor).dispose();
                } catch (Exception e) {
                    logger.error(new Message(Messages.FAILED_TO_DISPOSE_X, "Component '" + descriptor.getName() + "'"),
                                 e);
                }
            }
        }
    }

    private void checkDisposed()
    {
        if (umo.isDisposed()) {
            throw new IllegalStateException("Components Disposed Of");
        }
    }

    /**
     * Sets the current event being processed
     *
     * @param event the event being processed
     */
    public void onEvent(QueueSession session, UMOEvent event)
    {
        this.queueSession = session;
        this.event = (MuleEvent) event;
    }

    public ComponentStatistics getStatistics()
    {
        return stat;
    }

    public void setStatistics(ComponentStatistics stat)
    {
        this.stat = stat;
    }

    /**
     * Makes a synchronous call on the UMO
     *
     * @param event the event to pass to the UMO
     * @return the return event from the UMO
     * @throws UMOException if the call fails
     */
    public Object onCall(UMOEvent event) throws UMOException
    {
        logger.trace("MuleProxy: sync call for Mule UMO " + descriptor.getName());

        UMOMessage returnMessage = null;
        try {
            if (event.getEndpoint().canReceive()) {
                RequestContext.setEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = null;
                if (replyTo != null) {
                    replyToHandler = ((AbstractConnector) event.getEndpoint().getConnector()).getReplyToHandler();
                }
                InterceptorsInvoker invoker = new InterceptorsInvoker(interceptorList, descriptor, event.getMessage());

                // stats
                long startTime = 0;
                if (stat.isEnabled()) {
                    startTime = System.currentTimeMillis();
                }
                returnMessage = invoker.execute();

                // stats
                if (stat.isEnabled()) {
                    stat.addExecutionTime(System.currentTimeMillis() - startTime);
                }
                // this is the request event
                event = RequestContext.getEvent();
                if (event.isStopFurtherProcessing()) {
                    logger.debug("Event stop further processing has been set, no outbound routing will be performed.");
                }
                if (returnMessage != null && !event.isStopFurtherProcessing()) {
                    Map context = RequestContext.clearProperties();
                    if (context != null) {
                        returnMessage.addProperties(context);
                    }
                    if(descriptor.getOutboundRouter().hasEndpoints()) {
                        UMOMessage outboundReturnMessage = descriptor.getOutboundRouter().route(returnMessage, event.getSession(), event.isSynchronous());
                        if(outboundReturnMessage!=null) {
                            returnMessage = outboundReturnMessage;
                        }
                    } else {
                        logger.debug("Outbound router on component '" + descriptor.getName() + "' doesn't have any endpoints configured.");
                    }
                }

                //Process Response Router
                if (returnMessage != null && descriptor.getResponseRouter() != null) {
                    logger.debug("Waiting for response router message");
                    returnMessage = descriptor.getResponseRouter().getResponse(returnMessage);
                }

                // process repltyTo if there is one
                if (returnMessage != null && replyToHandler != null) {
                    String requestor = (String) returnMessage.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                    if ((requestor != null && !requestor.equals(descriptor.getName())) || requestor == null) {
                        replyToHandler.processReplyTo(event, returnMessage, replyTo);
                    }
                }


            } else {
                returnMessage = event.getSession().sendEvent(event);
                processReplyTo(returnMessage);
            }

            // stats
            if (stat.isEnabled()) {
                stat.incSentEventSync();
            }
        } catch (Exception e) {
            event.getSession().setValid(false);
            if (e instanceof UMOException) {
                handleException(e);
            } else {
                handleException(new MessagingException(new Message(Messages.EVENT_PROCESSING_FAILED_FOR_X,
                                                                   descriptor.getName()), event.getMessage(), e));
            }
        }
        return returnMessage;
    }

    /**
     * When an exception occurs this method can be called to invoke the
     * configured UMOExceptionStrategy on the UMO
     * 
     * @param exception If the UMOExceptionStrategy implementation fails
     */
    public void handleException(Exception exception)
    {
        descriptor.getExceptionListener().exceptionThrown(exception);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "proxy for: " + descriptor.toString();
    }

    /**
     * Determines if the proxy is suspended
     * 
     * @return true if the proxy (and the UMO) are suspended
     */
    public boolean isSuspended()
    {
        return suspended;
    }

    /**
     * Controls the suspension of the UMO event processing
     */
    public void suspend()
    {
        suspended = true;
    }

    /**
     * Triggers the UMO to resume processing of events if it is suspended
     */
    public void resume()
    {
        suspended = false;
    }

    private void processReplyTo(UMOMessage returnMessage) throws UMOException
    {
        if (returnMessage != null && returnMessage.getReplyTo() != null) {
            logger.info("sending reply to: " + returnMessage.getReplyTo());
            UMOEndpointURI endpointUri = new MuleEndpointURI(returnMessage.getReplyTo().toString());

            // get the endpointUri for this uri
            UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);

            // Create the replyTo event asynchronous
            UMOEvent replyToEvent = new MuleEvent(returnMessage, endpoint, event.getSession(), false);
            // make sure remove the replyTo property as not cause a a forever
            // replyto loop
            replyToEvent.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);

            // queue the event
            onEvent(queueSession, replyToEvent);
            logger.info("reply to sent: " + returnMessage.getReplyTo());
            if (stat.isEnabled()) {
                stat.incSentReplyToEvent();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        logger.trace("MuleProxy: async onEvent for Mule UMO " + descriptor.getName());

        try {
            if (event.getEndpoint().canReceive()) {
                // dispatch the next receiver
                RequestContext.setEvent(event);
                Object replyTo = event.getMessage().getReplyTo();
                ReplyToHandler replyToHandler = null;
                if (replyTo != null) {
                    replyToHandler = ((AbstractConnector) event.getEndpoint().getConnector()).getReplyToHandler();
                }
                InterceptorsInvoker invoker = new InterceptorsInvoker(interceptorList, descriptor, event.getMessage());

                // do stats
                long startTime = 0;
                if (stat.isEnabled()) {
                    startTime = System.currentTimeMillis();
                }
                UMOMessage result = invoker.execute();
                if (stat.isEnabled()) {
                    stat.addExecutionTime(System.currentTimeMillis() - startTime);
                }
                // processResponse(result, replyTo, replyToHandler);
                event = (MuleEvent) RequestContext.getEvent();
                if (result != null && !event.isStopFurtherProcessing()) {
                    descriptor.getOutboundRouter().route(result, event.getSession(), event.isSynchronous());
                }

                // process repltyTo if there is one
                if (result != null && replyToHandler != null) {
                    String requestor = (String) result.getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
                    if ((requestor != null && !requestor.equals(descriptor.getName())) || requestor == null) {
                        replyToHandler.processReplyTo(event, result, replyTo);
                    }
                }
            } else {
                UMOMessageDispatcher dispatcher = event.getEndpoint()
                                                       .getConnector()
                                                       .getDispatcher(event.getEndpoint().getEndpointURI().getAddress());
                dispatcher.dispatch(event);
            }

            if (stat.isEnabled()) {
                stat.incSentEventASync();
            }
        } catch (Exception e) {
            event.getSession().setValid(false);
            if (e instanceof UMOException) {
                handleException(e);
            } else {
                handleException(new MessagingException(new Message(Messages.EVENT_PROCESSING_FAILED_FOR_X,
                                                                   descriptor.getName()), event.getMessage(), e));
            }
        } finally {

            try {
                proxyPool.returnObject(this);
            } catch (Exception e2) {
                logger.error("Failed to return proxy: " + e2.getMessage(), e2);
            }
            getStatistics().setComponentPoolSize(proxyPool.getSize());
        }
    }

    public void release()
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOLifecycleAdapter#getDescriptor()
     */
    public UMOImmutableDescriptor getDescriptor()
    {
        return descriptor;
    }
}
