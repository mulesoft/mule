/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
