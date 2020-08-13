/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.tooling.internal.config.params.ParameterExtractor.extractValue;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.metadata.cache.DefaultMetadataCache;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.config.ResolverSetBasedParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.ValueProviderMediator;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.config.metadata.MetadataKeyDeclarationResolver;
import org.mule.runtime.module.tooling.internal.config.metadata.MetadataKeyResult;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class InternalDeclarationSession implements DeclarationSession {

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
        .map(cp -> connectionManager.testConnectivity(cp))
        .orElseGet(() -> failure(format("Could not find a connection provider for configuration: '%s'", configName),
                                 new MuleRuntimeException(createStaticMessage("Could not find connection provider"))));
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration component, String parameterName) {
    return artifactHelper()
        .findModel(component)
        .map(cm -> discoverValues(cm, parameterName, parameterValueResolver(component, cm), getConfigRef(component)))
        .orElse(resultFrom(newFailure()
            .withMessage("Could not resolve values")
            .withReason(format("Could not find component: %s:%s", component.getDeclaringExtension(), component.getName()))
            .build()));
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentElementDeclaration component) {
    return artifactHelper()
        .findComponentModel(component)
        .map(cm -> {
          Optional<ConfigurationInstance> configurationInstance =
              ofNullable(component.getConfigRef()).flatMap(name -> artifactHelper().getConfigurationInstance(name));
          MetadataKey metadataKey = new MetadataKeyDeclarationResolver(cm, component).resolveKey();
          ClassLoader extensionClassLoader = getClassLoader(artifactHelper().getExtensionModel(component));
          return withContextClassLoader(extensionClassLoader, () -> {
            DefaultMetadataContext metadataContext = createMetadataContext(configurationInstance, extensionClassLoader);
            return withMetadataContext(metadataContext, () -> new MetadataMediator<>(cm)
                .getMetadataKeys(metadataContext, metadataKey, reflectionCache));
          });
        })
        .orElseGet(() -> MetadataResult.failure(MetadataFailure.Builder.newFailure()
            .withMessage(format("Error resolving metadata keys for the [%s:%s] component",
                                component.getDeclaringExtension(), component.getName()))
            .withFailureCode(COMPONENT_NOT_FOUND)
            .onComponent()));
  }

  @Override
  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentElementDeclaration component) {
    return artifactHelper()
        .findComponentModel(component)
        .map(cm -> {
          Optional<ConfigurationInstance> configurationInstance =
              ofNullable(component.getConfigRef()).flatMap(name -> artifactHelper().getConfigurationInstance(name));

          MetadataKeyResult metadataKeyResult = new MetadataKeyDeclarationResolver(cm, component).resolveKeyResult();
          if (!metadataKeyResult.isComplete()) {
            return MetadataResult.<ComponentMetadataTypesDescriptor>failure(
                                                                            MetadataFailure.Builder.newFailure()
                                                                                .withMessage(metadataKeyResult.getPartialReason())
                                                                                .withFailureCode(INVALID_METADATA_KEY)
                                                                                .onComponent());
          }
          MetadataKey metadataKey = metadataKeyResult.getMetadataKey();
          ClassLoader extensionClassLoader = getClassLoader(artifactHelper().getExtensionModel(component));
          return withContextClassLoader(extensionClassLoader, () -> {
            MetadataMediator<? extends ComponentModel> metadataMediator = new MetadataMediator<>(cm);
            DefaultMetadataContext metadataContext = createMetadataContext(configurationInstance, extensionClassLoader);
            return withMetadataContext(metadataContext, () -> {
              MetadataResult<InputMetadataDescriptor> inputMetadata = metadataMediator
                  .getInputMetadata(metadataContext, metadataKey);
              MetadataResult<OutputMetadataDescriptor> outputMetadata = null;
              if (cm instanceof HasOutputModel) {
                outputMetadata = metadataMediator.getOutputMetadata(metadataContext, metadataKey);
              }
              return collectMetadata(inputMetadata, outputMetadata);
            });
          });
        })
        .orElseGet(() -> MetadataResult.failure(MetadataFailure.Builder.newFailure()
            .withMessage(format("Error resolving metadata for the [%s:%s] component",
                                component.getDeclaringExtension(), component.getName()))
            .withFailureCode(COMPONENT_NOT_FOUND)
            .onComponent()));
  }

  private MetadataResult<ComponentMetadataTypesDescriptor> collectMetadata(@Nonnull MetadataResult<InputMetadataDescriptor> inputMetadataResult,
                                                                           @Nullable MetadataResult<OutputMetadataDescriptor> outputMetadataResult) {
    if (inputMetadataResult.isSuccess() && (outputMetadataResult == null || outputMetadataResult.isSuccess())) {
      ComponentMetadataTypesDescriptor.ComponentMetadataTypesDescriptorBuilder builder =
          ComponentMetadataTypesDescriptor.builder().withInputMetadataDescriptor(inputMetadataResult.get());
      if (outputMetadataResult != null) {
        builder.withOutputMetadataDescriptor(outputMetadataResult.get());
      }
      return MetadataResult.success(builder.build());
    }
    List<MetadataFailure> failures = new ArrayList<>(inputMetadataResult.getFailures());
    if (outputMetadataResult != null) {
      failures.addAll(outputMetadataResult.getFailures());
    }
    return MetadataResult.failure(failures);
  }

  private DefaultMetadataContext createMetadataContext(Optional<ConfigurationInstance> configurationInstance,
                                                       ClassLoader extensionClassLoader) {
    return new DefaultMetadataContext(() -> configurationInstance,
                                      connectionManager,
                                      new DefaultMetadataCache(),
                                      new JavaTypeLoader(extensionClassLoader));
  }

  private static <T> T withMetadataContext(MetadataContext metadataContext, Callable<T> callable) {
    try {
      return callable.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    } finally {
      metadataContext.dispose();
    }
  }

  private Optional<String> getConfigRef(ParameterizedElementDeclaration component) {
    if (component instanceof ComponentElementDeclaration) {
      return ofNullable(((ComponentElementDeclaration) component).getConfigRef());
    }
    return empty();
  }

  @Override
  public void dispose() {
    //do nothing
  }

  private <T extends ParameterizedModel & EnrichableModel> ValueResult discoverValues(T componentModel,
                                                                                      String parameterName,
                                                                                      ParameterValueResolver parameterValueResolver,
                                                                                      Optional<String> configName) {
    ValueProviderMediator<T> valueProviderMediator = createValueProviderMediator(componentModel);
    ExtensionResolvingContext context =
        new ExtensionResolvingContext(() -> configName.flatMap(name -> artifactHelper().getConfigurationInstance(name)),
                                      connectionManager);
    try {
      return resultFrom(valueProviderMediator.getValues(parameterName,
                                                        parameterValueResolver,
                                                        (CheckedSupplier<Object>) () -> context
                                                            .getConnection().orElse(null),
                                                        (CheckedSupplier<Object>) () -> context
                                                            .getConfig().orElse(null)));
    } catch (ValueResolvingException e) {
      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
    } finally {
      context.dispose();
    }
  }

  private <T extends ParameterizedModel & EnrichableModel> ValueProviderMediator<T> createValueProviderMediator(T constructModel) {
    return new ValueProviderMediator<>(constructModel,
                                       () -> muleContext,
                                       () -> reflectionCache);
  }

  private <T extends ParameterizedModel> ParameterValueResolver parameterValueResolver(ParameterizedElementDeclaration parameterizedElementDeclaration,
                                                                                       T model) {
    Map<String, Object> parametersMap = new HashMap<>();

    Map<String, ParameterGroupModel> parameterGroups =
        model.getParameterGroupModels().stream().collect(toMap(NamedObject::getName, identity()));

    for (ParameterGroupElementDeclaration parameterGroupElement : parameterizedElementDeclaration.getParameterGroups()) {
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
                                       artifactHelper().getParameterClass(parameterModel, parameterizedElementDeclaration)));
      }
    }

    try {
      final ResolverSet resolverSet =
          ParametersResolver.fromValues(parametersMap,
                                        muleContext,
                                        // Required parameters should not invalidate the resolution of resolving ValueProviders
                                        true,
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
