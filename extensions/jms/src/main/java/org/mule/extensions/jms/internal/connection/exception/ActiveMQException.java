/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.exception;

import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.internal.connection.provider.activemq.ActiveMQConnectionProvider;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Generic {@link JmsExtensionException} thrown by the {@link ActiveMQConnectionProvider}
 * when an error occurs related to an {@link ActiveMQConnectionFactory}
 *
 * @since 4.0
 */
public class ActiveMQException extends JmsExtensionException {

  public ActiveMQException(String message) {
    super(message);
  }

  public ActiveMQException(String message, Exception exception) {
    super(exception, message);
  }
}
