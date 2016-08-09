/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.mule.runtime.core.api.config.MuleProperties.ENDPOINT_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CREDENTIALS_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_FORCE_SYNC_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METHOD_PROPERTY;
import static org.mule.runtime.core.util.ClassUtils.isConsumable;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.DefaultReplyToHandler;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.context.notification.DefaultProcessorsTrace;
import org.mule.runtime.core.management.stats.ProcessingTime;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.security.MuleCredentials;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
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

  private static Logger logger = LoggerFactory.getLogger(DefaultMuleEvent.class);

  /** Immutable MuleEvent state **/

  /** The Universally Unique ID for the event */
  private final String id;
  private MuleMessage message;
  private final MuleSession session;
  private transient FlowConstruct flowConstruct;

  protected Credentials credentials;
  protected MessageExchangePattern exchangePattern;
  protected URI messageSourceURI;
  protected String messageSourceName;
  private final ReplyToHandler replyToHandler;
  protected boolean transacted;
  protected boolean synchronous;

  /** Mutable MuleEvent state **/
  private boolean stopFurtherProcessing = false;
  protected int timeout = TIMEOUT_NOT_SET_VALUE;
  private transient OutputStream outputStream;
  private final ProcessingTime processingTime;
  private Object replyToDestination;

  protected String[] ignoredPropertyOverrides = new String[] {MULE_METHOD_PROPERTY};
  private boolean notificationsEnabled = true;

  private transient Map<String, Object> serializedData = null;

  private CopyOnWriteCaseInsensitiveMap<String, TypedValue> flowVariables = new CopyOnWriteCaseInsensitiveMap<>();

  private FlowCallStack flowCallStack = new DefaultFlowCallStack();
  private ProcessorsTrace processorsTrace = new DefaultProcessorsTrace();
  protected boolean nonBlocking;

  // Constructors

  /**
   * Constructor used to create an event with no message source with minimal arguments and a
   * {@link org.mule.runtime.core.api.MuleSession}
   */
  public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, FlowConstruct flowConstruct,
                          MuleSession session) {
    this(message, exchangePattern, flowConstruct, session,
         flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout(), null, null);
  }

  /**
   * Constructor used to create an event with no message source with minimal arguments
   */
  public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, FlowConstruct flowConstruct) {
    this(message, exchangePattern, flowConstruct, new DefaultMuleSession(),
         flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout(), null, null);
  }

  /**
   * Constructor used to create an event with no message source with minimal arguments and a
   * {@link org.mule.runtime.core.api.connector.ReplyToHandler}
   */
  public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, ReplyToHandler replyToHandler,
                          FlowConstruct flowConstruct) {
    this(message, URI.create("none"), exchangePattern, flowConstruct, new DefaultMuleSession(),
         flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout(), null, null, replyToHandler);
  }

  /**
   * Constructor used to create an event with no message source with minimal arguments and OutputStream
   */
  public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, FlowConstruct flowConstruct,
                          MuleSession session, OutputStream outputStream) {
    this(message, exchangePattern, flowConstruct, session,
         flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout(), null, outputStream);
  }

  /**
   * Constructor used to create an event with no message source with all additional arguments
   */
  public DefaultMuleEvent(MuleMessage message, MessageExchangePattern exchangePattern, FlowConstruct flowConstruct,
                          MuleSession session, int timeout, Credentials credentials, OutputStream outputStream) {
    this(message, URI.create("none"), exchangePattern, flowConstruct, session, timeout, credentials, outputStream);
  }

  /**
   * Constructor used to create an event with a uri that idendifies the message source with minimal arguments
   */
  public DefaultMuleEvent(MuleMessage message, URI messageSourceURI, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session) {
    this(message, messageSourceURI, exchangePattern, flowConstruct, session,
         flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout(), null, null);
  }

  /**
   * Constructor used to create an event with a uri that idendifies the message source with minimal arguments and OutputStream
   */
  public DefaultMuleEvent(MuleMessage message, URI messageSourceURI, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session, OutputStream outputStream) {
    this(message, messageSourceURI, exchangePattern, flowConstruct, session,
         flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout(), null, outputStream);
  }

  /**
   * Constructor used to create an event with a identifiable message source with all additional arguments
   */
  public DefaultMuleEvent(MuleMessage message, URI messageSourceURI, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session, int timeout, Credentials credentials,
                          OutputStream outputStream, ReplyToHandler replyToHandler) {
    this.id = generateEventId(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.exchangePattern = exchangePattern;
    this.outputStream = outputStream;
    this.credentials = null;
    this.messageSourceName = messageSourceURI.toString();
    this.messageSourceURI = messageSourceURI;
    this.processingTime = ProcessingTime.newInstance(this);
    this.replyToHandler = replyToHandler;
    this.replyToDestination = null;
    this.timeout = timeout;
    this.transacted = false;
    this.synchronous = resolveEventSynchronicity();
    this.nonBlocking = isFlowConstructNonBlockingProcessingStrategy();
  }

  /**
   * Constructor used to create an event with a identifiable message source with all additional arguments except a
   * {@link org.mule.runtime.core.api.connector.ReplyToHandler}
   */
  public DefaultMuleEvent(MuleMessage message, URI messageSourceURI, MessageExchangePattern exchangePattern,
                          FlowConstruct flowConstruct, MuleSession session, int timeout, Credentials credentials,
                          OutputStream outputStream) {
    this(message, messageSourceURI, exchangePattern, flowConstruct, session, timeout, credentials, outputStream, null);
  }

  // Constructors for inbound endpoint

  public DefaultMuleEvent(MuleMessage message, FlowConstruct flowConstruct, MuleSession session) {
    this(message, flowConstruct, session, null, null, null);
  }

  public DefaultMuleEvent(MuleMessage message, FlowConstruct flowConstruct) {
    this(message, flowConstruct, new DefaultMuleSession(), null, null, null);
  }

  public DefaultMuleEvent(MuleMessage message, FlowConstruct flowConstruct, MuleSession session, ReplyToHandler replyToHandler,
                          Object replyToDestination, OutputStream outputStream) {
    this.id = generateEventId(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.outputStream = outputStream;
    this.processingTime = ProcessingTime.newInstance(this);
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    // TODO See MULE-9307 - define where to get these values from
    this.credentials = null;
    this.exchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
    this.messageSourceName = null;
    this.messageSourceURI = null;
    this.timeout = 0;
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

  public DefaultMuleEvent(MuleEvent rewriteEvent, FlowConstruct flowConstruct) {
    this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(), rewriteEvent.isSynchronous());
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
    this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(), rewriteEvent.isSynchronous(),
         replyToHandler, replyToDestination, true, rewriteEvent.getExchangePattern());
  }

  public DefaultMuleEvent(MuleEvent rewriteEvent, FlowConstruct flowConstruct, ReplyToHandler replyToHandler,
                          Object replyToDestination, boolean synchronous) {
    this(rewriteEvent.getMessage(), rewriteEvent, flowConstruct, rewriteEvent.getSession(), synchronous, replyToHandler,
         replyToDestination, true, rewriteEvent.getExchangePattern());
  }

  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus);
  }

  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareFlowVars) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus, shareFlowVars,
         rewriteEvent.getExchangePattern(), rewriteEvent.getReplyToHandler());
  }

  /**
   * Copy constructor to be used when synchronicity and {@link org.mule.runtime.core.MessageExchangePattern} both need changing.
   */
  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareFlowVars,
                          MessageExchangePattern messageExchangePattern) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus, shareFlowVars,
         messageExchangePattern, rewriteEvent.getReplyToHandler());
  }

  /**
   * Copy constructor to be used when synchronicity, {@link org.mule.MessageExchangePattern} and {@link ReplyToHandler} all need
   * changing.
   */
  public DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, boolean synchronus, boolean shareFlowVars,
                          MessageExchangePattern messageExchangePattern, ReplyToHandler replyToHandler) {
    this(message, rewriteEvent, rewriteEvent.getFlowConstruct(), rewriteEvent.getSession(), synchronus, shareFlowVars,
         messageExchangePattern, replyToHandler);
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

  protected DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, FlowConstruct flowConstruct, MuleSession session,
                             boolean synchronous) {
    this(message, rewriteEvent, flowConstruct, session, synchronous, rewriteEvent.getReplyToHandler(),
         rewriteEvent.getReplyToDestination(), true, rewriteEvent.getExchangePattern());
  }

  protected DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, FlowConstruct flowConstruct, MuleSession session,
                             boolean synchronous, boolean shareFlowVars, MessageExchangePattern messageExchangePattern,
                             ReplyToHandler replyToHandler) {
    this(message, rewriteEvent, flowConstruct, session, synchronous, replyToHandler, rewriteEvent.getReplyToDestination(),
         shareFlowVars, messageExchangePattern);
  }

  protected DefaultMuleEvent(MuleMessage message, MuleEvent rewriteEvent, FlowConstruct flowConstruct, MuleSession session,
                             boolean synchronous, ReplyToHandler replyToHandler, Object replyToDestination, boolean shareFlowVars,
                             MessageExchangePattern messageExchangePattern) {
    this.id = rewriteEvent.getId();
    this.flowConstruct = flowConstruct;
    this.session = session;

    this.credentials = rewriteEvent.getCredentials();
    this.exchangePattern = messageExchangePattern;
    this.messageSourceName = rewriteEvent.getMessageSourceName();
    this.messageSourceURI = rewriteEvent.getMessageSourceURI();
    this.outputStream = rewriteEvent.getOutputStream();
    if (rewriteEvent instanceof DefaultMuleEvent) {
      this.processingTime = ((DefaultMuleEvent) rewriteEvent).processingTime;
      if (shareFlowVars) {
        this.flowVariables = ((DefaultMuleEvent) rewriteEvent).flowVariables;
      } else {
        this.flowVariables.putAll(((DefaultMuleEvent) rewriteEvent).flowVariables);
      }
    } else {
      this.processingTime = ProcessingTime.newInstance(this);
    }
    setMessage(message);
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    this.timeout = rewriteEvent.getTimeout();
    this.transacted = rewriteEvent.isTransacted();
    this.notificationsEnabled = rewriteEvent.isNotificationsEnabled();
    this.synchronous = synchronous;
    this.nonBlocking = rewriteEvent.isAllowNonBlocking() || isFlowConstructNonBlockingProcessingStrategy();
    this.flowCallStack =
        rewriteEvent.getFlowCallStack() == null ? new DefaultFlowCallStack() : rewriteEvent.getFlowCallStack().clone();
    // We want parallel paths of the same flows (i.e.: async events) to contribute to this list and be available at the end, so we
    // copy only the reference.
    this.processorsTrace = rewriteEvent.getProcessorsTrace();
  }

  public DefaultMuleEvent(MuleMessage message, URI messageSourceURI, String messageSourceName,
                          MessageExchangePattern exchangePattern, FlowConstruct flowConstruct, MuleSession session, int timeout,
                          Credentials credentials, OutputStream outputStream, boolean transacted, Object replyToDestination,
                          ReplyToHandler replyToHandler) {
    this.id = generateEventId(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.credentials = credentials;
    this.exchangePattern = exchangePattern;
    this.messageSourceURI = messageSourceURI;
    this.messageSourceName = messageSourceName;
    this.processingTime = ProcessingTime.newInstance(this);
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    this.transacted = transacted;
    this.synchronous = resolveEventSynchronicity() && replyToHandler == null;
    this.nonBlocking = isFlowConstructNonBlockingProcessingStrategy();
    this.timeout = timeout;
    this.outputStream = outputStream;
  }

  // Constructor with everything just in case

  public DefaultMuleEvent(MuleMessage message, URI messageSourceURI, String messageSourceName,
                          MessageExchangePattern exchangePattern, FlowConstruct flowConstruct, MuleSession session, int timeout,
                          Credentials credentials, OutputStream outputStream, boolean transacted, boolean synchronous,
                          Object replyToDestination, ReplyToHandler replyToHandler) {
    this.id = generateEventId(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.session = session;
    setMessage(message);

    this.credentials = credentials;
    this.exchangePattern = exchangePattern;
    this.messageSourceURI = messageSourceURI;
    this.messageSourceName = messageSourceName;
    this.processingTime = ProcessingTime.newInstance(this);
    this.replyToHandler = replyToHandler;
    this.replyToDestination = replyToDestination;
    this.transacted = transacted;
    this.synchronous = synchronous;
    this.nonBlocking = isFlowConstructNonBlockingProcessingStrategy();
    this.timeout = timeout;
    this.outputStream = outputStream;
  }

  protected boolean resolveEventSynchronicity() {
    return transacted || isFlowConstructSynchronous()
        || exchangePattern.hasResponse() && !isFlowConstructNonBlockingProcessingStrategy()
        || message.getInboundProperty(MULE_FORCE_SYNC_PROPERTY, Boolean.FALSE);
  }

  private boolean isFlowConstructSynchronous() {
    return (flowConstruct instanceof ProcessingDescriptor) && ((ProcessingDescriptor) flowConstruct).isSynchronous();
  }

  protected boolean isFlowConstructNonBlockingProcessingStrategy() {
    return (flowConstruct instanceof Pipeline)
        && ((Pipeline) flowConstruct).getProcessingStrategy() instanceof NonBlockingProcessingStrategy;
  }

  /**
   * This method is used to determine if a property on the previous event should be ignored for the next event. This method is
   * here because we don't have proper scoped handling of meta data yet The rules are
   * <ol>
   * <li>If a property is already set on the current event don't overwrite with the previous event value
   * <li>If the property name appears in the ignoredPropertyOverrides list, then we always set it on the new event
   * </ol>
   *
   * @param key The name of the property to ignore
   * @return true if the property should be ignored, false otherwise
   */
  public boolean ignoreProperty(String key) {
    if (key == null || key.startsWith(ENDPOINT_PROPERTY_PREFIX)) {
      return true;
    }

    for (String ignoredPropertyOverride : ignoredPropertyOverrides) {
      if (key.equals(ignoredPropertyOverride)) {
        return false;
      }
    }

    return null != message.getOutboundProperty(key);
  }

  @Override
  public Credentials getCredentials() {
    MuleCredentials creds = message.getOutboundProperty(MULE_CREDENTIALS_PROPERTY);
    return (credentials != null ? credentials : creds);
  }

  @Override
  public MuleMessage getMessage() {
    return message;
  }

  @Override
  public byte[] getMessageAsBytes() throws DefaultMuleException {
    try {
      return (byte[]) transformMessage(DataType.BYTE_ARRAY);
    } catch (Exception e) {
      throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsBytes(message.getPayload().getClass().getName()), e);
    }
  }

  @Override
  public <T> T transformMessage(Class<T> outputType) throws TransformerException {
    return (T) transformMessage(DataType.fromType(outputType));
  }

  @Override
  public Object transformMessage(DataType outputType) throws TransformerException {
    if (outputType == null) {
      throw new TransformerException(CoreMessages.objectIsNull("outputType"));
    }

    MuleMessage transformedMessage = getMuleContext().getTransformationService().transform(message, outputType);
    if (isConsumable(message.getDataType().getType())) {
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
  public String transformMessageToString() throws TransformerException {
    final DataType dataType = DataType.builder(getMessage().getDataType()).type(String.class).build();
    return (String) transformMessage(dataType);
  }

  @Override
  public String getMessageAsString() throws MuleException {
    return getMessageAsString(getMessage().getDataType().getMediaType().getCharset()
        .orElse(getDefaultEncoding(getMuleContext())));
  }

  /**
   * Returns the message contents for logging
   *
   * @param encoding the encoding to use when converting bytes to a string, if necessary
   * @return the message contents as a string
   * @throws org.mule.runtime.core.api.MuleException if the message cannot be converted into a string
   */
  @Override
  public String getMessageAsString(Charset encoding) throws MuleException {
    try {
      MuleMessage transformedMessage = getMuleContext().getTransformationService()
          .transform(message, DataType.builder().type(String.class).charset(encoding).build());
      if (isConsumable(message.getDataType().getType())) {
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
    buf.append(", stop processing=").append(isStopFurtherProcessing());
    buf.append(", ").append(messageSourceURI);

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

  /**
   * Determines whether the default processing for this event will be executed
   *
   * @return Returns the stopFurtherProcessing.
   */
  @Override
  public boolean isStopFurtherProcessing() {
    return stopFurtherProcessing;
  }

  /**
   * Setting this parameter will stop the Mule framework from processing this event in the standard way. This allow for client
   * code to override default behaviour. The common reasons for doing this are - 1. The service has more than one send endpoint
   * configured; the service must dispatch to other prviders programmatically by using the service on the current event 2. The
   * service doesn't send the current event out through a endpoint. i.e. the processing of the event stops in the uMO.
   *
   * @param stopFurtherProcessing The stopFurtherProcessing to set.
   */
  @Override
  public void setStopFurtherProcessing(boolean stopFurtherProcessing) {
    this.stopFurtherProcessing = stopFurtherProcessing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultMuleEvent)) {
      return false;
    }

    final DefaultMuleEvent event = (DefaultMuleEvent) o;

    if (message != null ? !message.equals(event.message) : event.message != null) {
      return false;
    }
    return id.equals(event.id);
  }

  @Override
  public int hashCode() {
    return 29 * id.hashCode() + (message != null ? message.hashCode() : 0);
  }

  @Override
  public int getTimeout() {
    if (getMuleContext().getConfiguration().isDisableTimeouts()) {
      return TIMEOUT_WAIT_FOREVER;
    }
    if (timeout == TIMEOUT_NOT_SET_VALUE) {
      return flowConstruct.getMuleContext().getConfiguration().getDefaultResponseTimeout();
    } else {
      return timeout;
    }
  }

  @Override
  public void setTimeout(int timeout) {
    if (timeout >= 0) {
      this.timeout = timeout;
    }
  }

  /**
   * An output stream can optionally be used to write response data to an incoming message.
   *
   * @return an output strem if one has been made available by the message receiver that received the message
   */
  @Override
  public OutputStream getOutputStream() {
    return outputStream;
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
    // this method can be called even on objects that were not serialized. In this case,
    // the temporary holder for serialized data is not initialized and we can just return
    if (serializedData == null) {
      return;
    }

    String serviceName = this.getTransientServiceName();
    // Can be null if service call originates from MuleClient
    if (serviceName != null) {
      flowConstruct = muleContext.getRegistry().lookupFlowConstruct(serviceName);
    }
    serializedData = null;
  }

  @Override
  public MuleContext getMuleContext() {
    return flowConstruct.getMuleContext();
  }

  @Override
  public ProcessingTime getProcessingTime() {
    return processingTime;
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
  public URI getMessageSourceURI() {
    return messageSourceURI;
  }

  @Override
  public String getMessageSourceName() {
    return messageSourceName;
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
    // Can be null if service call originates from MuleClient
    if (serializedData != null) {
      Object serviceName = serializedData.get("serviceName");
      if (serviceName != null) {
        out.writeObject(serviceName);
      }
    } else {
      if (getFlowConstruct() != null) {
        out.writeObject(getFlowConstruct() != null ? getFlowConstruct().getName() : "null");
      }
    }
    for (Map.Entry<String, TypedValue> entry : flowVariables.entrySet()) {
      Object value = entry.getValue();
      if (value != null && !(value instanceof Serializable)) {
        String message = String.format("Unable to serialize the flow variable %s, which is of type %s ", entry.getKey(), value);
        logger.error(message);
        throw new IOException(message);
      }
    }

  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    serializedData = new HashMap<>();

    try {
      // Optional
      this.setTransientServiceName(in.readObject());
    } catch (OptionalDataException e) {
      // ignore
    }
  }

  /**
   * Used to fetch the {@link #flowConstruct} after deserealization since its a transient value. This is not part of the public
   * API and should only be used internally for serialization/deserialization
   *
   * @param serviceName the name of the service
   */
  public void setTransientServiceName(Object serviceName) {
    if (serializedData == null) {
      serializedData = new HashMap<>();
    }
    serializedData.put("serviceName", serviceName);
  }

  private String getTransientServiceName() {
    return serializedData != null ? (String) serializedData.get("serviceName") : null;
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
  @SuppressWarnings("unchecked")
  public <T> T getFlowVariable(String key) {
    TypedValue typedValue = flowVariables.get(key);

    return typedValue == null ? null : (T) typedValue.getValue();
  }

  @Override
  public DataType getFlowVariableDataType(String key) {
    TypedValue typedValue = flowVariables.get(key);

    return typedValue == null ? null : typedValue.getDataType();
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

  @Override
  public ProcessorsTrace getProcessorsTrace() {
    return processorsTrace;
  }

  /**
   * @deprecated This should be used only by the compatibility module.
   * @param credentials
   * @param encoding
   * @param exchangePattern
   * @param name
   * @param uri
   * @param timeout
   * @param transacted
   */
  @Deprecated
  public void setEndpointFields(Credentials credentials, Charset encoding, MessageExchangePattern exchangePattern, String name,
                                URI uri, int timeout, boolean transacted) {
    this.credentials = credentials;
    if (!message.getDataType().getMediaType().getCharset().isPresent() && encoding != null) {
      this.message = MuleMessage.builder(message)
          .mediaType(DataType.builder(message.getDataType()).charset(encoding).build().getMediaType()).build();
    }
    this.exchangePattern = exchangePattern;
    this.messageSourceName = name;
    this.messageSourceURI = uri;
    this.timeout = timeout;
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
}
