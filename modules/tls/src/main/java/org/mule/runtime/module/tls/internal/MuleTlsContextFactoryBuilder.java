/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;

public class MuleTlsContextFactoryBuilder implements TlsContextFactoryBuilder {

  private TlsContextFactory defaultTlsContextFactory;

  public MuleTlsContextFactoryBuilder(TlsContextFactory defaultTlsContextFactory) {
    this.defaultTlsContextFactory = defaultTlsContextFactory;
    System.out.println("TLS CONTEXT is: " + defaultTlsContextFactory.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TlsContextFactory buildDefault() {
    return defaultTlsContextFactory;
  }

}
