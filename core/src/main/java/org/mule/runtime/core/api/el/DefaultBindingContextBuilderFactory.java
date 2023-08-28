/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.api.el.AbstractBindingContextBuilderFactory;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.core.internal.el.DefaultBindingContextBuilder;

public final class DefaultBindingContextBuilderFactory extends AbstractBindingContextBuilderFactory {

  @Override
  protected BindingContext.Builder create() {
    return new DefaultBindingContextBuilder();
  }

  @Override
  protected BindingContext.Builder create(BindingContext bindingContext) {
    return new DefaultBindingContextBuilder(bindingContext);
  }
}
