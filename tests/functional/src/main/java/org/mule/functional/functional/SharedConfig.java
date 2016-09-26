/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

/**
 * 
 *
 * @since 4.0
 */
public class SharedConfig extends AbstractAnnotatedObject implements Initialisable, MuleContextAware {

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
