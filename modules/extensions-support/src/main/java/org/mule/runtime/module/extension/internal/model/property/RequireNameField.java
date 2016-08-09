/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.ModelProperty;

import java.lang.reflect.Field;

/**
 * A custom model property used to indicate that the class implementing a {@link ConfigurationModel} has a {@link Field} on which
 * the config's name should be injected.
 *
 * @see ConfigName
 * @since 4.0
 */
public final class RequireNameField implements ModelProperty {

  /**
   * The {@link Field} on which the name should be injected
   */
  private final Field configNameField;

  /**
   * Creates a new instance
   *
   * @param configNameField on which the name should be injected
   */
  public RequireNameField(Field configNameField) {
    this.configNameField = configNameField;
  }

  /**
   * @return on which the name should be injected
   */
  public Field getConfigNameField() {
    return configNameField;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code requireNameField}
   */
  @Override
  public String getName() {
    return "requireNameField";
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isExternalizable() {
    return false;
  }
}
