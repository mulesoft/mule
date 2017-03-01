/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Utility class to share common behaviour between resolvers
 *
 * @since 4.0
 */
class ResolverUtils {

  private ResolverUtils() {

  }

  static ValueResolver<?> getFieldDefaultValueValueResolver(ObjectFieldType field, MuleContext muleContext) {
    MetadataType fieldType = field.getValue();
    String expression = getDefaultValue(field).get();
    return getExpressionBasedValueResolver(expression, fieldType, muleContext);
  }

  static ValueResolver<?> getExpressionBasedValueResolver(String expression, MetadataType metadataType, MuleContext muleContext) {
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType),
                                           () -> isParameterResolver(metadataType),
                                           metadataType,
                                           muleContext);
  }

  static ValueResolver<?> getExpressionBasedValueResolver(String expression, ParameterModel operationModel,
                                                          MuleContext muleContext) {
    MetadataType metadataType = operationModel.getType();
    Set<ModelProperty> modelProperties = operationModel.getModelProperties();
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType) || isTypedValue(modelProperties),
                                           () -> isParameterResolver(metadataType) || isParameterResolver(modelProperties),
                                           metadataType,
                                           muleContext);
  }

  private static ValueResolver<?> getExpressionBasedValueResolver(String expression, BooleanSupplier isTypedValue,
                                                                  BooleanSupplier isParameterResolver, MetadataType type,
                                                                  MuleContext muleContext) {
    if (isTypedValue.getAsBoolean()) {
      return new ExpressionTypedValueValueResolver<>(expression, getType(type), muleContext);
    } else if (isParameterResolver.getAsBoolean()) {
      return new ExpressionBasedParameterResolverValueResolver<>(expression, type, muleContext);
    } else if (muleContext.getExpressionManager().isExpression(expression)) {
      return new TypeSafeExpressionValueResolver<>(expression, getType(type), muleContext);
    } else {
      return new TypeSafeValueResolverWrapper(new StaticValueResolver<>(expression), getType(type), muleContext);
    }
  }
}
