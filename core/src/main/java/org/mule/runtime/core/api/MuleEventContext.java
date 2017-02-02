/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.nio.charset.Charset;

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
  Message getMessage();

  /**
   * Returns the event
   *
   * @return the event
   */
  Event getEvent();

  /**
   * Sets the event for this context.
   * 
   * @param event
   */
  void setEvent(Event event);

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
   * Returns the transaction for the current event or null if there is no transaction in progress
   * 
   * @return the transaction for the current event or null if there is no transaction in progress
   */
  Transaction getTransaction();

  MuleSession getSession();
}
