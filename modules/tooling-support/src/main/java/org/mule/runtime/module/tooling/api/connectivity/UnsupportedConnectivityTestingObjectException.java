/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.Message;

/**
 * Exception type that represents a failure when there's no {@link ConnectivityTestingService} that can do connectivity testing
 * over a provided component
 *
 * @since 4.0
 */
public class UnsupportedConnectivityTestingObjectException extends MuleRuntimeException {

  /**
   * {@inheritDoc}
   */
  public UnsupportedConnectivityTestingObjectException(Message message) {
    super(message);
  }
}
