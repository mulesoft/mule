/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transformer.TransformerException;

/**
 * <code>MuleEventContext</code> is the context object for the current request. Using the context, developers can
 * send/dispatch/receive events programmatically as well as manage transactions.
 */
public interface MuleEventContext {

  /**
   * Returns the message payload for this event
   * 
   * @return the message payload for this event
   */
  MuleMessage getMessage();

  /**
   * Returns the event
   *
   * @return the event
   */
  MuleEvent getEvent();

  /**
   * Returns the message transformed into its recognised or expected format. The transformer used is the one configured on the
   * endpoint through which this event was received.
   *
   * @param dataType The dataType required for the return object. This param just provides a convienient way to manage type
   *        casting of transformed objects
   * @param muleContext the Mule node.
   * @return the message transformed into it's recognised or expected format.
   * @throws org.mule.runtime.core.api.transformer.TransformerException if a failure occurs or if the return type is not the same
   *         as the expected type in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer
   */
  Object transformMessage(DataType dataType, MuleContext muleContext) throws TransformerException;

  /**
   * Returns the message transformed into it's recognised or expected format. The transformer used is the one configured on the
   * endpoint through which this event was received.
   * 
   * @param expectedType The class type required for the return object. This param just provides a convienient way to manage type
   *        casting of transformed objects
   * @param muleContext the Mule node.
   * @return the message transformed into it's recognised or expected format.
   * @throws org.mule.runtime.core.api.transformer.TransformerException if a failure occurs or if the return type is not the same
   *         as the expected type in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer
   */
  Object transformMessage(Class expectedType, MuleContext muleContext) throws TransformerException;

  /**
   * Returns the message transformed into it's recognised or expected format and then into a String. The transformer used is the
   * one configured on the endpoint through which this event was received. This method will use the encoding set on the event
   * 
   * @param muleContext the Mule node.
   * @return the message transformed into it's recognised or expected format as a Strings.
   * @throws TransformerException if a failure occurs in the transformer
   * @see org.mule.runtime.core.api.transformer.Transformer
   */
  String transformMessageToString(MuleContext muleContext) throws TransformerException;

  /**
   * Returns the message contents as a string This method will use the encoding set on the event
   * 
   * @param muleContext the Mule node.
   * @return the message contents as a string
   * @throws MuleException if the message cannot be converted into a string
   */
  String getMessageAsString(MuleContext muleContext) throws MuleException;

  /**
   * Returns the message contents as a string
   * 
   * @param encoding The encoding to use when transforming the message
   * @param muleContext the Mule node.
   * @return the message contents as a string
   * @throws MuleException if the message cannot be converted into a string
   */
  String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException;

  /**
   * Returns the current transaction (if any) for the session
   * 
   * @return the current transaction for the session or null if there is no transaction in progress
   */
  Transaction getCurrentTransaction();

  FlowConstruct getFlowConstruct();

  /**
   * Determines whether the default processing for this event will be executed. By default, the Mule server will route events
   * according to a components configuration. The user can override this behaviour by obtaining a reference to the MuleEvent
   * context, either by implementing <code>org.mule.runtime.core.api.lifecycle.Callable</code> or calling
   * <code>UMOManager.getEventContext</code> to obtain the MuleEventContext for the current thread. The user can programmatically
   * control how events are dispatched.
   * 
   * @param stopFurtherProcessing the value to set.
   */
  void setStopFurtherProcessing(boolean stopFurtherProcessing);

  /**
   * An output stream the can optionally be used write response data to an incoming message.
   * 
   * @return an output strem if one has been made available by the message receiver that received the message
   */
  OutputStream getOutputStream();

  MessageExchangePattern getExchangePattern();

  /**
   * Returns a reference to the Endpoint Uri for this context This is the endpoint on which the event was received
   * 
   * @return the receive endpoint for this event context
   */
  URI getEndpointURI();

  /**
   * Returns the transaction for the current event or null if there is no transaction in progress
   * 
   * @return the transaction for the current event or null if there is no transaction in progress
   */
  Transaction getTransaction();

  MuleSession getSession();
}
