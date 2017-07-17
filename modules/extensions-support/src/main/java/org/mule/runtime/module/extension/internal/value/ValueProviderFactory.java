/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides instances of the {@link ValueProvider}
 *
 * @since 4.0
 */
public class ValueProviderFactory {

  private final ValueProviderFactoryModelProperty factoryModelProperty;
  private final ParameterValueResolver parameterValueResolver;
  private final Supplier<Object> connectionSupplier;
  private final Supplier<Object> configurationSupplier;
  private final Field connectionField;
  private final Field configField;
  private final MuleContext muleContext;

  public ValueProviderFactory(ValueProviderFactoryModelProperty factoryModelProperty,
                              ParameterValueResolver parameterValueResolver, Supplier<Object> connectionSupplier,
                              Supplier<Object> configurationSupplier, Field connectionField, Field configField,
                              MuleContext muleContext) {
    this.factoryModelProperty = factoryModelProperty;
    this.parameterValueResolver = parameterValueResolver;
    this.connectionSupplier = connectionSupplier;
    this.configurationSupplier = configurationSupplier;
    this.connectionField = connectionField;
    this.configField = configField;
    this.muleContext = muleContext;
  }

  ValueProvider createValueProvider() throws ValueResolvingException {
    Class<? extends ValueProvider> resolverClass = factoryModelProperty.getValueProvider();

    try {
      ValueProvider resolver = instantiateClass(resolverClass);
      initialiseIfNeeded(resolver, true, muleContext);

      for (String requiredParam : factoryModelProperty.getRequiredParameters()) {
        Object parameterValue = parameterValueResolver.getParameterValue(requiredParam);
        injectValueIntoField(resolver, parameterValue, requiredParam);
      }

      if (factoryModelProperty.usesConnection()) {
        injectValueIntoField(resolver, connectionSupplier.get(), connectionField);
      }

      if (factoryModelProperty.usesConfig()) {
        injectValueIntoField(resolver, configurationSupplier.get(), configField);
      }
      return resolver;
    } catch (Exception e) {
      throw new ValueResolvingException("An error occurred trying to create a ValueProvider", UNKNOWN, e);
    }
  }

  private static void injectValueIntoField(ValueProvider fieldContainer, Object valueToInject, String requiredParamName) {
    Optional<Field> optionalField = IntrospectionUtils.getField(fieldContainer.getClass(), requiredParamName);
    if (optionalField.isPresent()) {
      Field field = optionalField.get();
      injectValueIntoField(fieldContainer, valueToInject, field);
    }
  }

  private static void injectValueIntoField(ValueProvider fieldContainer, Object valueToInject, Field field) {
    field.setAccessible(true);
    org.springframework.util.ReflectionUtils.setField(field, fieldContainer, valueToInject);
  }
}
