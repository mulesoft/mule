/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.lang.reflect.Field;

/**
 * A {@link ModelProperty} intended to be used on {@link ParameterModel parameters} to signal that if a value for the parameter
 * was not provided, then the runtime should use its own default encoding.
 *
 * @since 4.0
 */
public class DefaultEncodingModelProperty extends InjectedFieldModelProperty {

  /**
   * Creates a new instance
   *
   * @param field on which the value should be injected
   */
  public DefaultEncodingModelProperty(Field field) {
    super(field);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "defaultEncoding";
  }
}
