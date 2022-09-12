/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isNonBlocking;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConfigOverrideValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link CompletableComponentExecutorFactory} which produces instances of
 * {@link CompletableMethodOperationExecutor}.
 *
 * @param <T> the type of the class in which the implementing method is declared
 * @since 3.7
 */
public final class CompletableOperationExecutorFactory<T, M extends ComponentModel> implements
    CompletableComponentExecutorFactory<M> {

  private final Class<T> implementationClass;
  private final Method operationMethod;

  public CompletableOperationExecutorFactory(Class<T> implementationClass, Method operationMethod) {
    checkArgument(implementationClass != null, "implementationClass cannot be null");
    checkArgument(operationMethod != null, "operationMethod cannot be null");

    this.implementationClass = implementationClass;
    this.operationMethod = operationMethod;
  }

  @Override
  public CompletableComponentExecutor<M> createExecutor(M operationModel, Map<String, Object> parameters) {
    DefaultObjectBuilder objectBuilder = new DefaultObjectBuilder(implementationClass, new ReflectionCache());
    parameters.forEach((k, v) -> objectBuilder.addPropertyResolver(k, new StaticValueResolver<>(v)));
    Object delegate = withNullEvent(event -> {
      try {
        return objectBuilder.build(ValueResolvingContext.builder(event).build());
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not create instance of operation class "
            + implementationClass.getName()), e);
      }
    });

    if (isNonBlocking(operationModel)) {
      return new NonBlockingCompletableMethodOperationExecutor<>(operationModel, operationMethod, delegate);
    }

    return new CompletableMethodOperationExecutor<>(operationModel, operationMethod, delegate);
  }

  /**
   * Receives a {@link Map} with the execution parameters of an operation and returns a new one with the ones that should be used
   * to initialize it.
   * <p>
   * This is <b>ONLY</b> valid for extensions written with the Java SDK.
   *
   * @param extensionModel      the {@link ExtensionModel}
   * @param componentModel      the {@link ComponentModel}
   * @param operationParameters the operation's parameters in form of a map
   * @param actingComponent     the {@link Component} executing the operation
   * @param staticConfig        the static {@link ConfigurationInstance} if one exists. Use {@link Optional#empty()} if no config
   *                            or dynamic
   * @param extensionManager    the {@link ExtensionManager}
   * @param expressionManager   the {@link ExtensionManager}
   * @param reflectionCache     a {@link ReflectionCache}
   * @param <C>                 the component's generic type
   * @return a {@link Map} with the initialization parameters
   * @throws InitialisationException
   * @since 4.5.0
   */
  public static <C extends Component & Initialisable> Map<String, Object> extractExecutorInitialisationParams(
                                                                                                              ExtensionModel extensionModel,
                                                                                                              ComponentModel componentModel,
                                                                                                              Map<String, ? extends Object> operationParameters,
                                                                                                              C actingComponent,
                                                                                                              Optional<ConfigurationInstance> staticConfig,
                                                                                                              ExtensionManager extensionManager,
                                                                                                              ExpressionManager expressionManager,
                                                                                                              ReflectionCache reflectionCache)
      throws InitialisationException {

    Map<String, Object> initParams = new HashMap<>();

    LazyValue<ValueResolvingContext> resolvingContext =
        new LazyValue<>(() -> withNullEvent(event -> ValueResolvingContext.builder(event, expressionManager)
            .withConfig(staticConfig)
            .build()));

    LazyValue<Boolean> dynamicConfig = new LazyValue<>(
                                                       () -> extensionManager
                                                           .getConfigurationProvider(extensionModel, componentModel,
                                                                                     resolvingContext.get().getEvent())
                                                           .map(ConfigurationProvider::isDynamic)
                                                           .orElse(false));

    try {
      for (ParameterGroupModel group : componentModel.getParameterGroupModels()) {
        if (group.getName().equals(DEFAULT_GROUP_NAME)) {
          for (ParameterModel p : group.getParameterModels()) {
            if (!p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent()) {
              continue;
            }

            Object value = operationParameters.get(p.getName());
            if (value != null) {
              initParams.put(getMemberName(p), resolveComponentExecutorParam(
                                                                             resolvingContext,
                                                                             dynamicConfig,
                                                                             p,
                                                                             actingComponent,
                                                                             value));
            }
          }
        } else {
          ParameterGroupDescriptor groupDescriptor = group.getModelProperty(ParameterGroupModelProperty.class)
              .map(g -> g.getDescriptor())
              .orElse(null);

          if (groupDescriptor == null) {
            continue;
          }

          List<ParameterModel> fieldParameters = getGroupsOfFieldParameters(group);

          if (fieldParameters.isEmpty()) {
            continue;
          }

          ObjectBuilder groupBuilder = createFieldParameterGroupBuilder(
                                                                        groupDescriptor,
                                                                        operationParameters,
                                                                        fieldParameters, reflectionCache);

          try {
            initParams.put(((Field) groupDescriptor.getContainer()).getName(), groupBuilder.build(resolvingContext.get()));
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        }
      }

      return initParams;
    } finally {
      resolvingContext.ifComputed(ValueResolvingContext::close);
    }
  }

  private static <C extends Component & Initialisable> Object resolveComponentExecutorParam(LazyValue<ValueResolvingContext> resolvingContext,
                                                                                            Supplier<Boolean> dynamicConfig,
                                                                                            ParameterModel p,
                                                                                            C actingComponent,
                                                                                            Object value)
      throws InitialisationException {
    Object resolvedValue = null;
    try {
      if (value instanceof ConfigOverrideValueResolverWrapper) {
        resolvedValue = ((ConfigOverrideValueResolverWrapper<?>) value).resolveWithoutConfig(resolvingContext.get());
        if (resolvedValue == null) {
          if (dynamicConfig.get()) {
            final ComponentLocation location = actingComponent.getLocation();
            String message = format(
                                    "Component '%s' at %s uses a dynamic configuration and defines configuration override parameter '%s' which "
                                        + "is assigned on initialization. That combination is not supported. Please use a non dynamic configuration "
                                        + "or don't set the parameter.",
                                    location != null ? location.getComponentIdentifier().getIdentifier().toString()
                                        : actingComponent,
                                    actingComponent,
                                    p.getName());
            throw new InitialisationException(createStaticMessage(message), actingComponent);
          }
        }
      }

      if (resolvedValue == null) {
        if (value instanceof ValueResolver) {
          resolvedValue = resolveValue((ValueResolver<? extends Object>) value, resolvingContext.get());
        } else {
          resolvedValue = value;
        }
      }

      return resolvedValue;
    } catch (InitialisationException e) {
      throw e;
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static List<ParameterModel> getGroupsOfFieldParameters(ParameterGroupModel group) {
    return group.getParameterModels().stream()
        .filter(p -> p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent())
        .collect(toList());
  }

  private static ObjectBuilder createFieldParameterGroupBuilder(ParameterGroupDescriptor groupDescriptor,
                                                                Map<String, ? extends Object> parameters,
                                                                List<ParameterModel> fieldParameters,
                                                                ReflectionCache reflectionCache) {
    DefaultObjectBuilder groupBuilder =
        new DefaultObjectBuilder(groupDescriptor.getType().getDeclaringClass().get(), reflectionCache);

    fieldParameters.forEach(p -> {
      if (parameters.containsKey(p.getName())) {
        ValueResolver resolver = asValueResolver(parameters.get(p.getName()));
        Optional<Field> memberField = getMemberField(p);
        if (memberField.isPresent()) {
          groupBuilder.addPropertyResolver(getMemberField(p).get(), resolver);
        } else {
          groupBuilder.addPropertyResolver(p.getName(), resolver);
        }
      }
    });
    return groupBuilder;
  }

  private static ValueResolver asValueResolver(Object value) {
    return value instanceof ValueResolver
        ? (ValueResolver) value
        : new StaticValueResolver<>(value);
  }
}

