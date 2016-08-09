/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import org.mule.runtime.extension.api.introspection.ModelProperty;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;

/**
 * A marker {@link ModelProperty} to signal that the owning {@link OperationModel} is intercepting
 *
 * @since 4.0
 */
public class InterceptingModelProperty implements ModelProperty {

  /**
   * @return {@code intercepting}
   */
  @Override
  public String getName() {
    return "intercepting";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isExternalizable() {
    return false;
  }
}
