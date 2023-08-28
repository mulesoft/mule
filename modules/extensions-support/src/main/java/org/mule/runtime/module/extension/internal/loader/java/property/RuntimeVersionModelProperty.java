/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.sdk.api.annotation.param.RuntimeVersion;

import java.lang.reflect.Field;

/**
 * {@link ModelProperty} used to indicate that the class implementing a {@link ConfigurationModel} has a {@link Field} on which
 * the {@link org.mule.runtime.api.meta.MuleVersion} should be injected.
 *
 * @see RuntimeVersion
 * @since 4.4
 */
public class RuntimeVersionModelProperty extends InjectedFieldModelProperty {

  /**
   * Creates a new instance
   *
   * @param field on which the value should be injected
   */
  public RuntimeVersionModelProperty(Field field) {
    super(field);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "runtimeVersion";
  }
}
