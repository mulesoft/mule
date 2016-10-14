/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;

import java.nio.charset.Charset;
import java.util.List;

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
  public Event process(Event event) throws MuleException {
    try {
      connect();

      String prop = event.getMessage().getOutboundProperty(MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY);
      boolean disableTransportTransformer = (prop != null && Boolean.parseBoolean(prop))
          || endpoint.isDisableTransportTransformer();

      if (!disableTransportTransformer) {
        event = applyOutboundTransformers(event);
      }
      boolean hasResponse = endpoint.getExchangePattern().hasResponse();

      connector.getSessionHandler().storeSessionInfoToMessage(event.getSession(), event.getMessage(), endpoint.getMuleContext());

      if (hasResponse) {
        return createResponseEvent(doSend(event), event);
      } else {
        doDispatch(event);
        return event;
      }
    } catch (MuleException muleException) {
      throw muleException;
    } catch (Exception e) {
      throw new DispatchException(getEndpoint(), e);
    }
  }

  private Event createResponseEvent(InternalMessage resultMessage, Event requestEvent) throws MuleException {
    if (resultMessage != null) {
      MuleSession storedSession = connector.getSessionHandler().retrieveSessionInfoFromMessage(
                                                                                               resultMessage,
                                                                                               endpoint.getMuleContext());
      requestEvent.getSession().merge(storedSession);
      Event resultEvent = Event.builder(requestEvent).message(resultMessage).build();
      setCurrentEvent(resultEvent);
      return resultEvent;
    } else {
      return null;
    }
  }

  /**
   * @deprecated
   */
  @Deprecated
  protected boolean returnResponse(Event event) {
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
  protected boolean returnResponse(Event event, boolean doSend) {
    boolean remoteSync = false;
    if (endpoint.getConnector().isResponseEnabled()) {
      boolean hasResponse = endpoint.getExchangePattern().hasResponse();
      remoteSync = hasResponse || doSend;
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

  protected Event applyOutboundTransformers(Event event) throws MuleException {
    return Event.builder(event)
        .message(getTransformationService().applyTransformers(event.getMessage(), event, defaultOutboundTransformers))
        .build();
  }

  protected abstract void doDispatch(Event event) throws Exception;

  protected abstract InternalMessage doSend(Event event) throws Exception;

  protected Charset resolveEncoding(Event event) {
    return event.getMessage().getPayload().getDataType().getMediaType().getCharset().orElseGet(() -> {
      Charset encoding = getEndpoint().getEncoding();
      if (encoding == null) {
        encoding = getDefaultEncoding(getEndpoint().getMuleContext());
      }
      return encoding;
    });
  }

}
