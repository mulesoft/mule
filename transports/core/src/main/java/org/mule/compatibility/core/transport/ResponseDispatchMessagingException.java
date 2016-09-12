/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.i18n.I18nMessage;

/**
 *
 */
class ResponseDispatchMessagingException extends MuleException {

  ResponseDispatchMessagingException(I18nMessage message) {
    super(message);
  }

  ResponseDispatchMessagingException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  ResponseDispatchMessagingException(Throwable cause) {
    super(cause);
  }

}
