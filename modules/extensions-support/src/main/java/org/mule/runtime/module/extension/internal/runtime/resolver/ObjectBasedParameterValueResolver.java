/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFields;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ParameterValueResolver} implementation for Object based components, like {@link Source sources}, configurations
 * and {@link ConnectionProvider connection providers}
 *
 * @since 4.0
 */
public class ObjectBasedParameterValueResolver implements ParameterValueResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectBasedParameterValueResolver.class);

  private final Object object;
  private final ParameterizedModel parameterizedModel;
  private final ReflectionCache reflectionCache;

  public ObjectBasedParameterValueResolver(Object object, ParameterizedModel parameterizedModel,
                                           ReflectionCache reflectionCache) {
    this.object = object;
    this.parameterizedModel = parameterizedModel;
    this.reflectionCache = reflectionCache;
  }

  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    try {
      Optional<Field> field = getField(object.getClass(), parameterName, reflectionCache);
      if (field.isPresent()) {
        return getFieldValue(object, parameterName, reflectionCache);
      } else {
        for (ParameterGroupModel parameterGroupModel : parameterizedModel.getParameterGroupModels()) {
          Optional<ParameterGroupModelProperty> modelProperty =
              parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class);

          if (modelProperty.isPresent()) {
            ParameterGroupModelProperty property = modelProperty.get();
            Field container = (Field) property.getDescriptor().getContainer();

            Object parameterGroup = getFieldValue(object, container.getName(), reflectionCache);
            Optional<Field> desiredField = getField(parameterGroup.getClass(), parameterName, reflectionCache);
            if (desiredField.isPresent()) {
              return getFieldValue(parameterGroup, parameterName, reflectionCache);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ValueResolvingException("An error occurred trying to obtain the value for the parameter: " + parameterName);
    }
    throw new ValueResolvingException("Unable to resolve value for the parameter: " + parameterName);
  }

  @Override
  public Map<String, ValueResolver<? extends Object>> getParameters() throws ValueResolvingException{
    HashMap<String, ValueResolver<? extends Object>> parameters = new HashMap<>();
    try {
      addFields(getFields(object.getClass()), parameters);

      for (ParameterGroupModel parameterGroupModel : parameterizedModel.getParameterGroupModels()) {
        Optional<ParameterGroupModelProperty> modelProperty =
            parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class);
        if (modelProperty.isPresent()) {
          ParameterGroupModelProperty property = modelProperty.get();
          Field container = (Field) property.getDescriptor().getContainer();
          Object parameterGroup = getFieldValue(object, container.getName(), reflectionCache);
          addFields(getFields(parameterGroup.getClass()), parameters);
        }
      }
    } catch (Exception e) {
      throw new ValueResolvingException("An error occurred trying to obtain the parameters.");
    }
    return unmodifiableMap(parameters);
  }

  private void addFields(List<Field> fields, HashMap<String, ValueResolver<? extends Object>> parameters)
      throws NoSuchFieldException, IllegalAccessException {
    for (Field field : fields) {
      String parameterName = field.getName();
      parameters.put(parameterName, new StaticValueResolver<>(getFieldValue(object, parameterName, reflectionCache)));
    }
  }
}
