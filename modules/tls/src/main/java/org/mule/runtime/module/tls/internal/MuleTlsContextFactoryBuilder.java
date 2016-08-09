/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.tls.api.DefaultTlsContextFactoryBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

@DefaultTlsContextFactoryBuilder
public class MuleTlsContextFactoryBuilder implements TlsContextFactoryBuilder, Initialisable, MuleContextAware {

  private TlsContextFactory defaultTlsContextFactory;
  private MuleContext muleContext;
  private final AtomicBoolean initialised = new AtomicBoolean(false);

  /**
   * Creates a default {@link TlsContextFactory} and registers it under key
   * {@link MuleProperties#DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY}
   *
   * @throws InitialisationException if the {@link #defaultTlsContextFactory} could not be created or registered
   */
  @Override
  public void initialise() throws InitialisationException {
    if (!initialised.compareAndSet(false, true)) {
      return;
    }

    try {
      defaultTlsContextFactory = new DefaultTlsContextFactory();
      muleContext.getRegistry().registerObject(MuleProperties.DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY, defaultTlsContextFactory);
    } catch (Exception e) {
      throw new InitialisationException(createStaticMessage("Failed to create default "
          + TlsContextFactory.class.getSimpleName()), e, this);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TlsContextFactory buildDefault() {
    return defaultTlsContextFactory;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
