/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.el;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;

/**
 * Provides a {@link BindingContext} that should be considered global, meaning {@link Binding}s are not expected to change. Allows
 * to expose {@link ExpressionFunction}s and general variables. Implementations should be registered to take effect.
 *
 * @since 4.0
 */
@NoImplement
public interface GlobalBindingContextProvider {

  /**
   * @return a {@link BindingContext} that should be considered global.
   */
  BindingContext getBindingContext();

}
