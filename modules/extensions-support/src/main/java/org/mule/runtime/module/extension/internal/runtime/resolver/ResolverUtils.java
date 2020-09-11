/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static java.util.function.UnaryOperator.identity;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.management.stats.StatisticsUtils.visitable;
import static org.mule.runtime.core.internal.management.stats.visitor.InputDecoratorVisitor.builder;
import static org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty.getStackedTypesModelProperty;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.internal.management.stats.visitor.InputDecoratorVisitor;
import org.mule.runtime.core.internal.management.stats.visitor.OutputDecoratorVisitor;
import org.mule.runtime.core.internal.management.stats.visitor.Visitor;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;

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

  static ValueResolver<?> getDefaultValueResolver(ParameterModel parameter, MuleContext muleContext) {
    Object defaultValue = parameter.getDefaultValue();
    if (defaultValue instanceof String) {
      return getExpressionBasedValueResolver((String) defaultValue, parameter, muleContext);
    } else if (defaultValue != null) {
      return new StaticValueResolver<>(defaultValue);
    }
    return null;
  }

  public static <T> T resolveRecursively(ValueResolver<T> valueResolver, ValueResolvingContext resolvingContext)
      throws MuleException {
    T resolve = valueResolver.resolve(resolvingContext);
    if (resolve instanceof ValueResolver) {
      resolve = resolveRecursively((ValueResolver<T>) resolve, resolvingContext);
    }
    return resolve;
  }

  /**
   * Executes the {@code resolver} using the given {@code context},
   * applying all the required resolution rules that may apply for
   * the given {@code T} type.
   *
   * @param resolver the {@link ValueResolver} to execute
   * @param context  the {@link ValueResolvingContext} to pass on the {@code resolver}
   * @return the resolved value
   * @throws MuleException
   */
  public static <T> T resolveValue(ValueResolver<T> resolver, ValueResolvingContext context)
      throws MuleException {
    T value = resolveRecursively(resolver, context);
    if (context == null || context.resolveCursors()) {
      return (T) resolveCursor(value);
    } else {
      return value;
    }
  }

  /**
   * Obtains a {@link Cursor} based on the {@code value}, if one is available.
   *
   * @return the given {@code value} but converting a {@link CursorProvider} to a {@link Cursor} if any is present.
   */
  public static Object resolveCursor(Object value) {
    return resolveCursor(value, identity());
  }

  /**
   * Obtains a {@link Cursor} based on the {@code value}, if one is available.
   *
   * @return the given {@code value} but converting a {@link CursorProvider} to a {@link Cursor} if any is present.
   */
  public static Object resolveCursor(Object value, UnaryOperator valueMapper) {
    if (value instanceof CursorProvider) {
      return valueMapper.apply(((CursorProvider) value).openCursor());

    }

    return resolveTypedValue(value, valueMapper);
  }

  /**
   * Obtains the value of a {@link TypedValue} if appropriate.
   *
   * @return the given {@code value} from a typedValue.
   */
  public static Object resolveTypedValue(Object value, UnaryOperator valueMapper) {
    if (value instanceof TypedValue) {
      return resolveCursor((TypedValue) value, valueMapper);
    }

    return valueMapper.apply(value);
  }

  /**
   * Applies the valueMapper to the value of a {@link TypedValue} if appropriate
   *
   * @return the given {@code value} from a typedValue.
   */
  public static Object mapTypeValue(Object value, UnaryOperator valueMapper) {
    if (value instanceof TypedValue) {
      return typedValue((TypedValue<?>) value, valueMapper, ((TypedValue<?>) value).getValue());
    }

    return valueMapper.apply(value);
  }

  public static Object resolveCursor(TypedValue<?> typedValue) {
    return resolveCursor(typedValue, identity());
  }

  public static Object resolveCursor(TypedValue<?> typedValue, UnaryOperator valueMapper) {
    Object objectValue = typedValue.getValue();

    if (objectValue instanceof CursorProvider) {
      Cursor cursor = ((CursorProvider) objectValue).openCursor();
      return typedValue(typedValue, valueMapper, cursor);
    } else {
      final Object mappedValue = valueMapper.apply(objectValue);

      if (mappedValue == objectValue) {
        return typedValue;
      } else {
        return new TypedValue<>(mappedValue, typedValue.getDataType(), typedValue.getByteLength());
      }
    }
  }

  private static Object typedValue(TypedValue<?> typedValue, UnaryOperator valueMapper, Object value) {
    return new TypedValue<>(valueMapper.apply(value), DataType.builder()
        .type(value.getClass())
        .mediaType(typedValue.getDataType().getMediaType())
        .build(), typedValue.getByteLength());
  }

  private static ValueResolver<?> getExpressionBasedValueResolver(String expression, BooleanSupplier isTypedValue,
                                                                  BooleanSupplier isParameterResolver,
                                                                  Optional<StackedTypesModelProperty> stackedTypesModelProperty,
                                                                  MetadataType type,
                                                                  MuleContext muleContext) {

    try {
      ValueResolver resolver;
      if (stackedTypesModelProperty.isPresent()) {
        resolver = stackedTypesModelProperty.get().getValueResolverFactory().getExpressionBasedValueResolver(expression,
                                                                                                             getType(type));
        // TODO MULE-13518: Add support for stacked value resolvers for @Parameter inside pojos
        // The following "IFs" should be removed once implemented
      } else if (isTypedValue.getAsBoolean()) {
        ExpressionTypedValueValueResolver<Object> valueResolver =
            new ExpressionTypedValueValueResolver<>(expression, getType(type));
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        resolver = valueResolver;
      } else if (isParameterResolver.getAsBoolean()) {
        ExpressionBasedParameterResolverValueResolver<Object> valueResolver =
            new ExpressionBasedParameterResolverValueResolver<>(expression, getType(type), toDataType(type));
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        resolver = valueResolver;
      } else if (muleContext.getExpressionManager().isExpression(expression)) {
        TypeSafeExpressionValueResolver<Object> valueResolver =
            new TypeSafeExpressionValueResolver<>(expression, getType(type), toDataType(type));
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        resolver = valueResolver;
      } else {
        TypeSafeValueResolverWrapper typeSafeValueResolverWrapper =
            new TypeSafeValueResolverWrapper<>(new StaticValueResolver<>(expression), getType(type));
        typeSafeValueResolverWrapper.setTransformationService(muleContext.getTransformationService());
        resolver = typeSafeValueResolverWrapper;
      }

      initialiseIfNeeded(resolver, muleContext);
      return resolver;
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
