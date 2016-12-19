/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.source.SourceModel;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * A {@link ModelProperty} to be used in {@link SourceModel} instances, which
 * indicates that the source is listening for responses on the indicated methods.
 *
 * @since 4.0
 */
public final class SourceCallbackModelProperty implements ModelProperty {

  private final Optional<Method> onSuccessMethod;
  private final Optional<Method> onErrorMethod;

  /**
   * Creates a new instance
   *
   * @param onSuccessMethod an {@link Optional} with a reference to the success callback method
   * @param onErrorMethod   an {@link Optional} with a reference to the error callback method
   */
  public SourceCallbackModelProperty(Optional<Method> onSuccessMethod, Optional<Method> onErrorMethod) {
    this.onSuccessMethod = onSuccessMethod;
    this.onErrorMethod = onErrorMethod;
  }

  public Optional<Method> getOnSuccessMethod() {
    return onSuccessMethod;
  }

  public Optional<Method> getOnErrorMethod() {
    return onErrorMethod;
  }

  @Override
  public String getName() {
    return "sourceCallbackModelProperty";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
