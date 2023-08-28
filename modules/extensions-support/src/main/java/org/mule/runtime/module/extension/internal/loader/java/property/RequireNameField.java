/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
