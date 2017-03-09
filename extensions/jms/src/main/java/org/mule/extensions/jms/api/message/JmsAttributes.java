/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.message;

import org.mule.extensions.jms.api.config.AckMode;
import org.mule.runtime.api.message.Attributes;

import java.util.Optional;

import javax.jms.Message;

/**
 * Contains all the metadata of a JMS {@link Message}, it carries information such as the Headers,
 * the Properties and the required ID for performing an ACK on the Message.
 * <p>
 *
 * @since 4.0
 */
public interface JmsAttributes extends Attributes {

  /**
   * @return the {@link Message} properties
   */
  JmsMessageProperties getProperties();

  /**
   * @return the {@link Message} headers
   */
  JmsHeaders getHeaders();

  /**
   * @return the session Id required to ACK a {@link Message} that was consumed using {@link AckMode#MANUAL}
   */
  String getAckId();

}
