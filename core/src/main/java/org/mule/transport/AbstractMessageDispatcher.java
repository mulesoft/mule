/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.mule.OptimizedRequestContext.unsafeSetEvent;
import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.CompletionHandler;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.Flow;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;

import java.util.List;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

/**
 * Abstract implementation of an outbound channel adaptors. Outbound channel adaptors send messages over over
 * a specific transport. Different implementations may support different Message Exchange Patterns.
 */
public abstract class AbstractMessageDispatcher extends AbstractTransportMessageHandler
    implements MessageDispatcher
{

    protected List<Transformer> defaultOutboundTransformers;
    protected List<Transformer> defaultResponseTransformers;

    public AbstractMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected ConnectableLifecycleManager createLifecycleManager()
    {
        defaultOutboundTransformers = connector.getDefaultOutboundTransformers(endpoint);
        defaultResponseTransformers = connector.getDefaultResponseTransformers(endpoint);
        return new ConnectableLifecycleManager<MessageDispatcher>(getDispatcherName(), this);
    }

    protected String getDispatcherName()
    {
        return getConnector().getName() + ".dispatcher." + System.identityHashCode(this);
    }

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        try
        {
            connect();

            String prop = event.getMessage().getOutboundProperty(
                MuleProperties.MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY);
            boolean disableTransportTransformer = (prop != null && Boolean.parseBoolean(prop))
                                                  || endpoint.isDisableTransportTransformer();

            if (!disableTransportTransformer)
            {
                applyOutboundTransformers(event);
            }
            boolean hasResponse = endpoint.getExchangePattern().hasResponse();

            connector.getSessionHandler().storeSessionInfoToMessage(event.getSession(), event.getMessage());

            if (hasResponse)
            {
                if (!event.getMuleContext().waitUntilStarted(event.getTimeout()))
                {
                    throw new MessagingException(MessageFactory.createStaticMessage("Timeout waiting for mule context to be completely started"), event, this);
                }

                if (isNonBlocking(event))
                {
                    doSendNonBlocking(event, new NonBlockingSendCompletionHandler(event, ((Flow) event.getFlowConstruct()).getWorkManager(), connector));
                    // Update RequestContext ThreadLocal for backwards compatibility.  Clear event as we are done with this
                    // thread.
                    RequestContext.clear();
                    return NonBlockingVoidMuleEvent.getInstance();
                }
                else
                {
                    MuleMessage resultMessage = doSend(event);
                    return createResponseEvent(resultMessage, event);
                }
            }
            else
            {
                doDispatch(event);
                return VoidMuleEvent.getInstance();
            }
        }
        catch (MuleException muleException)
        {
            throw muleException;
        }
        catch (Exception e)
        {
            throw new DispatchException(event, getEndpoint(), e);
        }
    }

    private MuleEvent createResponseEvent(MuleMessage resultMessage, MuleEvent requestEvent) throws MuleException
    {
        if (resultMessage != null)
        {
            resultMessage.setMessageRootId(requestEvent.getMessage().getMessageRootId());

            // Ensure ENCODING message property is set to give exactly same behavior as before
            // OutboundRewriteResponseEventMessageProcessor was removed (MULE-7535).
            resultMessage.setEncoding(resultMessage.getEncoding());

            MuleSession storedSession = connector.getSessionHandler().retrieveSessionInfoFromMessage(
                    resultMessage);
            requestEvent.getSession().merge(storedSession);
            MuleEvent resultEvent = new DefaultMuleEvent(resultMessage, requestEvent);
            unsafeSetEvent(resultEvent);
            return resultEvent;
        }
        else
        {
            return null;
        }
    }

    private boolean isNonBlocking(MuleEvent event)
    {
        return event.getFlowConstruct() instanceof Flow && event.isAllowNonBlocking() && event.getReplyToHandler() != null &&
               isSupportsNonBlocking() && !endpoint.getTransactionConfig().isTransacted();
    }

    /**
     * Dispatcher implementations that support non-blocking processing should override this method and return 'true'.
     * To support non-blocking processing it is also necessary to implment the
     * {@link AbstractMessageDispatcher#doSendNonBlocking(MuleEvent, CompletionHandler)} method.
     *
     * @return true if non-blocking processing is supported by this dispatcher implemnetation.
     */
    protected boolean isSupportsNonBlocking()
    {
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated
    protected boolean returnResponse(MuleEvent event)
    {
        // Pass through false to conserve the existing behavior of this method but
        // avoid duplication of code.
        return returnResponse(event, false);
    }

    /**
     * Used to determine if the dispatcher implementation should wait for a response to an event on a response
     * channel after it sends the event. The following rules apply:
     * <ol>
     * <li>The connector has to support "back-channel" response. Some transports do not have the notion of a
     * response channel.
     * <li>Check if the endpoint is synchronous (outbound synchronicity is not explicit since 2.2 and does not
     * use the remoteSync message property).
     * <li>Or, if the send() method on the dispatcher was used. (This is required because the ChainingRouter
     * uses send() with async endpoints. See MULE-4631).
     * <li>Finally, if the current service has a response router configured, that the router will handle the
     * response channel event and we should not try and receive a response in the Message dispatcher If
     * remotesync should not be used we must remove the REMOTE_SYNC header Note the MuleClient will
     * automatically set the REMOTE_SYNC header when client.send(..) is called so that results are returned
     * from remote invocations too.
     * </ol>
     * 
     * @param event the current event
     * @return true if a response channel should be used to get a response from the event dispatch.
     */
    protected boolean returnResponse(MuleEvent event, boolean doSend)
    {
        boolean remoteSync = false;
        if (endpoint.getConnector().isResponseEnabled())
        {
            boolean hasResponse = endpoint.getExchangePattern().hasResponse();
            remoteSync = hasResponse || doSend;
            if (remoteSync)
            {
                // service will be null for client calls
                if (event.getFlowConstruct() != null && event.getFlowConstruct() instanceof Service)
                {
                    ServiceAsyncReplyCompositeMessageSource responseRouters = ((Service) event.getFlowConstruct()).getAsyncReplyMessageSource();
                    if (responseRouters != null && responseRouters.getEndpoints().size() > 0)
                    {
                        remoteSync = false;
                    }
                    else
                    {
                        remoteSync = true;
                    }
                }
            }
        }
        if (!remoteSync)
        {
            event.getMessage().removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
        }
        return remoteSync;
    }

    @Override
    protected WorkManager getWorkManager()
    {
        try
        {
            return connector.getDispatcherWorkManager();
        }
        catch (MuleException e)
        {
            logger.error(e);
            return null;
        }
    }

    @Override
    public OutboundEndpoint getEndpoint()
    {
        return (OutboundEndpoint) super.getEndpoint();
    }

    protected void applyOutboundTransformers(MuleEvent event) throws MuleException
    {
        event.getMessage().applyTransformers(event, defaultOutboundTransformers);
    }

    protected void applyResponseTransformers(MuleEvent event) throws MuleException
    {
        event.getMessage().applyTransformers(event, defaultResponseTransformers);
    }

    protected abstract void doDispatch(MuleEvent event) throws Exception;

    protected abstract MuleMessage doSend(MuleEvent event) throws Exception;

    protected void doSendNonBlocking(MuleEvent event, CompletionHandler<MuleMessage, Exception> completionHandler)
    {
        throw new IllegalStateException("This MessageDispatcher does not support non-blocking");
    }

    private class NonBlockingSendCompletionHandler implements CompletionHandler<MuleMessage, Exception>
    {
        private final MuleEvent event;
        private final WorkManager workManager;
        private final WorkListener workListener;


        public NonBlockingSendCompletionHandler(MuleEvent event, WorkManager workManager, WorkListener workListener)
        {
            this.event = event;
            this.workManager = workManager;
            this.workListener = workListener;
        }

        @Override
        public void onCompletion(final MuleMessage result)
        {
            try
            {
                workManager.scheduleWork(new Work()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            resetAccessControl(result);
                            MuleEvent responseEvent = createResponseEvent(result, event);
                            // Set RequestContext ThreadLocal in new thread for backwards compatibility
                            unsafeSetEvent(responseEvent);
                            event.getReplyToHandler().processReplyTo(responseEvent, null, null);
                        }
                        catch (MessagingException messagingException)
                        {
                            event.getReplyToHandler().processExceptionReplyTo(messagingException, null);
                        }
                        catch (MuleException exception)
                        {
                            event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, exception), null);
                        }
                    }

                    @Override
                    public void release()
                    {
                        // no-op
                    }
                }, WorkManager.INDEFINITE, null, workListener);
            }
            catch (Exception exception)
            {
                onFailure(exception);
            }
        }

        @Override
        public void onFailure(final Exception exception)
        {
            try
            {
                workManager.scheduleWork(new Work()
                {
                    @Override
                    public void run()
                    {
                        resetAccessControl(event);
                        // Set RequestContext ThreadLocal in new thread for backwards compatibility
                        unsafeSetEvent(event);
                        event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, exception), null);
                    }

                    @Override
                    public void release()
                    {
                        // no-op
                    }
                }, WorkManager.INDEFINITE, null, workListener);
            }
            catch (WorkException e)
            {
                // Handle exception in transport thread if unable to schedule work
                event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, exception), null);
            }
        }

        private void resetAccessControl(Object result)
        {
            // Reset access control for new thread
            if (result instanceof ThreadSafeAccess)
            {
                ((ThreadSafeAccess) result).resetAccessControl();
            }
        }

    }
}
