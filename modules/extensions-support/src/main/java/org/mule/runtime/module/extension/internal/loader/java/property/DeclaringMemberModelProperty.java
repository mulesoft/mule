/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * A custom model property to link a {@link ParameterModel} to the actual member it represents.
 * <p>
 * The most common use case for this is to support the {@link Alias} annotation. For example, consider a
 * {@link ConfigurationModel} parameter which is obtained through inspecting fields in a class. This property allows for the
 * introspection model to list the parameter by a given alias, while this parameter still provides the real name of the field
 * which is going to be needed for further operations
 * <p>
 * Another common use case is to get the field {@link Annotation}s in order to enrich the model with other properties.
 *
 * @since 4.0
 */
public final class DeclaringMemberModelProperty implements ModelProperty {

  private final Field declaringField;

  public DeclaringMemberModelProperty(Field declaringField) {
    this.declaringField = declaringField;
  }

  /**
   * The field associated to the {@link ParameterModel}
   *
   * @return a {@link Field}
   */
  public Field getDeclaringField() {
    return declaringField;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code declaringMember}
   */
  @Override
  public String getName() {
    return "declaringMember";
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
