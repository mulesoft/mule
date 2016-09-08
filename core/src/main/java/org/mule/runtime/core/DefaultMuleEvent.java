/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Optional.ofNullable;
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
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.DefaultReplyToHandler;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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

  // Use this constructor from the builder
  public DefaultMuleEvent(MessageContext context, MuleMessage message, Map<String, TypedValue<Object>> flowVariables,
                          MessageExchangePattern exchangePattern, FlowConstruct flowConstruct, MuleSession session,
                          boolean transacted, boolean synchronous, boolean nonBlocking, Object replyToDestination,
                          ReplyToHandler replyToHandler, FlowCallStack flowCallStack, Correlation correlation, Error error,
                          String legacyCorrelationId, boolean notificationsEnabled) {
    this.context = context;
    this.correlation = NO_CORRELATION;
    this.flowConstruct = flowConstruct;
    this.session = session;
    this.message = message;
    flowVariables.forEach((s, value) -> this.flowVariables.put(s, new TypedValue<>(value.getValue(), value.getDataType())));

    this.exchangePattern = exchangePattern;
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    this.transacted = transacted;
    this.synchronous = synchronous;
    this.nonBlocking = nonBlocking;

    this.flowCallStack = flowCallStack;

    this.correlation = correlation;
    this.error = error;
    this.legacyCorrelationId = legacyCorrelationId;

    this.notificationsEnabled = notificationsEnabled;
  }

  @Override
  public MessageContext getContext() {
    return context;
  }

  @Override
  public MuleMessage getMessage() {
    return message;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * Gets the recipient service of this event
   */
  @Override
  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
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

  private void setMessage(MuleMessage message) {
    this.message = message;
  }

  @Override
  public Set<String> getFlowVariableNames() {
    return flowVariables.keySet();
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

  private Correlation correlation;

  @Override
  public Correlation getCorrelation() {
    return correlation;
  }

  @Override
  public String getCorrelationId() {
    return legacyCorrelationId != null ? legacyCorrelationId : getContext().getCorrelationId();
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

  /**
   * Obtain the correlationId set during flow execution if any. This is only used to support transports and should not be used
   * otherwise. Customization of the correlationId, if needed, should instead be done as part of the source connector
   * configuration.
   *
   * @return legacy correlationId if set, otherwise {@code null}.
   */
  @Deprecated
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

}
