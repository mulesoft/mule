/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 * This exception is thrown when the underlying to an {@link ObjectStore}'s system fails.
 */
public class ObjectStoreNotAvaliableException extends ObjectStoreException {

  public ObjectStoreNotAvaliableException() {
    super();
  }

  public ObjectStoreNotAvaliableException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public ObjectStoreNotAvaliableException(I18nMessage message) {
    super(message);
  }

  public ObjectStoreNotAvaliableException(Throwable cause) {
    super(cause);
  }
}


