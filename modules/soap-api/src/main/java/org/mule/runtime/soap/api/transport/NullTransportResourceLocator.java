/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
public final class NullTransportResourceLocator implements TransportResourceLocator {

  @Override
  public boolean handles(String url) {
    return false;
  }

  @Override
  public InputStream getResource(String url) {
    throw new UnsupportedOperationException("Null implementation does not support this operation");
  }
}
