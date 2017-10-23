/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Function;

/**
 * A {@link ValueResolver} wrapper which defaults to obtaining the value from the current {@link ConfigurationInstance}
 * if the given {@link ValueResolver delegate} results in a {@code null} value.
 *
 * @param <T> the generic type of the produced values.
 * @since 4.0
 */
public final class ConfigOverrideValueResolverWrapper<T> implements ValueResolver<T>, Initialisable {

  private final ValueResolver<T> delegate;
  private final String parameterName;
  private final MuleContext muleContext;
  private Function<Object, Object> defaultValueResolver;

  /**
   * Creates a new instance
   *
   * @param delegate the {@link ValueResolver delegate} used to obtain a value in the first place.
   *                 Only if this {@code delegate} returns a {@code null} value will the resolution using
   *                 a {@link ConfigurationInstance config} will be attempted.
   * @param <T>      the generic type of the produced values.
   * @return a new instance of {@link ConfigOverrideValueResolverWrapper}
   */
  public static <T> ValueResolver<T> of(ValueResolver<T> delegate, String parameterName, MuleContext muleContext) {
    checkArgument(delegate != null, "A ValueResolver is required in order to delegate the value resolution.");
    checkArgument(!isBlank(parameterName), "A parameter name is required in order to use the config as a fallback.");
    return new ConfigOverrideValueResolverWrapper<>(delegate, parameterName, muleContext);
  }

  private ConfigOverrideValueResolverWrapper(ValueResolver<T> delegate, String parameterName,
                                             MuleContext muleContext) {
    this.muleContext = muleContext;
    checkArgument(delegate != null, "A ConfigOverride value resolver requires a non-null delegate");
    this.delegate = delegate;
    this.parameterName = parameterName;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    T value = delegate.resolve(context);
    if (value != null) {
      return value;
    }

    if (!context.getConfig().isPresent()) {
      throw new DefaultMuleException(createStaticMessage("Failed to obtain the config-provided value for parameter ["
          + parameterName + "]. No configuration was available in the current resolution context."));
    }

    return resolveConfigOverrideParameter(context.getConfig().get());
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
   * @param instance        the {@link ConfigurationInstance config} from where the parameter value will be obtained
   * @return the value of the parameter with name {@code parameterName} obtained from the {@code delegate} or
   * from {@link ConfigurationInstance#getValue() config instance} if the {@code delegate} produces a {@code null} value.
   */
  private T resolveConfigOverrideParameter(ConfigurationInstance instance) {
    try {
      return (T) getParameterValueResolverFromConfig(instance).apply(instance.getValue());
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to obtain the value for parameter [" + parameterName
          + "] from the associated configuration [" + instance.getName() + "]: "
          + e.getMessage(), e);
    }
  }

  private Function<Object, Object> getParameterValueResolverFromConfig(final ConfigurationInstance config) {
    if (defaultValueResolver != null) {
      return defaultValueResolver;
    }

    synchronized (this) {
      if (defaultValueResolver == null) {
        for (ParameterGroupModel group : config.getModel().getParameterGroupModels()) {
          Optional<String> fieldName = group.getParameterModels().stream()
              .filter(p -> p.getName().equals(parameterName))
              .findFirst()
              .map(p -> p.getModelProperty(DeclaringMemberModelProperty.class).get().getDeclaringField().getName());

          if (!fieldName.isPresent()) {
            continue;
          }

          if (group.getName().equals(DEFAULT_GROUP_NAME)) {
            defaultValueResolver = getParameterValueFromConfigField(config, fieldName.get());
          } else {
            defaultValueResolver = getParameterValueFromFieldInGroup(config, group, fieldName.get());
          }
          break;
        }
      }
    }

    if (defaultValueResolver == null) {
      throw new IllegalArgumentException("Missing parameter with name [" + parameterName
          + "] in config [" + config.getName() + "]");
    }

    return defaultValueResolver;
  }

  private Function<Object, Object> getParameterValueFromConfigField(ConfigurationInstance config, String fieldName) {
    Field parameterField = getField(config.getValue().getClass(), fieldName)
        .orElseThrow(() -> new IllegalArgumentException("Missing field with name [" + fieldName
            + "] in config [" + config.getName() + "]"));
    parameterField.setAccessible(true);

    return (target) -> {
      try {
        return parameterField.get(target);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Failed to read field with name [" + parameterField.getName()
            + " in config [" + config.getName() + "]: " + e.getMessage());
      }
    };
  }

  private Function<Object, Object> getParameterValueFromFieldInGroup(ConfigurationInstance config, ParameterGroupModel group,
                                                                     String fieldName) {
    ParameterGroupDescriptor descriptor = group.getModelProperty(ParameterGroupModelProperty.class)
        .map(ParameterGroupModelProperty::getDescriptor)
        .orElseThrow(() -> new IllegalArgumentException(
                                                        format("The group [%s] in config [%s] doesn't provide a group descriptor. "
                                                            + "Is not possible to retrieve the config parameter to override",
                                                               group.getName(), config.getName())));

    Field groupField = (Field) descriptor.getContainer();
    Field parameterField = getField(descriptor.getType().getDeclaringClass(), fieldName)
        .orElseThrow(() -> new IllegalArgumentException("Missing field with name [" + fieldName
            + "] in group [" + descriptor.getName() + "] of config ["
            + config.getName() + "]"));

    return new ParameterValueRetrieverFromConfigGroup(groupField, parameterField);
  }

  private static final class ParameterValueRetrieverFromConfigGroup implements Function {

    private final Field groupField;
    private final Field parameterField;

    public ParameterValueRetrieverFromConfigGroup(Field groupField, Field parameterField) {
      this.groupField = groupField;
      this.parameterField = parameterField;

      this.groupField.setAccessible(true);
      this.parameterField.setAccessible(true);
    }

    @Override
    public Object apply(Object configInstance) {
      Object groupInstance;
      try {
        groupInstance = groupField.get(configInstance);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Missing field with name [" + groupField.getName() + "] of config ["
            + groupField.getDeclaringClass().getName() + "]: " + e.getMessage());
      }

      try {
        return parameterField.get(groupInstance);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Missing field with name [" + parameterField.getName()
            + "] in group [" + parameterField.getDeclaringClass().getName() + "] of config ["
            + groupField.getDeclaringClass().getName() + "]: " + e.getMessage());
      }
    }
  }
}
