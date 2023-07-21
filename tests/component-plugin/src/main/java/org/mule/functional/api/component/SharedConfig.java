/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

/**
 * Test config to test connectors at the domain level
 *
 * @since 4.0
 */
public class SharedConfig extends AbstractComponent implements Initialisable, MuleContextAware {

  private MuleContext muleContext;
  private String name;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;

  }

  @Override
  public void initialise() throws InitialisationException {
    // Nothing to do
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
