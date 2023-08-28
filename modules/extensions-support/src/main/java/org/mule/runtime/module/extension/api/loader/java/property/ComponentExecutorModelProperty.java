/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.property;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;

/**
 * A {@link ModelProperty} meant to be used on {@link ComponentModel component models}, which provides access to a
 * {@link ComponentExecutorFactory} used to execute such component
 *
 * @since 4.0
 * @deprecated since 4.3. Use {@link CompletableComponentExecutorModelProperty} instead.
 */
@Deprecated
public final class ComponentExecutorModelProperty implements ModelProperty {

  private final ComponentExecutorFactory executorFactory;

  /**
   * Creates a new instance
   *
   * @param executorFactory a {@link ComponentExecutorFactory}
   */
  public ComponentExecutorModelProperty(ComponentExecutorFactory executorFactory) {
    this.executorFactory = executorFactory;
  }

  /**
   * @return a {@link ComponentExecutorFactory}
   */
  public ComponentExecutorFactory getExecutorFactory() {
    return executorFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return null;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
