/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.message.DefaultEventBuilder;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Represents any data event occurring in the Mule environment. All data sent or received within the mule environment will be
 * passed between components as an Event.
 * <p>
 * Holds a Message payload and provides helper methods for obtaining the data in a format that the receiving Mule component understands. The
 * event can also maintain any number of properties that can be set and retrieved by Mule components.
 *
 * @see Message
 */
public interface Event extends Serializable, org.mule.runtime.api.event.Event {

  class CurrentEventHolder {

    private static final ThreadLocal<Event> currentEvent = new ThreadLocal<>();
  }

  /**
   * @return the context applicable to all events created from the same root {@link Event} from a {@link MessageSource}.
   */
  EventContext getInternalContext();

  /**
   * Internal parameters used by the runtime to pass information around.
   * 
   */
  Map<String, ?> getInternalParameters();

  /**
   * Returns the correlation metadata of this message. See {@link GroupCorrelation}.
   *
   * @return the correlation metadata of this message.
   */
  Optional<GroupCorrelation> getGroupCorrelation();

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
   * @deprecated TODO MULE-10013 remove this
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
   * Indicates if notifications should be fired when processing this message.
   *
   * @return true if notifications are enabled, false otherwise
   */
  boolean isNotificationsEnabled();

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
   * @return the correlation id to use for this event.
   * @deprecated TODO MULE-10706 Mule 4: remove this
   */
  @Deprecated
  String getLegacyCorrelationId();

  /**
   * Create new {@link Builder}.
   *
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context) {
    return new DefaultEventBuilder(context);
  }

  /**
   * Create new {@link Builder} based on an existing {@link Event} instance. The existing {@link EventContext} is conserved.
   *
   * @param event existing event to use as a template to create builder instance
   * @return new builder instance.
   */
  static Builder builder(Event event) {
    return new DefaultEventBuilder(event);
  }

  /**
   * Create new {@link Builder} based on an existing {@link Event} instance and and {@link EventContext}. A new
   * {@link EventContext} is used instead of the existing instance referenced by the existing {@link Event}. This builder should
   * only be used in some specific scenarios like {@code flow-ref} where a new Flow executing the same {@link Event} needs a new
   * context.
   *
   * @param event existing event to use as a template to create builder instance
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context, Event event) {
    return new DefaultEventBuilder(context, event);
  }

  interface Builder {

    /**
     * Sets the configuration provided by the {@link org.mule.runtime.api.event.Event} into the builder
     *
     * @param event the event to get the data from
     * @return the builder instance
     */
    Builder from(org.mule.runtime.api.event.Event event);

    /**
     * Set the {@link Message} to construct {@link Event} with.
     *
     * @param message the message instance.
     * @return the builder instance
     */
    Builder message(Message message);

    /**
     * Set a map of variables. Any existing variables added to the builder will be removed.
     *
     * @param variables variables to be set.
     * @return the builder instance
     */
    Builder variables(Map<String, ?> variables);

    /**
     * Add a variable.
     *
     * @param key the key of the variable to add.
     * @param value the value of the variable to add. {@code null} values are supported.
     * @return the builder instance.
     */
    Builder addVariable(String key, Object value);

    /**
     * Add a variable.
     *
     * @param key the key of the variable to add.
     * @param value the value of the variable to add. {@code null} values are supported.
     * @param mediaType additional metadata about the {@code value} type.
     * @return the builder instance
     */
    Builder addVariable(String key, Object value, DataType mediaType);

    /**
     * Remove a variable.
     *
     * @param key the variable key.
     * @return the builder instance
     */
    Builder removeVariable(String key);

    /**
     * Set a map of properties to be consumed within a
     * {@link ModuleOperationMessageProcessorChainBuilder.ModuleOperationProcessorChain}.
     * <p>
     * For every module's <operation/> being consumed in a Mule Application, when being macro expanded, these properties will be
     * feed to it in a new and isolated {@link Event}, so that we can guarantee that for each invocation there's a real variable
     * scoping for them.
     *
     * @param properties properties to be set.
     * @return the builder instance
     * @see #parameters(Map)
     */
    Builder properties(Map<String, Object> properties);

    /**
     * Set a map of parameters to be consumed within a
     * {@link ModuleOperationMessageProcessorChainBuilder.ModuleOperationProcessorChain}.
     * <p>
     * For every module's <operation/> being consumed in a Mule Application, when being macro expanded, these parameters will be
     * feed to it in a new and isolated {@link Event}, so that we can guarantee that for each invocation there's a real variable
     * scoping for them.
     *
     * @param parameters parameters to be set.
     * @return the builder instance
     * @see #properties(Map)
     */
    Builder parameters(Map<String, Object> parameters);

    /**
     * Add a parameter.
     *
     * @param key the key of the parameter to add.
     * @param value the value of the variable to add. {@code null} values are supported.
     * @return the builder instance.
     */
    Builder addParameter(String key, Object value);

    /**
     * Add a parameter.
     *
     * @param key the key of the parameter to add.
     * @param value the value of the parameter to add. {@code null} values are supported.
     * @param dataType additional metadata about the {@code value} type.
     * @return the builder instance
     */
    Builder addParameter(String key, Object value, DataType dataType);

    /**
     * Remove a parameter.
     * <p>
     * 
     * @see #parameters(Map)
     *
     * @param key the parameter key.
     * @return the builder instance
     */
    Builder removeParameter(String key);

    /**
     * Set a map of parameters to be internal by the runtime to pass information within the context of an event
     *
     * @param internalParameters parameters to be set.
     * @return the builder instance
     */
    Builder internalParameters(Map<String, ?> internalParameters);

    /**
     * Adds an internal parameter.
     *
     * @param key the parameter key
     * @param value the parameter value
     * @return the builder instance
     */
    Builder addInternalParameter(String key, Object value);

    /**
     * Remove a internal parameter.
     *
     * @param key the parameter key.
     * @return the builder instance
     */
    Builder removeInternalParameter(String key);

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
     * Sets the group correlation information to the produced event.
     *
     * @param groupCorrelation the object containing the group correlation information to set on the produced event
     * @return the builder instance
     */
    Builder groupCorrelation(Optional<GroupCorrelation> groupCorrelation);

    /**
     * Sets an error related to the produced event.
     *
     * @param error the error associated with the produced event
     * @return the builder instance
     */
    Builder error(Error error);

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
     *
     * @param flow
     * @return the builder instance
     * @deprecated TODO MULE-10013 remove this
     */
    @Deprecated
    Builder flow(FlowConstruct flow);

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
     * Build a new {@link Event} based on the state configured in the {@link Builder}.
     *
     * @return new {@link Event} instance.
     */
    Event build();

  }

  /**
   * Helper method to get the value of a given variable in a null-safe manner such that {@code null} is returned for non-existent
   * variables rather than a {@link NoSuchElementException} exception being thrown.
   * 
   * @param key the key of the variable to retrieve.
   * @param event the event from which to retrieve a variable with the given key.
   * @param <T> the variable type
   * @return the value of the variables if it exists otherwise {@code null}.
   */
  static <T> T getVariableValueOrNull(String key, Event event) {
    if (event.getVariables().containsKey(key)) {
      return (T) event.getVariables().get(key).getValue();
    } else {
      return null;
    }
  }

  /**
   * Return the event associated with the currently executing thread.
   *
   * @return event for currently executing thread.
   */
  static Event getCurrentEvent() {
    return CurrentEventHolder.currentEvent.get();
  }

  /**
   * Set the event to be associated with the currently executing thread.
   *
   * @param event event for currently executing thread.
   */
  static void setCurrentEvent(Event event) {
    CurrentEventHolder.currentEvent.set(event);
  }

}
