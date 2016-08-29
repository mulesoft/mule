/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_SYNC_PROPERTY;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;

import java.nio.charset.Charset;
import java.util.List;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

/**
 * Abstract implementation of an outbound channel adaptors. Outbound channel adaptors send messages over over a specific
 * transport. Different implementations may support different Message Exchange Patterns.
 */
public abstract class AbstractMessageDispatcher extends AbstractTransportMessageHandler
    implements MessageDispatcher {

  protected List<Transformer> defaultOutboundTransformers;
  protected List<Transformer> defaultResponseTransformers;

  public AbstractMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
  }

  @Override
  protected ConnectableLifecycleManager createLifecycleManager() {
    defaultOutboundTransformers = connector.getDefaultOutboundTransformers(endpoint);
    defaultResponseTransformers = connector.getDefaultResponseTransformers(endpoint);
    return new ConnectableLifecycleManager<MessageDispatcher>(getDispatcherName(), this);
  }

  protected String getDispatcherName() {
    return getConnector().getName() + ".dispatcher." + System.identityHashCode(this);
  }

  @Override
  public MuleEvent process(final MuleEvent event) throws MuleException {
    try {
      connect();

      String prop = event.getMessage().getOutboundProperty(MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY);
      boolean disableTransportTransformer = (prop != null && Boolean.parseBoolean(prop))
          || endpoint.isDisableTransportTransformer();

      if (!disableTransportTransformer) {
        applyOutboundTransformers(event);
      }
      boolean hasResponse = endpoint.getExchangePattern().hasResponse();

      connector.getSessionHandler().storeSessionInfoToMessage(event.getSession(), event.getMessage(), endpoint.getMuleContext());

      if (hasResponse) {
        if (isNonBlocking(event)) {
          doSendNonBlocking(event,
                            new NonBlockingSendCompletionHandler(event, ((Flow) endpoint.getFlowConstruct()).getWorkManager(),
                                                                 connector));
          // Update RequestContext ThreadLocal for backwards compatibility. Clear event as we are done with this thread.
          setCurrentEvent(null);
          return NonBlockingVoidMuleEvent.getInstance();
        } else {
          return createResponseEvent(doSend(event), event);
        }
      } else {
        doDispatch(event);
        return VoidMuleEvent.getInstance();
      }
    } catch (MuleException muleException) {
      throw muleException;
    } catch (Exception e) {
      throw new DispatchException(event, getEndpoint(), e);
    }
  }

  private MuleEvent createResponseEvent(MuleMessage resultMessage, MuleEvent requestEvent) throws MuleException {
    if (resultMessage != null) {
      MuleSession storedSession = connector.getSessionHandler().retrieveSessionInfoFromMessage(
                                                                                               resultMessage,
                                                                                               endpoint.getMuleContext());
      requestEvent.getSession().merge(storedSession);
      MuleEvent resultEvent = MuleEvent.builder(requestEvent).message(resultMessage).build();
      setCurrentEvent(resultEvent);
      return resultEvent;
    } else {
      return null;
    }
  }

  private boolean isNonBlocking(MuleEvent event) {
    return endpoint.getFlowConstruct() instanceof Flow && event.isAllowNonBlocking() && event.getReplyToHandler() != null &&
        isSupportsNonBlocking() && !endpoint.getTransactionConfig().isTransacted();
  }

  /**
   * Dispatcher implementations that support non-blocking processing should override this method and return 'true'. To support
   * non-blocking processing it is also necessary to implment the
   * {@link AbstractMessageDispatcher#doSendNonBlocking(MuleEvent, CompletionHandler)} method.
   *
   * @return true if non-blocking processing is supported by this dispatcher implemnetation.
   */
  protected boolean isSupportsNonBlocking() {
    return false;
  }

  /**
   * @deprecated
   */
  @Deprecated
  protected boolean returnResponse(MuleEvent event) {
    // Pass through false to conserve the existing behavior of this method but
    // avoid duplication of code.
    return returnResponse(event, false);
  }

  /**
   * Used to determine if the dispatcher implementation should wait for a response to an event on a response channel after it
   * sends the event. The following rules apply:
   * <ol>
   * <li>The connector has to support "back-channel" response. Some transports do not have the notion of a response channel.
   * <li>Check if the endpoint is synchronous (outbound synchronicity is not explicit since 2.2 and does not use the remoteSync
   * message property).
   * <li>Or, if the send() method on the dispatcher was used. (This is required because the ChainingRouter uses send() with async
   * endpoints. See MULE-4631).
   * <li>Finally, if the current service has a response router configured, that the router will handle the response channel event
   * and we should not try and receive a response in the Message dispatcher If remotesync should not be used we must remove the
   * REMOTE_SYNC header Note the MuleClient will automatically set the REMOTE_SYNC header when client.send(..) is called so that
   * results are returned from remote invocations too.
   * </ol>
   * 
   * @param event the current event
   * @return true if a response channel should be used to get a response from the event dispatch.
   */
  protected boolean returnResponse(MuleEvent event, boolean doSend) {
    boolean remoteSync = false;
    if (endpoint.getConnector().isResponseEnabled()) {
      boolean hasResponse = endpoint.getExchangePattern().hasResponse();
      remoteSync = hasResponse || doSend;
    }
    if (!remoteSync) {
      event.setMessage(MuleMessage.builder(event.getMessage()).removeOutboundProperty(MULE_REMOTE_SYNC_PROPERTY).build());
      event.removeFlowVariable(MULE_REMOTE_SYNC_PROPERTY);
    }
    return remoteSync;
  }

  @Override
  protected WorkManager getWorkManager() {
    try {
      return connector.getDispatcherWorkManager();
    } catch (MuleException e) {
      logger.error("Cannot access dispatcher work manager", e);
      return null;
    }
  }

  @Override
  public OutboundEndpoint getEndpoint() {
    return (OutboundEndpoint) super.getEndpoint();
  }

  protected void applyOutboundTransformers(MuleEvent event) throws MuleException {
    event.setMessage(getTransformationService().applyTransformers(event.getMessage(), event, defaultOutboundTransformers));
  }

  protected abstract void doDispatch(MuleEvent event) throws Exception;

  protected abstract MuleMessage doSend(MuleEvent event) throws Exception;

  protected void doSendNonBlocking(MuleEvent event, CompletionHandler<MuleMessage, Exception, Void> completionHandler) {
    throw new IllegalStateException("This MessageDispatcher does not support non-blocking");
  }

  private class NonBlockingSendCompletionHandler implements CompletionHandler<MuleMessage, Exception, Void> {

    private final MuleEvent event;
    private final WorkManager workManager;
    private final WorkListener workListener;


    public NonBlockingSendCompletionHandler(MuleEvent event, WorkManager workManager, WorkListener workListener) {
      this.event = event;
      this.workManager = workManager;
      this.workListener = workListener;
    }

    @Override
    public void onCompletion(final MuleMessage result, ExceptionCallback<Void, Exception> exceptionCallback) {
      try {
        workManager.scheduleWork(new Work() {

          @Override
          public void run() {
            try {
              MuleEvent responseEvent = createResponseEvent(result, event);
              // Set RequestContext ThreadLocal in new thread for backwards compatibility
              setCurrentEvent(responseEvent);
              event.getReplyToHandler().processReplyTo(responseEvent, null, null);
            } catch (MessagingException messagingException) {
              event.getReplyToHandler().processExceptionReplyTo(messagingException, null);
            } catch (MuleException exception) {
              event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, exception), null);
            }
          }

          @Override
          public void release() {
            // no-op
          }
        }, WorkManager.INDEFINITE, null, workListener);
      } catch (Exception exception) {
        onFailure(exception);
        exceptionCallback.onException(exception);
      }
    }

    @Override
    public void onFailure(final Exception exception) {
      try {
        workManager.scheduleWork(new Work() {

          @Override
          public void run() {
            // Set RequestContext ThreadLocal in new thread for backwards compatibility
            setCurrentEvent(event);
            event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, exception), null);
          }

          @Override
          public void release() {
            // no-op
          }
        }, WorkManager.INDEFINITE, null, workListener);
      } catch (WorkException e) {
        // Handle exception in transport thread if unable to schedule work
        event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, exception), null);
      }
    }
  }

  protected Charset resolveEncoding(MuleEvent event) {
    return event.getMessage().getDataType().getMediaType().getCharset().orElseGet(() -> {
      Charset encoding = getEndpoint().getEncoding();
      if (encoding == null) {
        encoding = getDefaultEncoding(getEndpoint().getMuleContext());
      }
      return encoding;
    });
  }

}
