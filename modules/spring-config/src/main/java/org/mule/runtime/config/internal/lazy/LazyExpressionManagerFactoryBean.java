/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import org.mule.runtime.config.internal.DefaultExpressionManagerFactoryBean;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;

/**
 * Creates a lazy {@link org.mule.runtime.core.api.el.ExpressionManager}.
 * <p>
 * This factory creates a proxy on top of the real expression manager. That proxy is used to set the right classloader on the
 * current thread's context classloader before calling any method on the delegate object.
 *
 * @since 4.0
 */
public class LazyExpressionManagerFactoryBean extends DefaultExpressionManagerFactoryBean {

  @Override
  protected DefaultExpressionManager createBaseObject() {
    return new LazyExpressionManager();
  }

}
