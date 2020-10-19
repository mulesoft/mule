/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;
import static org.mule.runtime.module.tooling.internal.artifact.params.ParameterExtractor.extractValue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.runtime.config.ResolverSetBasedParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.HashMap;
import java.util.Map;

public class AbstractParameterResolverExecutor {

  protected final MuleContext muleContext;
  protected final ExpressionManager expressionManager;
  protected final ReflectionCache reflectionCache;
  protected final ArtifactHelper artifactHelper;

  public static final String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";

  public AbstractParameterResolverExecutor(MuleContext muleContext, ExpressionManager expressionManager,
                                           ReflectionCache reflectionCache, ArtifactHelper artifactHelper) {
    this.muleContext = muleContext;
    this.expressionManager = expressionManager;
    this.reflectionCache = reflectionCache;
    this.artifactHelper = artifactHelper;
  }

  protected ParameterValueResolver parameterValueResolver(ParameterizedElementDeclaration parameterizedElementDeclaration,
                                                          ParameterizedModel parameterizedModel)
      throws ExpressionNotSupportedException {
    Map<String, Object> parametersMap = parametersMap(parameterizedElementDeclaration, parameterizedModel);

    try {
      final ResolverSet resolverSet =
          ParametersResolver.fromValues(parametersMap,
                                        muleContext,
                                        // Required parameters should not invalidate the resolution of resolving ValueProviders
                                        true,
                                        reflectionCache,
                                        expressionManager,
                                        parameterizedModel.getName())
              .getParametersAsResolverSet(parameterizedModel, muleContext);
      return new ResolverSetBasedParameterResolver(resolverSet, parameterizedModel, reflectionCache, expressionManager);
    } catch (ConfigurationException e) {
      throw new MuleRuntimeException(createStaticMessage("Error resolving parameters values from declaration"), e);
    }
  }

  protected Map<String, Object> parametersMap(ParameterizedElementDeclaration parameterizedElementDeclaration,
                                              ParameterizedModel parameterizedModel)
      throws ExpressionNotSupportedException {
    Map<String, Object> parametersMap = new HashMap<>();

    Map<String, ParameterGroupModel> parameterGroups =
        parameterizedModel.getParameterGroupModels().stream().collect(toMap(NamedObject::getName, identity()));

    for (ParameterGroupElementDeclaration parameterGroupElement : parameterizedElementDeclaration.getParameterGroups()) {
      final String parameterGroupName = parameterGroupElement.getName();
      final ParameterGroupModel parameterGroupModel = parameterGroups.get(parameterGroupName);
      if (parameterGroupModel == null) {
        throw new MuleRuntimeException(createStaticMessage("Could not find parameter group with name: '%s' in model",
                                                           parameterGroupName));
      }

      for (ParameterElementDeclaration parameterElement : parameterGroupElement.getParameters()) {
        final String parameterName = parameterElement.getName();
        final ParameterModel parameterModel = parameterGroupModel.getParameter(parameterName)
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find parameter with name: '%s' in parameter group: '%s'",
                                                                            parameterName, parameterGroupName)));
        Object value = extractValue(parameterElement.getValue(),
                                    artifactHelper.getParameterClass(parameterModel, parameterizedElementDeclaration));
        if (!parameterModel.getExpressionSupport().equals(NOT_SUPPORTED) && isExpression(value)) {
          throw new ExpressionNotSupportedException(format("Error resolving value for parameter: '%s' from declaration, it cannot be an EXPRESSION value",
                                                           parameterName));
        }
        parametersMap.put(parameterName, value);
      }
    }
    return parametersMap;
  }

}
