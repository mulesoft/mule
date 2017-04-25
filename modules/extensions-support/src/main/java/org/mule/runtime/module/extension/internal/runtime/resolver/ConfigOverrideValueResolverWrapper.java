/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link ValueResolver} wrapper which defaults to obtaining the value from the current {@link ConfigurationInstance}
 * if the given {@link ValueResolver delegate} results in a {@code null} value.
 *
 * @param <T> the generic type of the produced values.
 * @since 4.0
 */
public class ConfigOverrideValueResolverWrapper<T> implements ValueResolver<T>, Initialisable {

  private final ValueResolver<T> delegate;
  private final String parameterName;
  private final MuleContext muleContext;
  private final Function<Event, Optional<ConfigurationInstance>> configProvider;
  private Field field;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ValueResolver delegate} used to obtain a value in the first place.
   *                 Only if this {@code delegate} returns a {@code null} value will the resolution using
   *                 a {@link ConfigurationInstance config} will be attempted.
   * @param <T>      the generic type of the produced values.
   * @return a new instance of {@link ConfigOverrideValueResolverWrapper}
   */
  public static <T> ValueResolver<T> of(ValueResolver<T> delegate, String parameterName, MuleContext muleContext,
                                        Function<Event, Optional<ConfigurationInstance>> configProvider) {
    checkArgument(delegate != null,
                  "A ValueResolver is required in order to delegate the value resolution.");
    return new ConfigOverrideValueResolverWrapper(delegate, parameterName, muleContext, configProvider);
  }

  private ConfigOverrideValueResolverWrapper(ValueResolver<T> delegate, String parameterName,
                                             MuleContext muleContext,
                                             Function<Event, Optional<ConfigurationInstance>> configProvider) {
    this.muleContext = muleContext;
    this.configProvider = configProvider;
    checkArgument(delegate != null, "A ConfigOverride value resolver requires a non-null delegate");
    this.delegate = delegate;
    this.parameterName = parameterName;
  }

  @Override
  public T resolve(Event event) throws MuleException {

    final Supplier supplier = () -> {
      try {
        return delegate.resolve(event);
      } catch (MuleException e) {
        throw new MuleRuntimeException(createStaticMessage("An error occurred while resolving the value for parameter "
            + parameterName), e);
      }
    };
    return (T) resolveConfigOverrideParameter(supplier, configProvider.apply(event), parameterName);
  }

  @Override
  public boolean isDynamic() {
    return delegate.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      muleContext.getInjector().inject(delegate);
      initialiseIfNeeded(delegate, muleContext);
    } catch (MuleException e) {
      throw new InitialisationException(
                                        createStaticMessage("Failed to initialise the delegate ValueResolver for ConfigOverride wrapper"),
                                        e, this);
    }
  }

  /**
   * Retrieves the value of the parameter of name {@code parameterName} obtained from the {@code delegate} or
   * from {@link ConfigurationInstance#getValue() config instance} if the {@code delegate} produces a
   * {@code null} value.
   *
   * @param config        the {@link ConfigurationInstance config} from where the parameter value will be obtained
   * @param parameterName the name parameter to resolve
   * @return the value of the parameter with name {@code parameterName} obtained from the {@code delegate} or
   * from {@link ConfigurationInstance#getValue() config instance} if the {@code delegate} produces a {@code null} value.
   */
  private Object resolveConfigOverrideParameter(Supplier delegate, Optional<ConfigurationInstance> config,
                                                String parameterName) {
    Object value = delegate.get();
    if (value == null) {
      checkState(config.isPresent(),
                 "Failed to obtain the config-provided value for parameter [" + parameterName + "]."
                     + " No configuration available in the current execution context.");

      ConfigurationInstance instance = config.get();
      try {
        value = getConfigField(instance).get(instance.getValue());
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to obtain the value for parameter [" + parameterName
            + "] from the associated configuration [" + instance.getName() + "]: "
            + e.getMessage(), e);
      }
    }
    return value;
  }

  private Field getConfigField(ConfigurationInstance config) throws NoSuchFieldException {
    if (field == null) {
      synchronized (this) {
        if (field == null) {
          final String configFieldName = config.getModel().getAllParameterModels().stream()
              .filter(p -> p.getName().equals(parameterName)
                  && p.getModelProperty(DeclaringMemberModelProperty.class).isPresent())
              .findFirst()
              .map(p -> p.getModelProperty(DeclaringMemberModelProperty.class).get().getDeclaringField().getName())
              .orElseThrow(() -> new IllegalArgumentException("Failed to obtain the declaring field for parameter ["
                  + parameterName
                  + "] from the associated configuration [" + config.getName()
                  + "]"));

          final Optional<Field> fieldOptional = getField(config.getValue().getClass(), configFieldName);
          if (fieldOptional.isPresent()) {
            field = fieldOptional.get();
            field.setAccessible(true);
          } else {
            throw new NoSuchFieldException("Missing field with name [" + configFieldName
                + "] in class ["
                + config.getValue().getClass().getName() + "]");
          }
        }
      }
    }

    return field;
  }
}
