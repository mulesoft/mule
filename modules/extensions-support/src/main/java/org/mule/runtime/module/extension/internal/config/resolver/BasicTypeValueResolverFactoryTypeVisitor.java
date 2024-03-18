/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.resolver;

import static java.lang.String.format;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * A {@link MetadataTypeVisitor} implementation that creates a {@link ValueResolver} instances depending on a parameter
 * {@link MetadataType} for basic types.
 *
 * @since 4.4, 4.3.1
 */
public class BasicTypeValueResolverFactoryTypeVisitor extends BasicTypeMetadataVisitor {

  private static final ConversionService CONVERSION_SERVICE = new DefaultConversionService();

  private final Reference<ValueResolver> resolverValueHolder = new Reference<>();
  private final String parameterName;
  private final Object value;
  private final Class<?> expectedClass;


  public BasicTypeValueResolverFactoryTypeVisitor(String parameterName, Object value, Class<?> expectedClass) {
    this.parameterName = parameterName;
    this.value = value;
    this.expectedClass = expectedClass;
  }

  public ValueResolver getResolver() {
    return resolverValueHolder.get();
  }

  protected void setResolver(ValueResolver resolver) {
    resolverValueHolder.set(resolver);
  }

  protected Object getValue() {
    return value;
  }

  protected Class<?> getExpectedClass() {
    return expectedClass;
  }

  @Override
  protected void visitBasicType(MetadataType metadataType) {
    resolverValueHolder.set(basicTypeResolver());
  }

  public ValueResolver basicTypeResolver() {
    if (CONVERSION_SERVICE.canConvert(value.getClass(), expectedClass)) {
      return new StaticValueResolver<>(convertSimpleValue(value, expectedClass, parameterName));
    } else {
      return defaultResolver();
    }
  }

  @Override
  protected void defaultVisit(MetadataType metadataType) {
    resolverValueHolder.set(defaultResolver());
  }

  private ValueResolver<?> defaultResolver() {
    return new TypeSafeValueResolverWrapper<>(new StaticValueResolver<>(value), expectedClass);
  }

  private Object convertSimpleValue(Object value, Class<?> expectedClass, String parameterName) {
    try {
      return CONVERSION_SERVICE.convert(value, expectedClass);
    } catch (Exception e) {
      throw new IllegalArgumentException(format("Could not transform simple value '%s' to type '%s' in parameter '%s'", value,
                                                expectedClass.getSimpleName(), parameterName));
    }
  }

}
