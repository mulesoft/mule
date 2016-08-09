/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.management.stats.ProcessingTime;

import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Legacy implementation of {@link org.mule.runtime.api.message.MuleEvent}
 * <p/>
 * Holds a MuleMessage payload and provides helper methods for obtaining the data in a format that the receiving Mule component
 * understands. The event can also maintain any number of properties that can be set and retrieved by Mule components.
 *
 * @see org.mule.runtime.api.message.MuleEvent
 * @see MuleMessage
 * @deprecated Use {@link org.mule.runtime.api.message.MuleEvent} instead
 */
@Deprecated
public interface MuleEvent extends org.mule.runtime.api.message.MuleEvent {

  int TIMEOUT_WAIT_FOREVER = 0;
  int TIMEOUT_NOT_SET_VALUE = Integer.MIN_VALUE;

  /**
   * Returns the message payload for this event
   * 
   * @return the message payload for this event
   */
  @Override
  MuleMessage getMessage();

  Credentials getCredentials();

  /**
   * Returns the contents of the message as a byte array.
   * 
   * @return the contents of the message as a byte array
   * @throws MuleException if the message cannot be converted into an array of bytes
   */
  byte[] getMessageAsBytes() throws MuleException;

  /**
   * Transforms the message into the requested format. The transformer used is the one configured on the endpoint through which
   * this event was received.
   * 
   * @param outputType The requested output type.
   * @return the message transformed into it's recognised or expected format.
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer if the transform fails or the outputtype is null
   */
  <T> T transformMessage(Class<T> outputType) throws TransformerException;

  /**
   * Transforms the message into the requested format. The transformer used is the one configured on the endpoint through which
   * this event was received.
   * 
   * @param outputType The requested output type.
   * @return the message transformed into it's recognised or expected format.
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer if the transform fails or the outputtype is null
   */
  Object transformMessage(DataType outputType) throws TransformerException;

  /**
   * Returns the message transformed into it's recognised or expected format and then into a String. The transformer used is the
   * one configured on the endpoint through which this event was received. If necessary this will use the encoding set on the
   * event
   * 
   * @return the message transformed into it's recognised or expected format as a Strings.
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer
   */
  String transformMessageToString() throws TransformerException;

  /**
   * Returns the message contents as a string If necessary this will use the encoding set on the event
   * 
   * @return the message contents as a string
   * @throws MuleException if the message cannot be converted into a string
   */
  String getMessageAsString() throws MuleException;

  /**
   * Returns the message contents as a string
   * 
   * @param encoding the encoding to use when converting the message to string
   * @return the message contents as a string
   * @throws MuleException if the message cannot be converted into a string
   */
  String getMessageAsString(Charset encoding) throws MuleException;

  /**
   * Retrieves the service session for the current event
   * 
   * @return the service session for the event
   */
  MuleSession getSession();

  /**
   * Retrieves the service for the current event
   * 
   * @return the service for the event
   */
  FlowConstruct getFlowConstruct();

  /**
   * Determines whether the default processing for this event will be executed. By default, the Mule server will route events
   * according to a components configuration. The user can override this behaviour by obtaining a reference to the MuleEvent
   * context, either by implementing <code>org.mule.runtime.core.api.lifecycle.Callable</code> or calling
   * <code>RequestContext.getEventContext</code> to obtain the MuleEventContext for the current thread. The user can
   * programmatically control how events are dispatched.
   * 
   * @return Returns true is the user has set stopFurtherProcessing.
   * @see org.mule.runtime.core.api.MuleContext
   * @see MuleEventContext
   * @see org.mule.runtime.core.api.lifecycle.Callable
   */
  boolean isStopFurtherProcessing();

  /**
   * Determines whether the default processing for this event will be executed. By default, the Mule server will route events
   * according to a components configuration. The user can override this behaviour by obtaining a reference to the MuleEvent
   * context, either by implementing <code>org.mule.runtime.core.api.lifecycle.Callable</code> or calling
   * <code>RequestContext.getEventContext</code> to obtain the MuleEventContext for the current thread. The user can
   * programmatically control how events are dispached.
   * 
   * @param stopFurtherProcessing the value to set.
   */
  void setStopFurtherProcessing(boolean stopFurtherProcessing);

  /**
   * The number of milliseconds to wait for a return event when running synchronously. 0 wait forever -1 try and receive, but do
   * not wait or a positive millisecond value
   * 
   * @return the event timeout in milliseconds
   */
  int getTimeout();

  /**
   * The number of milliseconds to wait for a return event when running synchronously. 0 wait forever -1 try and receive, but do
   * not wait or a positive millisecod value
   * 
   * @param timeout the event timeout in milliseconds
   */
  void setTimeout(int timeout);

  /**
   * An outputstream the can optionally be used write response data to an incoming message.
   * 
   * @return an output strem if one has been made available by the message receiver that received the message
   */
  OutputStream getOutputStream();

  /**
   * Returns the muleContext for the Mule node that this event was received in
   * 
   * @return the muleContext for the Mule node that this event was received in
   */
  MuleContext getMuleContext();

  /**
   * Returns the times spent processing this event (so far)
   */
  ProcessingTime getProcessingTime();

  /**
   * Returns the message exchange pattern for this event
   */
  MessageExchangePattern getExchangePattern();

  /**
   * Returns true is this event is being processed in a transaction
   */
  boolean isTransacted();

  /**
   * Returns the {@link URI} of the MessageSource that recieved or generated the message being processed.
   */
  URI getMessageSourceURI();

  /**
   * Returns the message source name if it has one, otherwise returns toString() of the URI returned be getMessageSourceURI()
   */
  String getMessageSourceName();

  /**
   * Return the replyToHandler (if any) that will be used to perform async reply
   */
  ReplyToHandler getReplyToHandler();

  /**
   * Return the destination (if any) that will be passed to the reply-to handler.
   */
  Object getReplyToDestination();

  boolean isSynchronous();

  void setMessage(MuleMessage message);

  /**
   * Gets the data type for a given flow variable
   *
   * @param key the name or key of the variable. This must be non-null.
   * @return the property data type or null if the flow variable does not exist
   */
  DataType getFlowVariableDataType(String key);

  Set<String> getFlowVariableNames();

  void clearFlowVariables();

  /**
   * Indicates if notifications should be fired when processing this message.
   *
   * @return true if notifications are enabled, false otherwise
   */
  boolean isNotificationsEnabled();

  /**
   * Enables the firing of notifications when processing the message.
   *
   * @param enabled
   */
  void setEnableNotifications(boolean enabled);

  /**
   * Indicates if the current event allows non-blocking execution and IO.
   *
   * @return true if non-blocking execution and IO is allowed. False otherwise.
   */
  boolean isAllowNonBlocking();

  /**
   * Events have a stack of executed flows (same as a call stack), so that at any given instant an application developer can
   * determine where this event came from.
   * <p/>
   * This will only be enabled if {@link DefaultMuleConfiguration#isFlowTrace()} is {@code true}. If {@code false}, the stack will
   * always be empty.
   * 
   * @return the flow stack associated to this event.
   * 
   * @since 3.8.0
   */
  FlowCallStack getFlowCallStack();

  /**
   * Events have a list of message processor paths it went trough so that the execution path of an event can be reconstructed
   * after it has executed.
   * <p/>
   * This will only be enabled if {@link DefaultMuleConfiguration#isFlowTrace()} is {@code true}. If {@code false}, the list will
   * always be empty.
   * 
   * @return the message processors trace associated to this event.
   * 
   * @since 3.8.0
   */
  ProcessorsTrace getProcessorsTrace();

  /**
   * The security context for this session. If not null outbound, inbound and/or method invocations will be authenticated using
   * this context
   *
   * @return the context for this session or null if the request is not secure.
   */
  SecurityContext getSecurityContext();

  /**
   * The security context for this session. If not null outbound, inbound and/or method invocations will be authenticated using
   * this context
   *
   * @param context the context for this session or null if the request is not secure.
   */
  void setSecurityContext(SecurityContext context);

}
