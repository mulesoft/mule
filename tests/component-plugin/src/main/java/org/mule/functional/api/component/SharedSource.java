/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Test source that is configured with a {@link SharedConfig}.
 *
 * @since 4.0
 */
public class SharedSource extends AbstractComponent
    implements MessageSource, Initialisable, MuleContextAware {

  private MuleContext muleContext;
  private SharedConfig config;
  private Processor listener;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    // Nothing to do
  }

  public void setConfig(SharedConfig config) {
    this.config = config;
  }

  public SharedConfig getConfig() {
    return config;
  }

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }
}
