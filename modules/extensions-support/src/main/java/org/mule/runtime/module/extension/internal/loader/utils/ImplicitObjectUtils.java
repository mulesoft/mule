/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.getExpressionBasedValueResolver;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.HashedResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.NullSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

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
   * @param muleContext the Mule node.
   * @return a {@link ResolverSet}
   */
  public static ResolverSet buildImplicitResolverSet(ParameterizedModel parameterizedModel, MuleContext muleContext) {
    ResolverSet resolverSet = new HashedResolverSet(muleContext);
    ParametersResolver parametersResolver =
        ParametersResolver.fromDefaultValues(parameterizedModel, muleContext);

    for (ParameterModel parameterModel : parameterizedModel.getAllParameterModels()) {
      Object defaultValue = parameterModel.getDefaultValue();
      ValueResolver resolver;
      if (defaultValue instanceof String) {
        resolver = getExpressionBasedValueResolver((String) defaultValue, parameterModel.getType(), muleContext);
      } else {
        resolver = new StaticValueResolver<>(null);
      }

      if (parameterModel.getModelProperty(NullSafeModelProperty.class).isPresent()) {
        MetadataType metadataType = parameterModel.getModelProperty(NullSafeModelProperty.class).get().defaultType();
        resolver = NullSafeValueResolverWrapper.of(resolver, metadataType, muleContext, parametersResolver);
      }

      resolverSet.add(parameterModel.getName(), resolver);
    }

    return resolverSet;
  }
}
