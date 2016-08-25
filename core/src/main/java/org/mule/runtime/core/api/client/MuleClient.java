/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.client;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.functional.Either;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * Provides methods for performing send, dispatch and request operations programatically.
 */
public interface MuleClient {

  /**
   * @deprecated use {@link #dispatch(String, org.mule.api.MuleMessage)} instead
   *
   *             Dispatches an event asynchronously to a endpointUri via a Mule server. The URL determines where to dispatch the
   *             event to.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of JMS you could set the JMSReplyTo
   *        property in these properties.
   * @throws org.mule.api.MuleException
   */
  @Deprecated
  void dispatch(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException;

  /**
   * Dispatches an event asynchronously to a endpointUri via a Mule server. The URL determines where to dispatch the event to.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message the message to send
   * @throws org.mule.api.MuleException
   */
  void dispatch(String url, MuleMessage message) throws MuleException;

  /**
   * Dispatches an event asynchronously to a endpointUri via a Mule server. The URL determines where to dispatch the event to.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message the message to send
   * @param operationOptions the options to configure the operation
   * @throws org.mule.api.MuleException
   */
  void dispatch(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException;

  /**
   * @deprecated use {@link #send(String, org.mule.api.MuleMessage)} instead
   *
   *             Sends an event synchronously to a endpointUri via a Mule server and a resulting message is returned.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of Jms you could set the JMSReplyTo
   *        property in these properties.
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  @Deprecated
  Either<Error, MuleMessage> send(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException;

  /**
   *
   * Sends an event synchronously to a endpointUri via a Mule server and a resulting message is returned.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message the Message for the event
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  Either<Error, MuleMessage> send(String url, MuleMessage message) throws MuleException;

  /**
   *
   * Sends an event synchronously to a endpointUri via a Mule server and a resulting message is returned.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message the Message for the event
   * @param operationOptions the options to configure the operation
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  Either<Error, MuleMessage> send(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException;

  /**
   * @deprecated use {@link #send(String, org.mule.api.MuleMessage, OperationOptions)}
   *
   *             Sends an event synchronously to a endpointUri via a mule server and a resulting message is returned.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of Jms you could set the JMSReplyTo
   *        property in these properties.
   * @param timeout The time in milliseconds the the call should block waiting for a response
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  @Deprecated
  Either<Error, MuleMessage> send(String url, Object payload, Map<String, Serializable> messageProperties, long timeout)
      throws MuleException;

  /**
   * @deprecated use {@link #send(String, org.mule.api.MuleMessage, OperationOptions)} instead
   *
   *             Sends an event synchronously to a endpointUri via a mule server and a resulting message is returned.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message The message to send
   * @param timeout The time in milliseconds the the call should block waiting for a response
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  @Deprecated
  Either<Error, MuleMessage> send(String url, MuleMessage message, long timeout) throws MuleException;

  /**
   * Will receive an event from an endpointUri determined by the URL.
   * 
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param timeout how long to block waiting to receive the event, if set to 0 the receive will not wait at all and if set to -1
   *        the receive will wait forever
   * @return the message received or <code>null</code> if no message was received
   * @throws org.mule.api.MuleException
   */
  Either<Error, Optional<MuleMessage>> request(String url, long timeout) throws MuleException;

  /**
   * Will register the specified process as a listener for the inbound endpoint. This may be implemented by subscription or
   * polling depending on the transport implementation
   * 
   * @param url endpoint uri
   * @param processor the processor to register
   * @param frequency the polling frequency (if transport polls)
   * @throws MuleException
   */
  // void receive(String url, MessageProcessor processor, long frequency) throws
  // MuleException;

}
