/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * This exception class is thrown in cases when an exception occurs while operating on an {@link ObjectStore}.
 */
public class ObjectStoreException extends MuleException {

  public ObjectStoreException() {
    super();
  }

  public ObjectStoreException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public ObjectStoreException(I18nMessage message) {
    super(message);
  }

  public ObjectStoreException(Throwable cause) {
    super(cause);
  }
}
