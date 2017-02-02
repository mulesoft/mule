/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.NullSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.List;

/**
 * Utilities for creating object with implicit values based on a {@link ParameterizedModel}
 *
 * @since 4.0
 */
public final class ImplicitObjectUtils {

  private ImplicitObjectUtils() {}

  /**
   * Creates a {@link ResolverSet} based on the default values for the {@link ParameterModel}s in the {@code parametrizedModel}.
   * <p>
   * If a {@link ParameterModel} returns {@code null} for {@link ParameterModel#getDefaultValue()} then it's ignored
   *
   * @param parameterizedModel a model holding the {@link ParameterModel}s to consider
   * @param muleContext        the Mule node.
   * @return a {@link ResolverSet}
   */
  // TODO - MULE-11610 : Implicit resolvers doesn't use the same resolving mechanism that for a defined element
  public static ResolverSet buildImplicitResolverSet(ParameterizedModel parameterizedModel, MuleContext muleContext) {
    ResolverSet resolverSet = new ResolverSet();
    ParametersResolver parametersResolver =
        ParametersResolver.fromDefaultValues(parameterizedModel, muleContext);

    for (ParameterModel parameterModel : parameterizedModel.getAllParameterModels()) {
      Object defaultValue = parameterModel.getDefaultValue();
      ValueResolver resolver;

      resolver = defaultValue != null
          ? new TypeSafeExpressionValueResolver<>((String) defaultValue, getType(parameterModel.getType()), muleContext)
          : new StaticValueResolver<>(null);

      if (parameterModel.getModelProperty(NullSafeModelProperty.class).isPresent()) {
        resolver = NullSafeValueResolverWrapper.of(resolver, parameterModel.getType(), muleContext, parametersResolver);
      }

      resolverSet.add(parameterModel.getName(), resolver);
    }

    return resolverSet;
  }

  /**
   * Returns the first item in the {@code models} {@link List} that can be used implicitly.
   * <p>
   * A {@link ParameterizedModel} is consider to be implicit when all its {@link ParameterModel}s are either optional or have a
   * default value
   *
   * @param models a {@link List} of {@code T}
   * @param <T>    the generic type of the items in the {@code models}. It's a type which is assignable from
   *               {@link ParameterizedModel}
   * @return one of the items in {@code models} or {@code null} if none of the models are implicit
   */
  public static <T extends ParameterizedModel> T getFirstImplicit(List<T> models) {
    return models.stream()
        .filter(ImplicitObjectUtils::canBeUsedImplicitly)
        .findFirst()
        .orElse(null);
  }

  private static boolean canBeUsedImplicitly(ParameterizedModel parameterizedModel) {
    return parameterizedModel.getAllParameterModels().stream().noneMatch(p -> p.isRequired() && p.getDefaultValue() == null);
  }
}
