/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.Set;

public class WithMuleContextValueProvider implements ValueProvider, MuleContextAware {

  private MuleContext context;

  @Override
  public Set<Value> resolve() {
    if (context != null) {
      return ValueBuilder.getValuesFor("INJECTED!!!");
    } else {
      throw new RuntimeException();
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.context = context;
  }
}
