/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldValue;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * {@link ParameterValueResolver} implementation for Object based components, like {@link Source sources}, configurations
 * and {@link ConnectionProvider connection providers}
 *
 * @since 4.0
 */
public class ObjectBasedParameterValueResolver implements ParameterValueResolver {

  private final Object object;
  private final ParameterizedModel parameterizedModel;

  public ObjectBasedParameterValueResolver(Object object, ParameterizedModel parameterizedModel) {
    this.object = object;
    this.parameterizedModel = parameterizedModel;
  }

  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    try {
      Optional<Field> field = getField(object.getClass(), parameterName);
      if (field.isPresent()) {
        return getFieldValue(object, parameterName);
      } else {
        for (ParameterGroupModel parameterGroupModel : parameterizedModel.getParameterGroupModels()) {
          Optional<ParameterGroupModelProperty> modelProperty =
              parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class);

          if (modelProperty.isPresent()) {
            ParameterGroupModelProperty property = modelProperty.get();
            Field container = (Field) property.getDescriptor().getContainer();

            Object parameterGroup = getFieldValue(object, container.getName());
            Optional<Field> desiredField = getField(parameterGroup.getClass(), parameterName);
            if (desiredField.isPresent()) {
              return getFieldValue(parameterGroup, parameterName);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ValueResolvingException("An error occurred trying to obtain the value for the parameter: " + parameterName);
    }
    throw new ValueResolvingException("Unable to resolve value for the parameter: " + parameterName);
  }
}
