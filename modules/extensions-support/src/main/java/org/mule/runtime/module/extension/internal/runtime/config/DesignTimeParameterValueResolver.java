/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getShowInDslParameters;

import static java.lang.String.format;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
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

import java.util.Map;
import java.util.Optional;

/**
 * {@link ParameterValueResolver} implementation for design-time services.
 *
 * @implNote This is the same as {@link ResolverSetBasedParameterResolver} but also resolves objects if a parameter group name is
 *           given, similarly to {@link OperationParameterValueResolver}, that is needed, for example, for multi-level metadata
 *           key resolution.
 * @since 4.8
 */
public final class DesignTimeParameterValueResolver extends ResolverSetBasedParameterResolver {

  private final ParameterizedModel model;
  private final ParameterGroupObjectBuilderAdapter paramGroupObjectBuilderAdapter;
  private final LazyValue<Map<String, String>> shownInDslParameterGroups;

  public DesignTimeParameterValueResolver(ResolverSet resolverSet,
                                          ParameterizedModel parameterizedModel,
                                          ReflectionCache reflectionCache, ExpressionManager expressionManager) {
    super(resolverSet, parameterizedModel, reflectionCache, expressionManager);
    this.model = parameterizedModel;
    this.paramGroupObjectBuilderAdapter = new ParameterGroupObjectBuilderAdapter(reflectionCache, expressionManager, resolverSet);
    this.shownInDslParameterGroups = new LazyValue<>(() -> getShowInDslParameters(parameterizedModel));
  }

  @Override
  protected Object resolveFromParameterGroup(String parameterName) throws ValueResolvingException, MuleException {
    Optional<Pair<ParameterGroupDescriptor, ParameterGroupModel>> parameterGroupDescriptor =
        getParameterGroupDescriptor(parameterName);
    if (parameterGroupDescriptor.isPresent()) {
      if (parameterGroupDescriptor.get().getSecond().isShowInDsl()) {
        // It is possible that the parameterName here is the aliased name where in fact the resolver set has a resolver for the
        // un-aliased parameter group name.
        return resolveFromResolverSetWithAdjustedContainerName(parameterGroupDescriptor.get().getFirst());
      } else {
        // If the parameter group is not shown in DSL, there will be no resolver in the set, but we still need to provide an
        // object for the group, for example for multi-level metadata key resolution. So, we will delegate to a
        // ParameterGroupObjectBuilder
        return resolveFromParameterGroupNotShownInDsl(parameterGroupDescriptor.get().getFirst(), parameterName);
      }
    }
    if (!shownInDslParameterGroups.get().containsKey(parameterName)) {
      // The base class implementation would fail if there is no resolver for the parameter, but will try to lookup for a resolver
      // for the containing group first (only if shown in DSL).
      return null;
    }
    return super.resolveFromParameterGroup(parameterName);
  }

  private Optional<Pair<ParameterGroupDescriptor, ParameterGroupModel>> getParameterGroupDescriptor(String parameterGroupName) {
    return model.getParameterGroupModels().stream()
        .filter(pgm -> parameterGroupName.equals(pgm.getName()))
        .findFirst()
        .flatMap(pgm -> pgm.getModelProperty(ParameterGroupModelProperty.class)
            .map(mp -> new Pair<>(mp.getDescriptor(), pgm)));
  }

  private Object resolveFromParameterGroupNotShownInDsl(ParameterGroupDescriptor parameterGroupDescriptor, String parameterName)
      throws ValueResolvingException {
    try {
      return paramGroupObjectBuilderAdapter.build(parameterGroupDescriptor);
    } catch (MuleException e) {
      throw new ValueResolvingException(format("Error occurred trying to resolve value for the parameter [%s]", parameterName),
                                        e);
    }
  }

  private Object resolveFromResolverSetWithAdjustedContainerName(ParameterGroupDescriptor parameterGroupDescriptor)
      throws ValueResolvingException {
    return getParameterValue(getContainerName(parameterGroupDescriptor.getContainer()));
  }

  private static class ParameterGroupObjectBuilderAdapter {

    private final ReflectionCache reflectionCache;
    private final ExpressionManager expressionManager;
    private final LazyValue<Either<MuleException, ResolverSetResult>> resultLazyValue;

    private ParameterGroupObjectBuilderAdapter(ReflectionCache reflectionCache, ExpressionManager expressionManager,
                                               ResolverSet resolverSet) {
      this.reflectionCache = reflectionCache;
      this.expressionManager = expressionManager;
      this.resultLazyValue = new LazyValue<>(() -> {
        try (ValueResolvingContext ctx = buildResolvingContext()) {
          return right(resolverSet.resolve(ctx));
        } catch (MuleException e) {
          return left(e);
        }
      });
    }

    private ValueResolvingContext buildResolvingContext() {
      return ValueResolvingContext.builder(getNullEvent()).withExpressionManager(expressionManager).build();
    }

    private <T> T build(ParameterGroupDescriptor descriptor) throws MuleException {
      Either<MuleException, ResolverSetResult> resolverSetResultEither = resultLazyValue.get();
      if (resolverSetResultEither.isLeft()) {
        throw resolverSetResultEither.getLeft();
      }
      return doBuild(descriptor, resolverSetResultEither.getRight());
    }

    private <T> T doBuild(ParameterGroupDescriptor descriptor, ResolverSetResult resolverSetResult)
        throws MuleException {
      return new ParameterGroupObjectBuilder<T>(descriptor, reflectionCache, expressionManager).build(resolverSetResult);
    }
  }
}
