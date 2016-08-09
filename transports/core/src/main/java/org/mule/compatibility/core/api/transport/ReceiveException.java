/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.transport;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * <code>ReceiveException</code> is specifically thrown by the Provider receive method if something fails in the underlying
 * transport
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class ReceiveException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1960304517882133951L;

  private ImmutableEndpoint endpoint;

  /**
   * @param message the exception message
   */
  public ReceiveException(Message message, ImmutableEndpoint endpoint, long timeout) {
    super(message);
    this.endpoint = endpoint;
    addInfo("Endpoint", ObjectUtils.toString(this.endpoint, "null"));
    addInfo("Timeout", String.valueOf(timeout));
  }

  /**
   * @param message the exception message
   * @param cause the exception that cause this exception to be thrown
   */
  public ReceiveException(Message message, ImmutableEndpoint endpoint, long timeout, Throwable cause) {
    super(message, cause);
    this.endpoint = endpoint;
    addInfo("Endpoint", ObjectUtils.toString(this.endpoint, "null"));
    addInfo("Timeout", String.valueOf(timeout));
  }

  public ReceiveException(ImmutableEndpoint endpoint, long timeout, Throwable cause) {
    this(TransportCoreMessages.failedToRecevieWithTimeout(endpoint, timeout), endpoint, timeout, cause);
  }
}
