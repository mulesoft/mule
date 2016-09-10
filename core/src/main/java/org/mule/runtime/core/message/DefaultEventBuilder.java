/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;


import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.DefaultReplyToHandler;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEventBuilder implements Event.Builder {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMessageBuilder.class);

  private EventContext context;
  private InternalMessage message;
  private Map<String, DefaultTypedValue<Object>> flowVariables = new HashMap<>();
  private Error error;
  private MessageExchangePattern exchangePattern = REQUEST_RESPONSE;
  private FlowConstruct flow;
  private GroupCorrelation groupCorrelation = new GroupCorrelation(null, null);
  private String legacyCorrelationId;
  private FlowCallStack flowCallStack = new DefaultFlowCallStack();
  private ReplyToHandler replyToHandler;
  private Object replyToDestination;
  private boolean transacted;
  private Boolean synchronous;
  private boolean nonBlocking;
  private MuleSession session = new DefaultMuleSession();
  private Event originalEvent;
  private boolean modified;
  private boolean notificationsEnabled = true;

  public DefaultEventBuilder(EventContext messageContext) {
    this.context = messageContext;
  }

  public DefaultEventBuilder(Event event) {
    this.originalEvent = event;
    this.context = event.getContext();
    this.message = event.getMessage();
    this.flow = event.getFlowConstruct();
    this.groupCorrelation = event.getGroupCorrelation();
    if (event instanceof MuleEventImplementation) {
      this.legacyCorrelationId = ((MuleEventImplementation) event).getLegacyCorrelationId();
    }

    this.flowCallStack = event.getFlowCallStack().clone();

    this.exchangePattern = event.getExchangePattern();

    this.replyToHandler = event.getReplyToHandler();
    this.replyToDestination = event.getReplyToDestination();
    this.message = event.getMessage();

    if (event.isSynchronous()) {
      this.synchronous = event.isSynchronous();
    }
    this.transacted = event.isTransacted();
    this.nonBlocking = event.isAllowNonBlocking();

    this.session = event.getSession();
    this.error = event.getError().orElse(null);

    this.notificationsEnabled = event.isNotificationsEnabled();

    event.getVariableNames().forEach(key -> this.flowVariables
        .put(key, new DefaultTypedValue<>(event.getVariable(key), event.getVariableDataType(key))));
  }

  @Override
  public Event.Builder message(InternalMessage message) {
    this.message = message;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder variables(Map<String, Object> flowVariables) {
    flowVariables.forEach((s, o) -> this.flowVariables.put(s, new DefaultTypedValue<>(o, DataType.fromObject(o))));
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder addVariable(String key, Object value) {
    flowVariables.put(key, new DefaultTypedValue<>(value, DataType.fromObject(value)));
    this.modified = true;
    return this;

  }

  @Override
  public Event.Builder addVariable(String key, Object value, DataType dataType) {
    flowVariables.put(key, new DefaultTypedValue<>(value, dataType));
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder removeVariable(String key) {
    flowVariables.remove(key);
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder correlationId(String correlationId) {
    legacyCorrelationId = correlationId;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder groupCorrelation(GroupCorrelation correlation) {
    this.groupCorrelation = correlation;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder error(Error error) {
    this.error = error;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder synchronous(boolean synchronous) {
    this.synchronous = synchronous;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder exchangePattern(MessageExchangePattern exchangePattern) {
    this.exchangePattern = exchangePattern;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder flow(FlowConstruct flow) {
    this.flow = flow;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder replyToHandler(ReplyToHandler replyToHandler) {
    this.replyToHandler = replyToHandler;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder replyToDestination(Object replyToDestination) {
    this.replyToDestination = replyToDestination;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder transacted(boolean transacted) {
    this.transacted = transacted;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder session(MuleSession session) {
    this.session = session;
    this.modified = true;
    return this;
  }

  @Override
  public Builder disableNotifications() {
    this.notificationsEnabled = false;
    this.modified = true;
    return this;
  }

  @Override
  @Deprecated
  public Event.Builder refreshSync() {
    this.synchronous = resolveEventSynchronicity();
    this.nonBlocking = isFlowConstructNonBlockingProcessingStrategy();

    this.modified = true;
    return this;
  }

  @Override
  public Event build() {
    if (originalEvent != null && !modified) {
      return originalEvent;
    } else {
      return new MuleEventImplementation(context, message, flowVariables, exchangePattern, flow, session, transacted,
                                         synchronous == null ? (resolveEventSynchronicity() && replyToHandler == null)
                                             : synchronous,
                                         nonBlocking || isFlowConstructNonBlockingProcessingStrategy(), replyToDestination,
                                         replyToHandler, flowCallStack, groupCorrelation, error, legacyCorrelationId,
                                         notificationsEnabled);
    }
  }

  protected boolean resolveEventSynchronicity() {
    return transacted
        || isFlowConstructSynchronous()
        || exchangePattern != null && exchangePattern.hasResponse() && !isFlowConstructNonBlockingProcessingStrategy();
  }

  private boolean isFlowConstructSynchronous() {
    return (flow instanceof ProcessingDescriptor) && ((ProcessingDescriptor) flow)
        .isSynchronous();
  }

  protected boolean isFlowConstructNonBlockingProcessingStrategy() {
    return (flow instanceof Pipeline)
        && ((Pipeline) flow).getProcessingStrategy() instanceof NonBlockingProcessingStrategy;
  }

  /**
   * <code>MuleEventImplementation</code> represents any data event occurring in the Mule environment. All data sent or received
   * within the Mule environment will be passed between components as an MuleEvent.
   * <p>
   * The MuleEvent holds some data and provides helper methods for obtaining the data in a format that the receiving Mule
   * component understands. The event can also maintain any number of flowVariables that can be set and retrieved by Mule
   * components.
   */
  public static class MuleEventImplementation implements Event, DeserializationPostInitialisable {

    private static final long serialVersionUID = 1L;

    /** Immutable MuleEvent state **/

    private EventContext context;
    private InternalMessage message;
    private final MuleSession session;
    private transient FlowConstruct flowConstruct;

    protected MessageExchangePattern exchangePattern;
    private final ReplyToHandler replyToHandler;
    protected boolean transacted;
    protected boolean synchronous;

    /** Mutable MuleEvent state **/
    private Object replyToDestination;

    private boolean notificationsEnabled = true;

    private CopyOnWriteCaseInsensitiveMap<String, DefaultTypedValue> flowVariables = new CopyOnWriteCaseInsensitiveMap<>();

    private FlowCallStack flowCallStack = new DefaultFlowCallStack();
    protected boolean nonBlocking;
    private String legacyCorrelationId;
    private Error error;

    // Use this constructor from the builder
    public MuleEventImplementation(EventContext context, InternalMessage message,
                                   Map<String, DefaultTypedValue<Object>> flowVariables,
                                   MessageExchangePattern exchangePattern, FlowConstruct flowConstruct, MuleSession session,
                                   boolean transacted, boolean synchronous, boolean nonBlocking, Object replyToDestination,
                                   ReplyToHandler replyToHandler, FlowCallStack flowCallStack, GroupCorrelation groupCorrelation,
                                   Error error, String legacyCorrelationId, boolean notificationsEnabled) {
      this.context = context;
      this.flowConstruct = flowConstruct;
      this.session = session;
      this.message = message;
      flowVariables
          .forEach((s, value) -> this.flowVariables.put(s, new DefaultTypedValue<>(value.getContent(), value.getDataType())));

      this.exchangePattern = exchangePattern;
      this.replyToHandler = replyToHandler;
      this.replyToDestination = replyToDestination;
      this.transacted = transacted;
      this.synchronous = synchronous;
      this.nonBlocking = nonBlocking;

      this.flowCallStack = flowCallStack;

      this.groupCorrelation = groupCorrelation;
      this.error = error;
      this.legacyCorrelationId = legacyCorrelationId;

      this.notificationsEnabled = notificationsEnabled;
    }

    @Override
    public EventContext getContext() {
      return context;
    }

    @Override
    public InternalMessage getMessage() {
      return message;
    }

    @Override
    public Optional<Error> getError() {
      return ofNullable(error);
    }

    @Override
    public byte[] getMessageAsBytes(MuleContext muleContext) throws DefaultMuleException {
      try {
        return (byte[]) transformMessage(DataType.BYTE_ARRAY, muleContext);
      } catch (Exception e) {
        throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsBytes(message.getPayload()
            .getClass()
            .getName()), e);
      }
    }

    @Override
    public <T> T transformMessage(Class<T> outputType, MuleContext muleContext) throws TransformerException {
      return (T) transformMessage(DataType.fromType(outputType), muleContext);
    }

    @Override
    public Object transformMessage(DataType outputType, MuleContext muleContext) throws TransformerException {
      if (outputType == null) {
        throw new TransformerException(CoreMessages.objectIsNull("outputType"));
      }

      InternalMessage transformedMessage = muleContext.getTransformationService().transform(message, outputType);
      if (message.getDataType().isStreamType()) {
        setMessage(transformedMessage);
      }
      return transformedMessage.getPayload();
    }

    /**
     * Returns the message transformed into it's recognised or expected format and then into a String. The transformer used is the
     * one configured on the endpoint through which this event was received.
     *
     * @return the message transformed into it's recognised or expected format as a Strings.
     * @throws org.mule.runtime.core.api.transformer.TransformerException if a failure occurs in the transformer
     * @see org.mule.runtime.core.api.transformer.Transformer
     */
    @Override
    public String transformMessageToString(MuleContext muleContext) throws TransformerException {
      final DataType dataType = DataType.builder(getMessage().getDataType()).type(String.class).build();
      return (String) transformMessage(dataType, muleContext);
    }

    @Override
    public String getMessageAsString(MuleContext muleContext) throws MuleException {
      return getMessageAsString(getMessage().getDataType().getMediaType().getCharset()
          .orElse(getDefaultEncoding(muleContext)), muleContext);
    }

    /**
     * Returns the message contents for logging
     *
     * @param encoding the encoding to use when converting bytes to a string, if necessary
     * @param muleContext the Mule node.
     * @return the message contents as a string
     * @throws org.mule.runtime.core.api.MuleException if the message cannot be converted into a string
     */
    @Override
    public String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException {
      try {
        InternalMessage transformedMessage = muleContext.getTransformationService()
            .transform(message, DataType.builder().type(String.class).charset(encoding).build());
        if (message.getDataType().isStreamType()) {
          setMessage(transformedMessage);
        }

        return (String) transformedMessage.getPayload();
      } catch (Exception e) {
        throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsString(message.getClass().getName()), e);
      }
    }

    @Override
    public String toString() {
      return "DefaultMuleEvent{" +
          "context=" + context +
          ", message=" + message +
          ", flowVariables=" + flowVariables +
          ", error=" + error +
          '}';
    }

    @Override
    public MuleSession getSession() {
      return session;
    }

    @Override
    public FlowConstruct getFlowConstruct() {
      return flowConstruct;
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.runtime.core.util.store.DeserializationPostInitialisable} is used. This will get invoked after the object
     * has been deserialized passing in the current MuleContext when using either
     * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
     * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param muleContext the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    @SuppressWarnings({"unused"})
    private void initAfterDeserialisation(MuleContext muleContext) throws MuleException {
      if (message instanceof InternalMessage) {
        setMessage(message);
      }
      if (replyToHandler instanceof DefaultReplyToHandler) {
        ((DefaultReplyToHandler) replyToHandler).initAfterDeserialisation(muleContext);
      }
      if (replyToDestination instanceof DeserializationPostInitialisable) {
        try {
          DeserializationPostInitialisable.Implementation.init(replyToDestination, muleContext);
        } catch (Exception e) {
          throw new DefaultMuleException(e);
        }

      }
      // Can be null if service call originates from MuleClient
      if (context.getOriginatingFlowName() != null) {
        flowConstruct = muleContext.getRegistry().lookupFlowConstruct(context.getOriginatingFlowName());
      }
    }

    @Override
    public MuleContext getMuleContext() {
      return flowConstruct.getMuleContext();
    }

    @Override
    public MessageExchangePattern getExchangePattern() {
      return exchangePattern;
    }

    @Override
    public boolean isTransacted() {
      return transacted || TransactionCoordination.getInstance().getTransaction() != null;
    }

    @Override
    public ReplyToHandler getReplyToHandler() {
      return replyToHandler;
    }

    @Override
    public Object getReplyToDestination() {
      return replyToDestination;
    }

    @Override
    public boolean isSynchronous() {
      return synchronous;
    }

    // //////////////////////////
    // Serialization methods
    // //////////////////////////

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      for (Map.Entry<String, DefaultTypedValue> entry : flowVariables.entrySet()) {
        Object value = entry.getValue();
        if (value != null && !(value instanceof Serializable)) {
          String message = String.format(
                                         "Unable to serialize the flow variable %s, which is of type %s ", entry.getKey(), value);
          logger.error(message);
          throw new IOException(message);
        }
      }

    }

    private void setMessage(InternalMessage message) {
      this.message = message;
    }

    @Override
    public Set<String> getVariableNames() {
      return flowVariables.keySet();
    }

    @Override
    public <T> T getVariable(String key) {
      TypedValue typedValue = flowVariables.get(key);

      if (typedValue == null) {
        throw new NoSuchElementException("The flow variable '" + key + "' does not exist.");
      } else {
        return (T) typedValue.getContent();
      }
    }

    @Override
    public DataType getVariableDataType(String key) {
      TypedValue typedValue = flowVariables.get(key);

      if (typedValue == null) {
        throw new NoSuchElementException("The flow variable '" + key + "' does not exist.");
      } else {
        return typedValue.getDataType();
      }
    }

    @Override
    public boolean isNotificationsEnabled() {
      return notificationsEnabled;
    }

    @Override
    public boolean isAllowNonBlocking() {
      return nonBlocking && !synchronous;
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return flowCallStack;
    }

    @Override
    public SecurityContext getSecurityContext() {
      return session.getSecurityContext();
    }

    private GroupCorrelation groupCorrelation;

    @Override
    public GroupCorrelation getGroupCorrelation() {
      return groupCorrelation;
    }

    @Override
    public String getCorrelationId() {
      return legacyCorrelationId != null ? legacyCorrelationId : getContext().getCorrelationId();
    }

    private static final ThreadLocal<Event> currentEvent = new ThreadLocal<>();

    /**
     * Return the event associated with the currently executing thread.
     *
     * @return event for currently executing thread.
     */
    public static Event getCurrentEvent() {
      return currentEvent.get();
    }

    /**
     * Set the event to be associated with the currently executing thread.
     *
     * @param event event for currently executing thread.
     */
    public static void setCurrentEvent(Event event) {
      currentEvent.set(event);
    }

    /**
     * Obtain the correlationId set during flow execution if any. This is only used to support transports and should not be used
     * otherwise. Customization of the correlationId, if needed, should instead be done as part of the source connector
     * configuration.
     *
     * @return legacy correlationId if set, otherwise {@code null}.
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    public String getLegacyCorrelationId() {
      return this.legacyCorrelationId;
    }

    public static <T> T getFlowVariableOrNull(String key, Event event) {
      T value = null;
      try {
        value = event.getVariable(key);
      } catch (NoSuchElementException nsse) {
        // Ignore
      }
      return value;
    }

  }

}
