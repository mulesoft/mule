/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static java.lang.String.format;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.artifact.metadata.MetadataComponentExecutor;
import org.mule.runtime.module.tooling.internal.artifact.metadata.MetadataKeysExecutor;
import org.mule.runtime.module.tooling.internal.artifact.sampledata.SampleDataExecutor;
import org.mule.runtime.module.tooling.internal.artifact.value.ValueProviderExecutor;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Optional;

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

  @Inject
  private SampleDataService sampleDataService;

  private final LazyValue<ArtifactHelper> artifactHelperLazyValue;
  private final LazyValue<ValueProviderExecutor> valueProviderExecutorLazyValue;
  private final LazyValue<MetadataKeysExecutor> metadataKeysExecutorLazyValue;
  private final LazyValue<MetadataComponentExecutor> metadataComponentExecutorLazyValue;
  private final LazyValue<SampleDataExecutor> sampleDataExecutorLazyValue;

  InternalDeclarationSession(ArtifactDeclaration artifactDeclaration) {
    this.artifactHelperLazyValue =
        new LazyValue<>(() -> new ArtifactHelper(extensionManager, componentLocator, artifactDeclaration));

    this.valueProviderExecutorLazyValue =
        new LazyValue<>(() -> new ValueProviderExecutor(muleContext, connectionManager, expressionManager, reflectionCache,
                                                        artifactHelper()));
    this.metadataKeysExecutorLazyValue =
        new LazyValue<>(() -> new MetadataKeysExecutor(connectionManager, reflectionCache, artifactHelper()));

    this.metadataComponentExecutorLazyValue =
        new LazyValue<>(() -> new MetadataComponentExecutor(connectionManager, reflectionCache, artifactHelper()));

    this.sampleDataExecutorLazyValue =
        new LazyValue<>(() -> new SampleDataExecutor(muleContext, connectionManager, expressionManager, sampleDataService,
                                                     reflectionCache, artifactHelper()));
  }

  private ArtifactHelper artifactHelper() {
    return artifactHelperLazyValue.get();
  }

  private ValueProviderExecutor valueProviderExecutor() {
    return valueProviderExecutorLazyValue.get();
  }

  private MetadataKeysExecutor metadataKeysExecutor() {
    return metadataKeysExecutorLazyValue.get();
  }

  private MetadataComponentExecutor metadataComponentExecutor() {
    return metadataComponentExecutorLazyValue.get();
  }

  private SampleDataExecutor sampleDataExecutor() {
    return sampleDataExecutorLazyValue.get();
  }

  @Override
  public ConnectionValidationResult testConnection(String configName) {
    return artifactHelper()
        .findConnectionProvider(configName)
        .map(cp -> connectionManager.testConnectivity(cp))
        .orElseGet(() -> failure(format("Could not perform test connection for configuration: '%s'. Connection provider is not defined",
                                        configName),
                                 new MuleRuntimeException(createStaticMessage("Could not find connection provider"))));
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration parameterizedElementDeclaration, String providerName) {
    Optional<ExtensionModel> optionalExtensionModel = artifactHelper().findExtension(parameterizedElementDeclaration);
    if (!optionalExtensionModel.isPresent()) {
      return resultFrom(newFailure()
          .withMessage(extensionNotFoundErrorMessage(parameterizedElementDeclaration.getDeclaringExtension()))
          .withFailureCode(COMPONENT_NOT_FOUND.getName())
          .build());
    }

    Optional<? extends ParameterizedModel> optionalParameterizedModel =
        artifactHelper().findModel(optionalExtensionModel.get(), parameterizedElementDeclaration);
    if (!optionalParameterizedModel.isPresent()) {
      return resultFrom(newFailure()
          .withMessage(couldNotFindComponentErrorMessage(parameterizedElementDeclaration))
          .withFailureCode(COMPONENT_NOT_FOUND.getName())
          .build());
    }
    return valueProviderExecutor().resolveValues(optionalParameterizedModel.get(), parameterizedElementDeclaration,
                                                 providerName);
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentElementDeclaration componentElementDeclaration) {
    Optional<ExtensionModel> optionalExtensionModel = artifactHelper().findExtension(componentElementDeclaration);
    if (!optionalExtensionModel.isPresent()) {
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(extensionNotFoundErrorMessage(componentElementDeclaration.getDeclaringExtension()))
          .withFailureCode(COMPONENT_NOT_FOUND)
          .onKeys());
    }

    Optional<? extends ComponentModel> optionalComponentModel =
        artifactHelper().findComponentModel(optionalExtensionModel.get(), componentElementDeclaration);
    if (!optionalComponentModel.isPresent()) {
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(couldNotFindComponentErrorMessage(componentElementDeclaration))
          .withFailureCode(COMPONENT_NOT_FOUND)
          .onKeys());
    }

    return metadataKeysExecutor().resolveMetadataKeys(optionalComponentModel.get(), componentElementDeclaration);
  }


  @Override
  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentElementDeclaration componentElementDeclaration) {
    Optional<ExtensionModel> optionalExtensionModel = artifactHelper().findExtension(componentElementDeclaration);
    if (!optionalExtensionModel.isPresent()) {
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(extensionNotFoundErrorMessage(componentElementDeclaration.getDeclaringExtension()))
          .withFailureCode(COMPONENT_NOT_FOUND)
          .onComponent());
    }

    Optional<? extends ComponentModel> optionalComponentModel =
        artifactHelper().findComponentModel(optionalExtensionModel.get(), componentElementDeclaration);
    if (!optionalComponentModel.isPresent()) {
      return MetadataResult.failure(MetadataFailure.Builder.newFailure()
          .withMessage(couldNotFindComponentErrorMessage(componentElementDeclaration))
          .withFailureCode(COMPONENT_NOT_FOUND)
          .onComponent());
    }

    return metadataComponentExecutor().resolveComponentMetadata(optionalComponentModel.get(), componentElementDeclaration);

  }

  @Override
  public SampleDataResult getSampleData(ComponentElementDeclaration componentElementDeclaration) {
    Optional<ExtensionModel> optionalExtensionModel = artifactHelper().findExtension(componentElementDeclaration);
    if (!optionalExtensionModel.isPresent()) {
      return SampleDataResult.resultFrom(SampleDataFailure.Builder.newFailure()
          .withMessage(extensionNotFoundErrorMessage(componentElementDeclaration.getDeclaringExtension()))
          .withFailureCode(COMPONENT_NOT_FOUND.getName())
          .build());
    }

    Optional<? extends ComponentModel> optionalComponentModel =
        artifactHelper().findComponentModel(optionalExtensionModel.get(), componentElementDeclaration);
    if (!optionalComponentModel.isPresent()) {
      return SampleDataResult.resultFrom(SampleDataFailure.Builder.newFailure()
          .withMessage(couldNotFindComponentErrorMessage(componentElementDeclaration))
          .withFailureCode(COMPONENT_NOT_FOUND.getName())
          .build());
    }

    return sampleDataExecutor().getSampleData(optionalComponentModel.get(), componentElementDeclaration);
  }

  private String couldNotFindComponentErrorMessage(ElementDeclaration declaration) {
    return format("Could not find component: '%s:%s'", declaration.getDeclaringExtension(), declaration.getName());
  }

  private String extensionNotFoundErrorMessage(String declaringExtension) {
    return format("ElementDeclaration is defined for extension: '%s' which is not part of the context: '%s'", declaringExtension,
                  artifactHelper().getExtensions());
  }

  @Override
  public void dispose() {
    //do nothing
  }

}
