/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.sdk.api.values.FieldValues;

import java.util.Map;

/**
 * An immutable model property which communicates the {@link ValueProviderFactoryModelProperty} associated to each
 * {@link FieldValues}'s target path of a {@link ParameterModel}'s {@link FieldValues}.
 *
 * @since 4.4
 */
public final class FieldsValueProviderFactoryModelProperty implements ModelProperty {

  private final Map<String, ValueProviderFactoryModelProperty> fieldsValueProviderFactories;

  /**
   * Creates a new instance
   *
   * @param fieldsValueProviderFactories Map of the acting parameter target path and its corresponding
   *                                     {@link ValueProviderFactoryModelProperty}
   * @throws NullPointerException if {@code fieldsValueProviderFactories} is {@code null}
   */
  public FieldsValueProviderFactoryModelProperty(Map<String, ValueProviderFactoryModelProperty> fieldsValueProviderFactories) {
    requireNonNull(fieldsValueProviderFactories, "Map of value provider factories cannot be null");
    this.fieldsValueProviderFactories = fieldsValueProviderFactories;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "FieldsValueProviderFactory";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  public Map<String, ValueProviderFactoryModelProperty> getFieldsValueProviderFactories() {
    return fieldsValueProviderFactories;
  }
}
