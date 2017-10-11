/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;

import org.apache.logging.log4j.ThreadContext;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

/**
 * Allows access to the privileged behavior of the {@link Event} implementation.
 *
 * @since 4.0
 */
public interface PrivilegedEvent extends CoreEvent {

  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  class CurrentEventHolder {

    private static final ThreadLocal<PrivilegedEvent> currentEvent = new ThreadLocal<>();
  }

  /**
   * @return the context applicable to all events created from the same root {@link CoreEvent} from a {@link MessageSource}.
   */
  @Override
  BaseEventContext getContext();

  /**
   * @return the correlation ID to use for this event.
   * @deprecated TODO MULE-10706 Mule 4: remove this
   */
  @Deprecated
  String getLegacyCorrelationId();

  /**
   * Retrieves the service session for the current event
   *
   * @return the service session for the event
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  MuleSession getSession();

  /**
   * Return the replyToHandler (if any) that will be used to perform async reply
   *
   * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
   */
  @Deprecated
  ReplyToHandler getReplyToHandler();

  /**
   * Return the destination (if any) that will be passed to the reply-to handler.
   *
   * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
   */
  @Deprecated
  Object getReplyToDestination();

  /**
   * Return the event associated with the currently executing thread.
   *
   * @return event for currently executing thread.
   */
  static PrivilegedEvent getCurrentEvent() {
    return CurrentEventHolder.currentEvent.get();
  }

  /**
   * Set the event to be associated with the currently executing thread.
   *
   * @param event event for currently executing thread.
   */
  static void setCurrentEvent(PrivilegedEvent event) {
    CurrentEventHolder.currentEvent.set(event);

    if (event == null) {
      ThreadContext.remove(CORRELATION_ID_MDC_KEY);
    } else {
      ThreadContext.put(CORRELATION_ID_MDC_KEY, "[event: " + event.getCorrelationId() + "] ");
    }
  }

  /**
   * Returns the contents of the message as a byte array.
   *
   * @param muleContext the Mule node.
   * @return the contents of the message as a byte array
   * @throws MuleException if the message cannot be converted into an array of bytes
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  byte[] getMessageAsBytes(MuleContext muleContext) throws MuleException;

  /**
   * Transforms the message into the requested format. The transformer used is the one configured on the endpoint through which
   * this event was received.
   *
   * @param outputType The requested output type.
   * @param muleContext the Mule node.
   * @return the message transformed into it's recognized or expected format.
   * @throws MessageTransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer if the transform fails or the outputtype is null
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  Object transformMessage(DataType outputType, MuleContext muleContext) throws MessageTransformerException;

  /**
   * Returns the message contents as a string if necessary. This will use the encoding set on the event
   *
   * @param muleContext the Mule node.
   * @return the message contents as a string
   * @throws MuleException if the message cannot be converted into a string
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  String getMessageAsString(MuleContext muleContext) throws MuleException;

  /**
   * Returns the message contents as a string
   *
   * @param encoding the encoding to use when converting the message to string
   * @param muleContext the Mule node.
   * @return the message contents as a string
   * @throws MuleException if the message cannot be converted into a string
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException;

  /**
   * Indicates if notifications should be fired when processing this message.
   *
   * @return true if notifications are enabled, false otherwise
   */
  boolean isNotificationsEnabled();

  /**
   * Create new {@link Builder}.
   *
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context) {
    return new DefaultEventBuilder((BaseEventContext) context);
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance. The existing {@link EventContext} is conserved.
   *
   * @param event existing event to use as a template to create builder instance
   * @return new builder instance.
   */
  static Builder builder(CoreEvent event) {
    return new DefaultEventBuilder((InternalEvent) event);
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance and and {@link EventContext}. A new
   * {@link EventContext} is used instead of the existing instance referenced by the existing {@link CoreEvent}. This builder
   * should only be used in some specific scenarios like {@code flow-ref} where a new Flow executing the same {@link CoreEvent}
   * needs a new context.
   *
   * @param event existing event to use as a template to create builder instance
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context, CoreEvent event) {
    return new DefaultEventBuilder((BaseEventContext) context, (InternalEvent) event);
  }

  public interface Builder extends CoreEvent.Builder {

    /**
     * Set correlationId overriding the correlationId from {@link EventContext#getCorrelationId()} that came from the source
     * system or that was configured in the connector source. This is only used to support transports and should not be used
     * otherwise.
     *
     * @param correlationId to override existing correlationId
     * @return the builder instance
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    Builder correlationId(String correlationId);

    /**
     *
     * @param replyToHandler
     * @return the builder instance
     * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
     */
    @Deprecated
    Builder replyToHandler(ReplyToHandler replyToHandler);

    /**
     *
     * @param replyToDestination
     * @return the builder instance
     * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
     */
    @Deprecated
    Builder replyToDestination(Object replyToDestination);

    /**
     * Disables the firing of notifications when processing the produced event.
     *
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    Builder disableNotifications();

    /**
     * @param session
     * @return the builder instance
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    Builder session(MuleSession session);

    /**
     * Build a new {@link PrivilegedEvent} based on the state configured in the {@link Builder}.
     *
     * @return new {@link PrivilegedEvent} instance.
     */
    @Override
    PrivilegedEvent build();

    @Override
    Builder message(Message message);

    @Override
    Builder variables(Map<String, ?> variables);

    @Override
    Builder addVariable(String key, Object value);

    @Override
    Builder addVariable(String key, Object value, DataType mediaType);

    @Override
    Builder removeVariable(String key);

    @Override
    Builder groupCorrelation(Optional<GroupCorrelation> groupCorrelation);

    @Override
    Builder error(org.mule.runtime.api.message.Error error);

    @Override
    Builder securityContext(SecurityContext securityContext);
  }
}
