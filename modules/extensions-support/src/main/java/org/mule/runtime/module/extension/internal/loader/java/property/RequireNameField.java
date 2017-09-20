/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.extension.api.annotation.param.RefName;

import java.lang.reflect.Field;

/**
 * A custom model property used to indicate that the class implementing a {@link ConfigurationModel} has a {@link Field} on which
 * the config's name should be injected.
 *
 * @see RefName
 * @since 4.0
 */
public final class RequireNameField extends InjectedFieldModelProperty {

  /**
   * Creates a new instance
   *
   * @param field on which the value should be injected
   */
  public RequireNameField(Field field) {
    super(field);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "requireNameField";
  }
}
