/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
