/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.module.tooling.internal.config.params.ParameterExtractor.extractValue;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.config.ResolverSetBasedParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.ValueProviderMediator;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

public class InternalDeclarationSession implements DeclarationSession {

  private static final Supplier<Object> NULL_SUPPLIER = () -> null;

  @Inject
  private ConfigurationComponentLocator componentLocator;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private MuleContext muleContext;

  @Inject
  private ConnectionManager connectionManager;

  @Inject
  private ExpressionManager expressionManager;

  private LazyValue<ArtifactHelper> artifactHelperLazyValue;

  InternalDeclarationSession(ArtifactDeclaration artifactDeclaration) {
    this.artifactHelperLazyValue =
        new LazyValue<>(() -> new ArtifactHelper(extensionManager, componentLocator, artifactDeclaration));
  }

  private ArtifactHelper artifactHelper() {
    return artifactHelperLazyValue.get();
  }

  @Override
  public ConnectionValidationResult testConnection(String configName) {
    return artifactHelper()
        .findConnectionProvider(configName)
        .map(cp -> {
          Object connection = null;
          try {
            connection = cp.connect();
            return cp.validate(connection);
          } catch (Exception e) {
            return failure("Could not perform connectivity testing", e);
          } finally {
            if (connection != null) {
              cp.disconnect(connection);
            }
          }
        })
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find connection provider")));
  }

  @Override
  public ValueResult getValues(ComponentElementDeclaration component, String parameterName) {
    return artifactHelper()
        .findComponentModel(component)
        .map(cm -> discoverValues(cm, parameterName, parameterValueResolver(component, cm), ofNullable(component.getConfigRef())))
        .orElse(resultFrom(emptySet()));
  }

  @Override
  public void dispose() {
    //do nothing
  }

  private <T extends ComponentModel> ValueResult discoverValues(T componentModel,
                                                                String parameterName,
                                                                ParameterValueResolver parameterValueResolver,
                                                                Optional<String> configName) {
    ValueProviderMediator<T> valueProviderMediator = createValueProviderMediator(componentModel);
    try {
      return resultFrom(valueProviderMediator.getValues(parameterName,
                                                        parameterValueResolver,
                                                        configName.map(this::connectionSupplier).orElse(NULL_SUPPLIER),
                                                        configName.map(this::configSupplier).orElse(NULL_SUPPLIER)));
    } catch (ValueResolvingException e) {
      return resultFrom(newFailure(e).build());
    }
  }

  private <T extends ComponentModel> ValueProviderMediator<T> createValueProviderMediator(T constructModel) {
    return new ValueProviderMediator<>(constructModel,
                                       () -> muleContext,
                                       () -> reflectionCache);
  }

  private Supplier<Object> connectionSupplier(String configName) {
    return artifactHelper().getConnectionInstance(configName)
        .map(ci -> (Supplier<Object>) () -> ci)
        .orElse(NULL_SUPPLIER);

  }

  private Supplier<Object> configSupplier(String configName) {
    return artifactHelper().getConfigurationInstance(configName)
        .map(ci -> (Supplier<Object>) () -> ci)
        .orElse(NULL_SUPPLIER);
  }

  private <T extends ComponentModel> ParameterValueResolver parameterValueResolver(ComponentElementDeclaration componentElementDeclaration,
                                                                                   T model) {
    Map<String, Object> parametersMap = new HashMap<>();

    Map<String, ParameterGroupModel> parameterGroups =
        model.getParameterGroupModels().stream().collect(toMap(NamedObject::getName, identity()));

    for (ParameterGroupElementDeclaration parameterGroupElement : componentElementDeclaration.getParameterGroups()) {
      final String parameterGroupName = parameterGroupElement.getName();
      final ParameterGroupModel parameterGroupModel = parameterGroups.get(parameterGroupName);
      if (parameterGroupModel == null) {
        throw new MuleRuntimeException(createStaticMessage("Could not find parameter group with name: %s in model",
                                                           parameterGroupName));
      }

      for (ParameterElementDeclaration parameterElement : parameterGroupElement.getParameters()) {
        final String parameterName = parameterElement.getName();
        final ParameterModel parameterModel = parameterGroupModel.getParameter(parameterName)
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find parameter with name: %s in parameter group: %s",
                                                                            parameterName, parameterGroupName)));
        parametersMap.put(parameterName,
                          extractValue(parameterElement.getValue(),
                                       artifactHelper().getParameterClass(parameterModel, componentElementDeclaration)));
      }
    }

    try {
      final ResolverSet resolverSet =
          ParametersResolver.fromValues(parametersMap,
                                        muleContext,
                                        false,
                                        reflectionCache,
                                        expressionManager,
                                        model.getName())
              .getParametersAsResolverSet(model, muleContext);
      return new ResolverSetBasedParameterResolver(resolverSet, model, reflectionCache, expressionManager);
    } catch (ConfigurationException e) {
      throw new MuleRuntimeException(e);
    }
  }

}
