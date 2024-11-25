/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingException;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ParameterGroupObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationParameterValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Optional;

/**
 * {@link ParameterValueResolver} implementation for design-time services.
 *
 * @implNote This is the same as {@link ResolverSetBasedParameterResolver} but also resolves objects if a parameter group name is
 *           given, and it is not shown in DSL, similarly to {@link OperationParameterValueResolver}.
 * @since 4.8
 */
public final class DesignTimeParameterValueResolver extends ResolverSetBasedParameterResolver {

  private final ParameterizedModel model;
  private final ParameterGroupObjectBuilderAdapter paramGroupObjectBuilderAdapter;

  public DesignTimeParameterValueResolver(ResolverSet resolverSet,
                                          ParameterizedModel parameterizedModel,
                                          ReflectionCache reflectionCache, ExpressionManager expressionManager) {
    super(resolverSet, parameterizedModel, reflectionCache, expressionManager);
    this.model = parameterizedModel;
    this.paramGroupObjectBuilderAdapter = new ParameterGroupObjectBuilderAdapter(reflectionCache, expressionManager, resolverSet);
  }

  @Override
  protected Object resolveFromParameterGroup(String parameterName) throws ValueResolvingException, MuleException {
    Optional<ParameterGroupDescriptor> parameterGroupDescriptor = getParameterGroupDescriptor(parameterName);
    if (parameterGroupDescriptor.isPresent()) {
      return paramGroupObjectBuilderAdapter.build(parameterGroupDescriptor.get());
    }
    return super.resolveFromParameterGroup(parameterName);
  }

  private Optional<ParameterGroupDescriptor> getParameterGroupDescriptor(String parameterGroupName) {
    return model.getParameterGroupModels().stream()
        .filter(pgm -> parameterGroupName.equals(pgm.getName()))
        .filter(pgm -> !pgm.isShowInDsl())
        .findFirst()
        .flatMap(pgm -> pgm.getModelProperty(ParameterGroupModelProperty.class))
        .map(ParameterGroupModelProperty::getDescriptor);
  }

  private static class ParameterGroupObjectBuilderAdapter {

    private final ReflectionCache reflectionCache;
    private final ExpressionManager expressionManager;
    private final LazyValue<Optional<ResolverSetResult>> resultLazyValue;

    private ParameterGroupObjectBuilderAdapter(ReflectionCache reflectionCache, ExpressionManager expressionManager,
                                               ResolverSet resolverSet) {
      this.reflectionCache = reflectionCache;
      this.expressionManager = expressionManager;
      this.resultLazyValue = new LazyValue<>(() -> {
        try (ValueResolvingContext ctx = buildResolvingContext()) {
          return of(resolverSet.resolve(ctx));
        } catch (MuleException e) {
          return empty();
        }
      });
    }

    private ValueResolvingContext buildResolvingContext() {
      return ValueResolvingContext.builder(getNullEvent()).withExpressionManager(expressionManager).build();
    }

    private <T> Optional<T> build(ParameterGroupDescriptor descriptor) {
      return resultLazyValue.get().flatMap(resolverSetResult -> doBuild(descriptor, resolverSetResult));
    }

    private <T> Optional<T> doBuild(ParameterGroupDescriptor descriptor, ResolverSetResult resolverSetResult) {
      try {
        return of(new ParameterGroupObjectBuilder<T>(descriptor, reflectionCache, expressionManager).build(resolverSetResult));
      } catch (MuleException e) {
        return empty();
      }
    }
  }
}
