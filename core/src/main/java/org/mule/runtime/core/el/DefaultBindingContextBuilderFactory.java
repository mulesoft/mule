/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import org.mule.runtime.api.el.AbstractBindingContextBuilderFactory;
import org.mule.runtime.api.el.BindingContext;

public class DefaultBindingContextBuilderFactory extends AbstractBindingContextBuilderFactory {

  @Override
  protected BindingContext.Builder create() {
    return new DefaultBindingContextBuilder();
  }

  @Override
  protected BindingContext.Builder create(BindingContext bindingContext) {
    return new DefaultBindingContextBuilder(bindingContext);
  }
}
