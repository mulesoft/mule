/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.message.DefaultMuleEventBuilder;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Legacy implementation of {@link MuleEvent}
 * <p>
 * Holds a MuleMessage payload and provides helper methods for obtaining the data in a format that the receiving Mule component
 * understands. The event can also maintain any number of properties that can be set and retrieved by Mule components.
 *
 * @see org.mule.runtime.api.message.MuleEvent
 * @see MuleMessage
 */
public interface MuleEvent extends org.mule.runtime.api.message.MuleEvent {

  /**
   * @return the context applicable to all events created from the same root {@link MuleEvent} from a {@link MessageSource}.
   */
  MessageContext getContext();

  /**
   * Returns the correlation metadata of this message. See {@link Correlation}.
   * 
   * @return the correlation metadata of this message.
   */
  Correlation getCorrelation();

  /**
   * The returned value will depend on the {@link MessageSource} that created this event, and the flow that is executing the
   * event.
   *
   * @return the correlation id to use for this event.
   */
  String getCorrelationId();

  /**
   * Returns the message payload for this event
   * 
   * @return the message payload for this event
   */
  @Override
  MuleMessage getMessage();

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
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer if the transform fails or the outputtype is null
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  <T> T transformMessage(Class<T> outputType, MuleContext muleContext) throws TransformerException;

  /**
   * Transforms the message into the requested format. The transformer used is the one configured on the endpoint through which
   * this event was received.
   * 
   * @param outputType The requested output type.
   * @param muleContext the Mule node.
   * @return the message transformed into it's recognized or expected format.
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer if the transform fails or the outputtype is null
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  Object transformMessage(DataType outputType, MuleContext muleContext) throws TransformerException;

  /**
   * Returns the message transformed into it's recognized or expected format and then into a String. The transformer used is the
   * one configured on the endpoint through which this event was received. If necessary this will use the encoding set on the
   * event.
   * 
   * @param muleContext the Mule node.
   * @return the message transformed into it's recognized or expected format as a Strings.
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer
   * @deprecated TODO MULE-10013 Move message serialization logic from within the message to an external service
   */
  @Deprecated
  String transformMessageToString(MuleContext muleContext) throws TransformerException;

  /**
   * Returns the message contents as a string If necessary this will use the encoding set on the event
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
   * Retrieves the service session for the current event
   * 
   * @return the service session for the event
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  MuleSession getSession();

  /**
   * Retrieves the service for the current event
   * 
   * @return the service for the event
   * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API
   */
  @Deprecated
  FlowConstruct getFlowConstruct();

  /**
   * Returns the muleContext for the Mule node that this event was received in
   * 
   * @return the muleContext for the Mule node that this event was received in
   * @deprecated TODO MULE-10013 remove this
   */
  @Deprecated
  MuleContext getMuleContext();

  /**
   * Returns the message exchange pattern for this event
   * 
   * @deprecated TODO MULE-10445 Mule 4 - New Threading model
   */
  @Deprecated
  MessageExchangePattern getExchangePattern();

  /**
   * Returns true is this event is being processed in a transaction
   * 
   * @deprecated TODO MULE-10445 Mule 4 - New Threading model
   */
  @Deprecated
  boolean isTransacted();

  /**
   * Return the replyToHandler (if any) that will be used to perform async reply
   * 
   * @deprecated TODO MULE-9731 Migrate 3.7 ReplyToHandler centric non-blocking support to use new non-blocking API
   */
  @Deprecated
  ReplyToHandler getReplyToHandler();

  /**
   * Return the destination (if any) that will be passed to the reply-to handler.
   * 
   * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API
   */
  @Deprecated
  Object getReplyToDestination();

  /**
   * 
   * @return
   * @deprecated MULE-10445 Mule 4 - New Threading model
   */
  @Deprecated
  boolean isSynchronous();

  /**
   * Indicates if notifications should be fired when processing this message.
   *
   * @return true if notifications are enabled, false otherwise
   */
  boolean isNotificationsEnabled();

  /**
   * Indicates if the current event allows non-blocking execution and IO.
   *
   * @return true if non-blocking execution and IO is allowed. False otherwise.
   * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API
   */
  @Deprecated
  boolean isAllowNonBlocking();

  /**
   * Events have a stack of executed flows (same as a call stack), so that at any given instant an application developer can
   * determine where this event came from.
   * <p>
   * This will only be enabled if {@link DefaultMuleConfiguration#isFlowTrace()} is {@code true}. If {@code false}, the stack will
   * always be empty.
   * 
   * @return the flow stack associated to this event.
   * 
   * @since 3.8.0
   */
  FlowCallStack getFlowCallStack();

  /**
   * The security context for this session. If not null outbound, inbound and/or method invocations will be authenticated using
   * this context
   *
   * @return the context for this session or null if the request is not secure.
   */
  SecurityContext getSecurityContext();

  /**
   * Create new {@link Builder}.
   *
   * @param eventContext the event context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(MessageContext eventContext) {
    return new DefaultMuleEventBuilder(eventContext);
  }

  /**
   * Create new {@link Builder} based on an existing {@link org.mule.runtime.api.message.MuleEvent} instance.
   * The existing {@link MessageContext} is conserved.
   *
   * @param event existing event to use as a template to create builder instance
   * @return new builder instance.
   */
  static Builder builder(MuleEvent event) {
    return new DefaultMuleEventBuilder(event);
  }

  interface Builder {

    /**
     * Set the {@link MuleMessage} to construct {@link MuleEvent} with.
     *
     * @param message the message instance.
     * @return the builder instance
     */
    Builder message(MuleMessage message);

    /**
     * Set a map of flow variables.  Any existing flow variables added to the builder will be removed.
     *
     * @param flowVariables flow variables to be set.
     * @return the builder instance
     */
    Builder flowVariables(Map<String, Object> flowVariables);

    /**
     * Add a flow variable.
     *
     * @param key the key of the flow variable to add.
     * @param value the value of the flow varibale to add.
     * @return the builder instance.
     */
    Builder addFlowVariable(String key, Object value);

    /**
     * Add a flow variable.
     *
     * @param key the key of the flow variable to add.
     * @param value the value of the flow variable to add.
     * @param mediaType the flow variable media this
     * @return the builder instance
     */
    Builder addFlowVariable(String key, Object value, DataType mediaType);

    /**
     * Remove a flow variable.
     *
     * @param key the flow variable key.
     * @return the builder instance
     */
    Builder removeFlowVariable(String key);

    /**
     * Set correlationId overriding the correlationId from {@link MessageContext#getCorrelationId()} that came from the source
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
     * Sets the correlation information to the produced event.
     *
     * @param correlation the object containing the correlation information to set on the produced event
     * @return the builder instance
     */
    Builder correlation(Correlation correlation);

    /**
     * Sets an error related to the produced event.
     *
     * @param error the error associated with the produced event
     * @return the builder instance
     */
    Builder error(Error error);

    /**
     * 
     * @param synchronous
     * @return the builder instance
     * @deprecated MULE-10445 Mule 4 - New Threading model
     */
    @Deprecated
    Builder synchronous(boolean synchronous);

    /**
     * 
     * @param exchangePattern
     * @return the builder instance
     * @deprecated MULE-10445 Mule 4 - New Threading model
     */
    @Deprecated
    Builder exchangePattern(MessageExchangePattern exchangePattern);

    /**
     * 
     * @param replyToHandler
     * @return the builder instance
     * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API
     */
    @Deprecated
    Builder replyToHandler(ReplyToHandler replyToHandler);

    /**
     * 
     * @param replyToDestination
     * @return the builder instance
     * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API
     */
    @Deprecated
    Builder replyToDestination(Object replyToDestination);

    /**
     * 
     * @param flow
     * @return the builder instance
     * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API
     */
    @Deprecated
    Builder flow(FlowConstruct flow);

    /**
     * 
     * @param transacted
     * @return the builder instance
     * @deprecated TODO MULE-10445 Mule 4 - New Threading model
     */
    @Deprecated
    Builder transacted(boolean transacted);

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
     * @return the builder instance
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    Builder refreshSync();

    /**
     * Build a new {@link MuleEvent} based on the state configured in the {@link Builder}.
     *
     * @return new {@link MuleEvent} instance.
     */
    MuleEvent build();

  }
}
