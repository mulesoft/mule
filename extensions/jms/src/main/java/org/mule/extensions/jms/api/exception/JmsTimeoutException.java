/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Custom JMS Exception that represents an error when consuming a Message
 * but a timeout is reached before the Message arrives
 *
 * @since 4.0
 */
public class JmsTimeoutException extends JmsExtensionException {

  public JmsTimeoutException(I18nMessage message) {
    super(message);
  }
}
