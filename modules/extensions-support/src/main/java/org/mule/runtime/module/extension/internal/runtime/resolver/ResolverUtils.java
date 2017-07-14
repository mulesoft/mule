/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;

import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;

/**
 * Utility class to share common behaviour between resolvers
 *
 * @since 4.0
 */
public class ResolverUtils {

  private ResolverUtils() {

  }

  static ValueResolver<?> getFieldDefaultValueValueResolver(ObjectFieldType field, MuleContext muleContext) {
    Optional<String> defaultValue = getDefaultValue(field);
    checkArgument(defaultValue.isPresent(), "No default value available for field :" + field.getKey().getName());
    return getExpressionBasedValueResolver(defaultValue.get(), field.getValue(), muleContext);
  }

  static ValueResolver<?> getFieldDefaultValueValueResolver(MetadataType fieldType, String defaultValue,
                                                            MuleContext muleContext) {
    return getExpressionBasedValueResolver(defaultValue, fieldType, muleContext);
  }

  public static ValueResolver<?> getExpressionBasedValueResolver(String expression, MetadataType metadataType,
                                                                 MuleContext muleContext) {
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

  /**
   * Gets a {@link ValueResolver} for the parameter if it has an associated a default value or encoding.
   *
   * @param hasDefaultEncoding whether the parameter has to use runtime's default encoding or not
   * @return {@link Supplier} for obtaining the the proper {@link ValueResolver} for the default value, {@code null} if there is
   *         no default.
   */
  static ValueResolver<?> getDefaultValueResolver(boolean hasDefaultEncoding, MuleContext muleContext,
                                                  Supplier<ValueResolver<?>> supplier) {
    return hasDefaultEncoding ? new StaticValueResolver<>(muleContext.getConfiguration().getDefaultEncoding()) : supplier.get();
  }

  private static ValueResolver<?> getExpressionBasedValueResolver(String expression, BooleanSupplier isTypedValue,
                                                                  BooleanSupplier isParameterResolver, MetadataType type,
                                                                  MuleContext muleContext) {

    try {
      if (isTypedValue.getAsBoolean()) {
        ExpressionTypedValueValueResolver<Object> valueResolver =
            new ExpressionTypedValueValueResolver<>(expression, getType(type));
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        return valueResolver;
      } else if (isParameterResolver.getAsBoolean()) {
        ExpressionBasedParameterResolverValueResolver<Object> valueResolver =
            new ExpressionBasedParameterResolverValueResolver<>(expression, type);
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        return valueResolver;
      } else if (muleContext.getExpressionManager().isExpression(expression)) {
        TypeSafeExpressionValueResolver<Object> valueResolver = new TypeSafeExpressionValueResolver<>(expression, type);
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        valueResolver.initialise();
        return valueResolver;
      } else {
        TypeSafeValueResolverWrapper typeSafeValueResolverWrapper =
            new TypeSafeValueResolverWrapper<>(new StaticValueResolver<>(expression), getType(type));
        typeSafeValueResolverWrapper.setTransformationService(muleContext.getTransformationService());
        typeSafeValueResolverWrapper.initialise();
        return typeSafeValueResolverWrapper;
      }
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
