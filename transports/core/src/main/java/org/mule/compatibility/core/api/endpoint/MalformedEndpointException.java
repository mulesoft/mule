/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.endpoint;

import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.core.config.i18n.Message;

/**
 * <code>MalformedEndpointException</code> is thrown by the MuleEndpointURI class if it fails to parse a Url
 * 
 * @see org.mule.compatibility.core.endpoint.MuleEndpointURI
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class MalformedEndpointException extends EndpointException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -3179045414716505094L;

  /**
   * @param endpoint the endpoint that could not be parsed
   */
  public MalformedEndpointException(String endpoint) {
    super(TransportCoreMessages.endpointIsMalformed(endpoint));
  }

  /**
   * @param endpoint the endpoint that could not be parsed
   */
  public MalformedEndpointException(Message message, String endpoint) {
    super(TransportCoreMessages.endpointIsMalformed(endpoint).setNextMessage(message));
  }

  /**
   * @param endpoint the endpoint that could not be parsed
   * @param cause the exception that cause this exception to be thrown
   */
  public MalformedEndpointException(String endpoint, Throwable cause) {
    super(TransportCoreMessages.endpointIsMalformed(endpoint), cause);
  }

  public MalformedEndpointException(Throwable cause) {
    super(cause);
  }
}
