/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;

import org.mule.extension.ws.internal.WebServiceConsumer;

import org.apache.cxf.message.Message;

/**
 * This in an Adapter interface that allows that different transports (such as JMS or HTTP) behave the same way when dispatching a
 * Web Service operation message when using the {@link WebServiceConsumer}.
 *
 * @since 4.0
 */
public interface WscDispatcher {

  /**
   * Sends off a Soap {@link Message} to a destination and returns it's response.
   *
   * @return a {@link WscResponse} with the content returned by the transport and it's corresponding Content-Type.
   */
  WscResponse dispatch(Message message);

  /**
   * Disposes all the associated resources to this {@link WscDispatcher} instance.
   */
  void dispose();
}
