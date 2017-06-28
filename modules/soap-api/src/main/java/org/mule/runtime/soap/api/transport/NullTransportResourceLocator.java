/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 *
 */
package org.mule.runtime.soap.api.transport;

import java.io.InputStream;

/**
 * Null implementation of {@link TransportResourceLocator}. It does not handle any resource.
 *
 * @since 4.0
 */
public class NullTransportResourceLocator implements TransportResourceLocator {

  @Override
  public boolean handles(String url) {
    return false;
  }

  @Override
  public InputStream getResource(String url) {
    throw new UnsupportedOperationException("Null implementation does not support this operation");
  }
}
