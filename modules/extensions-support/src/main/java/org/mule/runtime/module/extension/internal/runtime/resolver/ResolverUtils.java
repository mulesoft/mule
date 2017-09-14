/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty.getStackedTypesModelProperty;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
                                           empty(),
                                           metadataType,
                                           muleContext);
  }

  static ValueResolver<?> getExpressionBasedValueResolver(String expression, ParameterModel operationModel,
                                                          MuleContext muleContext) {
    MetadataType metadataType = operationModel.getType();
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType),
                                           () -> isParameterResolver(metadataType),
                                           getStackedTypesModelProperty(operationModel.getModelProperties()),
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
    // TODO MULE-13066
    return hasDefaultEncoding ? new StaticValueResolver<>(muleContext.getConfiguration().getDefaultEncoding()) : supplier.get();
  }

  public static <T> T resolveRecursively(ValueResolver<T> valueResolver, ValueResolvingContext resolvingContext)
      throws MuleException {
    T resolve = valueResolver.resolve(resolvingContext);
    if (resolve instanceof ValueResolver) {
      resolve = resolveRecursively((ValueResolver<T>) resolve, resolvingContext);
    }
    return resolve;
  }

  private static ValueResolver<?> getExpressionBasedValueResolver(String expression, BooleanSupplier isTypedValue,
                                                                  BooleanSupplier isParameterResolver,
                                                                  Optional<StackedTypesModelProperty> stackedTypesModelProperty,
                                                                  MetadataType type,
                                                                  MuleContext muleContext) {

    try {
      if (stackedTypesModelProperty.isPresent()) {
        return stackedTypesModelProperty.get().getValueResolverFactory().getExpressionBasedValueResolver(expression,
                                                                                                         getType(type));
        //TODO MULE-13518: Add support for stacked value resolvers for @Parameter inside pojos // The following "IFs" should be removed once implemented
      } else if (isTypedValue.getAsBoolean()) {
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
