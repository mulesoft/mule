/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.api;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.AbstractTlsContextFactoryBuilderFactory;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;

/**
 * Default implementation of {@link AbstractTlsContextFactoryBuilderFactory} which has a default TLS context. This is injected into
 * each new {@link TlsContextFactoryBuilder} so that a single instance is exposed.
 *
 * @since 4.0
 */
public class DefaultTlsContextFactoryBuilderFactory extends AbstractTlsContextFactoryBuilderFactory {

  private TlsContextFactory defaultTlsContextFactory = new DefaultTlsContextFactory(emptyMap());

  public DefaultTlsContextFactoryBuilderFactory() {
    try {
      initialiseIfNeeded(defaultTlsContextFactory);
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(createStaticMessage("Failed to initialise default TlsContextFactory"), e);
    }
  }

  @Override
  protected TlsContextFactoryBuilder create() {
    return new org.mule.runtime.module.tls.internal.DefaultTlsContextFactoryBuilder(defaultTlsContextFactory);
  }

}
