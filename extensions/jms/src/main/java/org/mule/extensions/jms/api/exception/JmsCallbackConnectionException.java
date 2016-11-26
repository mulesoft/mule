/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Custom {@link MuleRuntimeException} to be used in a JMS {@link Connection} {@link ExceptionListener}
 *
 * @since 4.0
 */
public final class JmsCallbackConnectionException extends MuleRuntimeException {

  public JmsCallbackConnectionException(JMSException e) {
    super(e);
  }
}
