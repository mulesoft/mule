/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_FORCE_SYNC_PROPERTY;
import static org.mule.runtime.core.message.Correlation.NO_CORRELATION;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
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
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DefaultMuleEvent</code> represents any data event occurring in the Mule environment. All data sent or received within the
 * Mule environment will be passed between components as an MuleEvent.
 * <p/>
 * The MuleEvent holds some data and provides helper methods for obtaining the data in a format that the receiving Mule component
 * understands. The event can also maintain any number of flowVariables that can be set and retrieved by Mule components.
 */

public class DefaultMuleEvent implements MuleEvent, DeserializationPostInitialisable {

  private static final long serialVersionUID = 1L;

  private static final ThreadLocal<MuleEvent> currentEvent = new ThreadLocal<>();
  private static Logger logger = LoggerFactory.getLogger(DefaultMuleEvent.class);

  /** Immutable MuleEvent state **/

  /**
   * The Universally Unique ID for the event
   * 
   * @deprecated TODO MULE-9281 remove when the event becomes immutable
   */
  @Deprecated
  private String id;
  private MessageContext context;
  private MuleMessage message;
  private final MuleSession session;
  private transient FlowConstruct flowConstruct;

  protected MessageExchangePattern exchangePattern;
  private final ReplyToHandler replyToHandler;
  protected boolean transacted;
  protected boolean synchronous;

  /** Mutable MuleEvent state **/
  private Object replyToDestination;

  private boolean notificationsEnabled = true;

  private CopyOnWriteCaseInsensitiveMap<String, TypedValue> flowVariables = new CopyOnWriteCaseInsensitiveMap<>();

  private FlowCallStack flowCallStack = new DefaultFlowCallStack();
  protected boolean nonBlocking;
  private String legacyCorrelationId;
  private Error error;

  // Constructors

  /**
   * Constructor used to create an event with no message source with minimal arguments
   */
  public DefaultMuleEvent(MessageContext context, MuleMessage message, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct) {
    this(context, message, exchangePattern, flowConstruct, new DefaultMuleSession());
  }

  /**
   * Constructor used to create an event with no message source with all additional arguments
   */
  public DefaultMuleEvent(MessageContext context, MuleMessage message, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session) {
    this(context, message, exchangePattern, flowConstruct, session, null);
  }

  /**
   * Constructor used to create an event with a identifiable message source with all additional arguments
   */
  public DefaultMuleEvent(MessageContext context, MuleMessage message, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session, ReplyToHandler replyToHandler) {
    this.context = context;
    this.correlation = NO_CORRELATION;
    this.id = generateEventId(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.exchangePattern = exchangePattern;
    this.replyToHandler = replyToHandler;
    this.replyToDestination = null;
    this.transacted = false;
    this.synchronous = resolveEventSynchronicity();
    this.nonBlocking = isFlowConstructNonBlockingProcessingStrategy();
  }

  // Constructors for inbound endpoint

  public DefaultMuleEvent(MessageContext context, MuleMessage message, FlowConstruct flowConstruct,
                          MuleSession session) {
    this(context, message, flowConstruct, session, null, null);
  }

  public DefaultMuleEvent(MessageContext context, MuleMessage message, FlowConstruct flowConstruct) {
    this(context, message, flowConstruct, new DefaultMuleSession(), null, null);
  }

  public DefaultMuleEvent(MessageContext context, MuleMessage message, FlowConstruct flowConstruct,
                          MuleSession session, ReplyToHandler replyToHandler, Object replyToDestination) {
    this.context = context;
    this.correlation = NO_CORRELATION;
    this.id = generateEventId(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    // TODO See MULE-9307 - define where to get these values from
    this.exchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
    this.transacted = false;

    this.synchronous = resolveEventSynchronicity();
  }

  // Constructors to copy MuleEvent

  /**
   * A helper constructor used to rewrite an event payload
   *
   * @param message The message to use as the current payload of the event
   * @param rewriteEvent the previous event that will be used as a template for this event
   */
  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent) {
    this(message, rewriteEvent, rewriteEvent.getSession());
  }

  /**
   * Copy constructor used when ReplyToHandler instance needs switching out
   *
   * @param rewriteEvent
   * @param replyToHandler
   */
  public DefaultMuleEvent(MuleEvent rewriteEvent, ReplyToHandler replyToHandler) {
    this(rewriteEvent, rewriteEvent.getFlowConstruct(), replyToHandler, null);
  }

  public DefaultMuleEvent(MuleEvent rewriteEvent, FlowConstruct flowConstruct, ReplyToHandler replyToHandler,
                          Object replyToDestination) {
    this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(),
         rewriteEvent.isSynchronous(), replyToHandler, replyToDestination, true, rewriteEvent.getExchangePattern());
  }

  public DefaultMuleEvent(MuleEvent rewriteEvent, FlowConstruct flowConstruct, ReplyToHandler replyToHandler,
                          Object replyToDestination, boolean synchronous) {
    this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(),
         synchronous, replyToHandler, replyToDestination, true, rewriteEvent.getExchangePattern());
  }

  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus);
  }

  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareFlowVars) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus,
         shareFlowVars, rewriteEvent.getExchangePattern(), rewriteEvent.getReplyToHandler());
  }

  /**
   * Copy constructor to be used when synchronicity and {@link org.mule.runtime.core.MessageExchangePattern} both need changing.
   */
  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareFlowVars,
                          MessageExchangePattern messageExchangePattern) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus,
         shareFlowVars, messageExchangePattern, rewriteEvent.getReplyToHandler());
  }

  /**
   * Copy constructor to be used when synchronicity, {@link org.mule.MessageExchangePattern} and {@link ReplyToHandler} all need
   * changing.
   */
  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareFlowVars,
                          MessageExchangePattern messageExchangePattern, ReplyToHandler replyToHandler) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus,
         shareFlowVars, messageExchangePattern, replyToHandler);
  }

  /**
   * A helper constructor used to rewrite an event payload
   *
   * @param message The message to use as the current payload of the event
   * @param rewriteEvent the previous event that will be used as a template for this event
   */
  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, MuleSession session) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), session, rewriteEvent.isSynchronous());
  }

  protected DefaultMuleEvent(MuleMessage message,
                             MuleEvent rewriteEvent,
                             FlowConstruct flowConstruct,
                             MuleSession session,
                             boolean synchronous) {
    this(message, rewriteEvent, flowConstruct, session, synchronous, rewriteEvent.getReplyToHandler(),
         rewriteEvent.getReplyToDestination(), true, rewriteEvent.getExchangePattern());
  }

  protected DefaultMuleEvent(MuleMessage message,
                             MuleEvent rewriteEvent,
                             FlowConstruct flowConstruct,
                             MuleSession session,
                             boolean synchronous,
                             boolean shareFlowVars,
                             MessageExchangePattern messageExchangePattern,
                             ReplyToHandler replyToHandler) {
    this(message, rewriteEvent, flowConstruct, session, synchronous, replyToHandler,
         rewriteEvent.getReplyToDestination(), shareFlowVars, messageExchangePattern);
  }

  protected DefaultMuleEvent(MuleMessage message,
                             MuleEvent rewriteEvent,
                             FlowConstruct flowConstruct,
                             MuleSession session,
                             boolean synchronous,
                             ReplyToHandler replyToHandler,
                             Object replyToDestination,
                             boolean shareFlowVars,
                             MessageExchangePattern messageExchangePattern) {
    this.context = rewriteEvent.getContext();
    this.correlation = rewriteEvent.getCorrelation();
    this.parent = rewriteEvent.getParent();
    this.id = rewriteEvent.getId();
    this.flowConstruct = flowConstruct;
    this.session = session;

    this.exchangePattern = messageExchangePattern;
    if (rewriteEvent instanceof DefaultMuleEvent) {
      if (shareFlowVars) {
        this.flowVariables = ((DefaultMuleEvent) rewriteEvent).flowVariables;
      } else {
        this.flowVariables.putAll(((DefaultMuleEvent) rewriteEvent).flowVariables);
      }
      this.legacyCorrelationId = ((DefaultMuleEvent) rewriteEvent).getLegacyCorrelationId();
    } else {
    }
    setMessage(message);
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    this.transacted = rewriteEvent.isTransacted();
    this.notificationsEnabled = rewriteEvent.isNotificationsEnabled();
    this.synchronous = synchronous;
    this.nonBlocking = rewriteEvent.isAllowNonBlocking() || isFlowConstructNonBlockingProcessingStrategy();
    this.flowCallStack =
        rewriteEvent.getFlowCallStack() == null ? new DefaultFlowCallStack() : rewriteEvent.getFlowCallStack().clone();
    this.error = rewriteEvent.getError();
  }

  // Use this constructor from the builder
  public DefaultMuleEvent(MessageContext context, MuleMessage message, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session, boolean transacted, boolean synchronous,
                          boolean nonBlocking, Object replyToDestination, ReplyToHandler replyToHandler,
                          FlowCallStack flowCallStack) {
    this.context = context;
    this.correlation = NO_CORRELATION;
    // this.id = generateEventId(flowConstruct.getMuleContext());
    this.id = "" + System.identityHashCode(this);
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.exchangePattern = exchangePattern;
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    this.transacted = transacted;
    this.synchronous = synchronous;
    this.nonBlocking = nonBlocking;

    this.flowCallStack = flowCallStack;
  }

  protected boolean resolveEventSynchronicity() {
    return transacted
        || isFlowConstructSynchronous()
        || exchangePattern.hasResponse() && !isFlowConstructNonBlockingProcessingStrategy()
        || message.getInboundProperty(MULE_FORCE_SYNC_PROPERTY, Boolean.FALSE);
  }

  private boolean isFlowConstructSynchronous() {
    return (flowConstruct instanceof ProcessingDescriptor) && ((ProcessingDescriptor) flowConstruct)
        .isSynchronous();
  }

  protected boolean isFlowConstructNonBlockingProcessingStrategy() {
    return (flowConstruct instanceof Pipeline)
        && ((Pipeline) flowConstruct).getProcessingStrategy() instanceof NonBlockingProcessingStrategy;
  }

  @Override
  public MessageContext getContext() {
    return context;
  }

  // TODO MULE-9281 Remove when the builder is in place.
  @Deprecated
  public void setContext(MessageContext executionContext) {
    this.context = executionContext;
  }

  @Override
  public MuleMessage getMessage() {
    return message;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Error getError() {
    return error;
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

    MuleMessage transformedMessage = muleContext.getTransformationService().transform(message, outputType);
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
      MuleMessage transformedMessage = muleContext.getTransformationService()
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
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("MuleEvent: ").append(getId());
    buf.append(", correlation=").append(getCorrelation().toString());

    return buf.toString();
  }

  protected String generateEventId(MuleContext context) {
    return context.getUniqueIdString();
  }

  @Override
  public MuleSession getSession() {
    return session;
  }

  /**
   * Gets the recipient service of this event
   */
  @Override
  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  @Override
  // TODO MULE- 9281 Remove once MuleEvent is immutable
  @Deprecated
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    DefaultMuleEvent that = (DefaultMuleEvent) o;
    return Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return 29 * id.hashCode() + (message != null ? message.hashCode() : 0);
  }

  /**
   * Invoked after deserialization. This is called when the marker interface
   * {@link org.mule.runtime.core.util.store.DeserializationPostInitialisable} is used. This will get invoked after the object has
   * been deserialized passing in the current MuleContext when using either
   * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
   * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
   * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
   *
   * @param muleContext the current muleContext instance
   * @throws MuleException if there is an error initializing
   */
  @SuppressWarnings({"unused"})
  private void initAfterDeserialisation(MuleContext muleContext) throws MuleException {
    if (message instanceof MuleMessage) {
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
    for (Map.Entry<String, TypedValue> entry : flowVariables.entrySet()) {
      Object value = entry.getValue();
      if (value != null && !(value instanceof Serializable)) {
        String message = String.format(
                                       "Unable to serialize the flow variable %s, which is of type %s ", entry.getKey(), value);
        logger.error(message);
        throw new IOException(message);
      }
    }

  }

  @Override
  public void setMessage(MuleMessage message) {
    this.message = message;
  }

  /**
   * This method does a complete deep copy of the event.
   *
   * This method should be used whenever the event is going to be executed in a different context and changes to that event must
   * not effect the source event.
   *
   * @param event the event that must be copied
   * @return the copied event
   */
  public static MuleEvent copy(MuleEvent event) {
    DefaultMuleEvent eventCopy = new DefaultMuleEvent(event.getMessage(), event, new DefaultMuleSession(event.getSession()));
    eventCopy.flowVariables = ((DefaultMuleEvent) event).flowVariables.clone();
    return eventCopy;
  }

  @Override
  public Set<String> getFlowVariableNames() {
    return flowVariables.keySet();
  }

  @Override
  public void clearFlowVariables() {
    flowVariables.clear();
  }

  @Override
  public <T> T getFlowVariable(String key) {
    TypedValue typedValue = flowVariables.get(key);

    if (typedValue == null) {
      throw new NoSuchElementException("The flow variable '" + key + "' does not exist.");
    } else {
      return (T) typedValue.getValue();
    }
  }

  @Override
  public DataType getFlowVariableDataType(String key) {
    TypedValue typedValue = flowVariables.get(key);

    if (typedValue == null) {
      throw new NoSuchElementException("The flow variable '" + key + "' does not exist.");
    } else {
      return typedValue.getDataType();
    }
  }

  @Override
  public void setFlowVariable(String key, Object value) {
    setFlowVariable(key, value, value != null ? DataType.fromObject(value) : DataType.OBJECT);
  }

  @Override
  public void setFlowVariable(String key, Object value, DataType dataType) {
    flowVariables.put(key, new TypedValue(value, dataType));
  }

  @Override
  public void removeFlowVariable(String key) {
    flowVariables.remove(key);
  }

  @Override
  public boolean isNotificationsEnabled() {
    return notificationsEnabled;
  }

  @Override
  public void setEnableNotifications(boolean enabled) {
    notificationsEnabled = enabled;
  }

  @Override
  public boolean isAllowNonBlocking() {
    return nonBlocking && !synchronous;
  }

  @Override
  public FlowCallStack getFlowCallStack() {
    return flowCallStack;
  }

  /**
   * @deprecated This should be used only by the compatibility module.
   * @param encoding
   * @param exchangePattern
   * @param name
   * @param uri
   * @param transacted
   */
  @Deprecated
  public void setEndpointFields(Charset encoding, MessageExchangePattern exchangePattern, boolean transacted) {
    if (!message.getDataType().getMediaType().getCharset().isPresent() && encoding != null) {
      this.message = MuleMessage.builder(message)
          .mediaType(DataType.builder(message.getDataType()).charset(encoding).build().getMediaType())
          .build();
    }
    this.exchangePattern = exchangePattern;
    this.transacted = transacted;

    this.synchronous = resolveEventSynchronicity();
    this.nonBlocking = isFlowConstructNonBlockingProcessingStrategy();
  }

  @Override
  public SecurityContext getSecurityContext() {
    return session.getSecurityContext();
  }

  @Override
  public void setSecurityContext(SecurityContext context) {
    session.setSecurityContext(context);
  }

  private MuleEvent parent;
  private Correlation correlation;

  public void setParent(MuleEvent parent) {
    this.parent = parent;
  }

  @Override
  public MuleEvent getParent() {
    return parent;
  }

  public void setCorrelation(Correlation correlation) {
    this.correlation = correlation;
  }

  @Override
  public Correlation getCorrelation() {
    return correlation;
  }

  @Override
  public String getCorrelationId() {
    return legacyCorrelationId != null ? legacyCorrelationId : getContext().getCorrelationId()
        + (getParent() != null && !getParent().getFlowCallStack().getElements().isEmpty()
            ? ":" + getParent().getFlowCallStack().getElements().get(0).getProcessorPath() : "")
        + getCorrelation().getSequence().map(s -> ":" + s.toString()).orElse("");
  }

  /**
   * Return the event associated with the currently executing thread.
   *
   * @return event for currently executing thread.
   */
  public static MuleEvent getCurrentEvent() {
    return currentEvent.get();
  }

  /**
   * Set the event to be associated with the currently executing thread.
   *
   * @param event event for currently executing thread.
   */
  public static void setCurrentEvent(MuleEvent event) {
    currentEvent.set(event);
  }

  public void setLegacyCorrelationId(String legacyCorrelationId) {
    this.legacyCorrelationId = legacyCorrelationId;
  }

  public String getLegacyCorrelationId() {
    return this.legacyCorrelationId;
  }

  public static <T> T getFlowVariableOrNull(String key, MuleEvent event) {
    T value = null;
    try {
      value = event.getFlowVariable(key);
    } catch (NoSuchElementException nsse) {
      // Ignore
    }
    return value;
  }

  @Override
  public void setError(Error error) {
    this.error = error;
  }
}
