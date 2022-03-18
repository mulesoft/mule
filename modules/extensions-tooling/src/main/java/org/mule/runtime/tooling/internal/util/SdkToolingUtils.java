/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal.util;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

public final class SdkToolingUtils {

  private static final Logger LOGGER = getLogger(SdkToolingUtils.class);

  private SdkToolingUtils() {}

  public static ResolverSet toResolverSet(Map<String, ?> values,
                                          ParameterizedModel parameterizedModel,
                                          MuleContext muleContext,
                                          ReflectionCache reflectionCache,
                                          ExpressionManager expressionManager)
      throws Exception {

    ParametersResolver parametersResolver = fromValues(values,
                                                       muleContext,
                                                       true,
                                                       reflectionCache,
                                                       expressionManager);

    ResolverSet typeUnsafeResolverSet = parametersResolver.getParametersAsResolverSet(muleContext,
                                                                                      parameterizedModel,
                                                                                      parameterizedModel
                                                                                          .getParameterGroupModels());

    Map<String, ParameterModel> paramModels =
        parameterizedModel.getAllParameterModels().stream().collect(toMap(p -> p.getName(), identity()));

    ResolverSet typeSafeResolverSet = new ResolverSet(muleContext);
    typeUnsafeResolverSet.getResolvers().forEach((paramName, resolver) -> {
      ParameterModel model = paramModels.get(paramName);
      if (model != null) {
        Optional<Class<Object>> clazz = getType(model.getType());
        if (clazz.isPresent()) {
          resolver = new TypeSafeValueResolverWrapper(resolver, clazz.get());
        }
      }

      typeSafeResolverSet.add(paramName, resolver);
    });

    typeSafeResolverSet.initialise();
    return typeSafeResolverSet;
  }

  public static void stopAndDispose(Object object) {
    if (object == null) {
      return;
    }

    try {
      stopIfNeeded(object);
    } catch (MuleException e) {
      LOGGER.error("Exception trying to stop " + object, e);
    } finally {
      disposeIfNeeded(object, LOGGER);
    }
  }
}
