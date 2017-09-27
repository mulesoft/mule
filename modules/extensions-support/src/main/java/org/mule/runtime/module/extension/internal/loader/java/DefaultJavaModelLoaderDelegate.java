/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel.ExternalLibraryModelBuilder;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.DeclaresExternalLibraries;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.ExternalLibs;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.runtime.extension.api.annotation.license.RequiresEntitlement;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.contributor.InfrastructureFieldContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.StackableTypesParameterContributor;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionTypeFactory;
import org.mule.runtime.module.extension.internal.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.java.type.WithParameters;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class provided in the constructor
 *
 * @since 4.0
 */
public class DefaultJavaModelLoaderDelegate implements ModelLoaderDelegate {

  protected final Class<?> extensionType;
  protected final ClassTypeLoader typeLoader;
  protected final String version;

  private final ConfigModelLoaderDelegate configLoaderDelegate = new ConfigModelLoaderDelegate(this);
  private final OperationModelLoaderDelegate operationLoaderDelegate = new OperationModelLoaderDelegate(this);
  private final FunctionModelLoaderDelegate functionModelLoaderDelegate = new FunctionModelLoaderDelegate(this);
  private final SourceModelLoaderDelegate sourceModelLoaderDelegate = new SourceModelLoaderDelegate(this);
  private final ConnectionProviderModelLoaderDelegate connectionProviderModelLoaderDelegate =
      new ConnectionProviderModelLoaderDelegate(this);

  private final ParameterModelsLoaderDelegate fieldParametersLoader;
  private final ParameterModelsLoaderDelegate methodParametersLoader;

  public DefaultJavaModelLoaderDelegate(Class<?> extensionType, String version) {
    checkArgument(extensionType != null, format("describer %s does not specify an extension type", getClass().getName()));
    this.extensionType = extensionType;
    this.version = version;
    this.typeLoader = getDefault().createTypeLoader(extensionType.getClassLoader());

    this.fieldParametersLoader = new ParameterModelsLoaderDelegate(getParameterFieldsContributors(), typeLoader);
    this.methodParametersLoader = new ParameterModelsLoaderDelegate(getParameterMethodsContributors(), typeLoader);
  }

  private List<ParameterDeclarerContributor> getParameterMethodsContributors() {
    return ImmutableList
        .of(StackableTypesParameterContributor.defaultContributor(typeLoader));
  }

  private ImmutableList<ParameterDeclarerContributor> getParameterFieldsContributors() {
    return ImmutableList
        .of(new InfrastructureFieldContributor(),
            StackableTypesParameterContributor.defaultContributor(typeLoader));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    final ExtensionElement extensionElement = ExtensionTypeFactory.getExtensionType(extensionType);
    Extension extension = MuleExtensionAnnotationParser.getExtension(extensionType);
    ExtensionDeclarer declarer =
        context.getExtensionDeclarer()
            .named(extension.name())
            .onVersion(version)
            .fromVendor(extension.vendor())
            .withCategory(extension.category())
            .withModelProperty(new ImplementingTypeModelProperty(extensionType));

    processLicenseRequirements(declarer);
    parseExternalLibs(extensionElement, declarer);
    addExceptionEnricher(extensionElement, declarer);

    configLoaderDelegate.declareConfigurations(declarer, extensionElement);
    connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, extensionElement);

    if (!isEmpty(extensionElement.getConfigurations())) {
      operationLoaderDelegate
          .declareOperations(declarer, declarer, null, extensionElement.getOperations(), false);

      functionModelLoaderDelegate
          .declareFunctions(declarer, declarer, null, extensionElement.getFunctions());

      extensionElement.getSources()
          .forEach(source -> sourceModelLoaderDelegate.declareMessageSource(declarer, declarer, source, false));
    }

    return declarer;
  }

  private void processLicenseRequirements(ExtensionDeclarer declarer) {
    Optional<RequiresEntitlement> requiresEntitlementOptional =
        MuleExtensionAnnotationParser.getOptionalAnnotation(extensionType, RequiresEntitlement.class);
    Optional<RequiresEnterpriseLicense> requiresEnterpriseLicenseOptional =
        MuleExtensionAnnotationParser.getOptionalAnnotation(extensionType, RequiresEnterpriseLicense.class);
    boolean requiresEnterpriseLicense =
        requiresEnterpriseLicenseOptional.map(requiresEnterpriseLicenseAnnotation -> true).orElse(false);
    boolean allowsEvaluationLicense =
        requiresEnterpriseLicenseOptional.map(RequiresEnterpriseLicense::allowEvaluationLicense).orElse(true);
    Optional<String> requiredEntitlement = ofNullable(requiresEntitlementOptional.map(RequiresEntitlement::name).orElse(null));

    declarer.withModelProperty(new LicenseModelProperty(requiresEnterpriseLicense, allowsEvaluationLicense, requiredEntitlement));
  }

  void parseExternalLibs(WithAnnotations withAnnotations, DeclaresExternalLibraries declarer) {
    Optional<ExternalLibs> externalLibs = withAnnotations.getAnnotation(ExternalLibs.class);
    if (externalLibs.isPresent()) {
      stream(externalLibs.get().value()).forEach(lib -> parseExternalLib(declarer, lib));
    } else {
      withAnnotations.getAnnotation(ExternalLib.class).ifPresent(lib -> parseExternalLib(declarer, lib));
    }
  }

  private void parseExternalLib(DeclaresExternalLibraries declarer, ExternalLib externalLibAnnotation) {

    ExternalLibraryModelBuilder builder = ExternalLibraryModel.builder()
        .withName(externalLibAnnotation.name())
        .withDescription(externalLibAnnotation.description())
        .withType(externalLibAnnotation.type())
        .isOptional(externalLibAnnotation.optional());

    ifNotBlank(externalLibAnnotation.nameRegexpMatcher(), builder::withRegexpMatcher);
    ifNotBlank(externalLibAnnotation.requiredClassName(), builder::withRequiredClassName);
    ifNotBlank(externalLibAnnotation.coordinates(), builder::withCoordinates);

    declarer.withExternalLibrary(builder.build());
  }

  <M extends WithAnnotations> HasModelProperties addExceptionEnricher(M model, HasModelProperties declarer) {
    MuleExtensionAnnotationParser.getExceptionEnricherFactory(model).map(ExceptionHandlerModelProperty::new)
        .ifPresent(declarer::withModelProperty);
    return declarer;
  }

  Class<?>[] getOperationClasses(Class<?> extensionType) {
    Operations operations = extensionType.getAnnotation(Operations.class);
    return operations == null ? EMPTY_CLASS_ARRAY : operations.value();
  }

  boolean isInvalidConfigSupport(boolean supportsConfig, Optional<ExtensionParameter>... parameters) {
    return !supportsConfig && Stream.of(parameters).anyMatch(Optional::isPresent);
  }

  Declarer selectDeclarerBasedOnConfig(ExtensionDeclarer extensionDeclarer,
                                       Declarer declarer,
                                       Optional<ExtensionParameter>... parameters) {

    for (Optional<ExtensionParameter> parameter : parameters) {
      if (parameter.isPresent()) {
        return declarer;
      }
    }

    return extensionDeclarer;
  }

  Optional<ExtensionParameter> getConfigParameter(WithParameters element) {
    return element.getParametersAnnotatedWith(Config.class).stream().findFirst();
  }

  Optional<ExtensionParameter> getConnectionParameter(WithParameters element) {
    return element.getParametersAnnotatedWith(Connection.class).stream().findFirst();
  }

  ConfigModelLoaderDelegate getConfigLoaderDelegate() {
    return configLoaderDelegate;
  }

  OperationModelLoaderDelegate getOperationLoaderDelegate() {
    return operationLoaderDelegate;
  }

  FunctionModelLoaderDelegate getFunctionModelLoaderDelegate() {
    return functionModelLoaderDelegate;
  }

  SourceModelLoaderDelegate getSourceModelLoaderDelegate() {
    return sourceModelLoaderDelegate;
  }

  ConnectionProviderModelLoaderDelegate getConnectionProviderModelLoaderDelegate() {
    return connectionProviderModelLoaderDelegate;
  }

  ClassTypeLoader getTypeLoader() {
    return typeLoader;
  }

  Class<?> getExtensionType() {
    return extensionType;
  }

  protected ParameterModelsLoaderDelegate getFieldParametersLoader() {
    return fieldParametersLoader;
  }

  protected ParameterModelsLoaderDelegate getMethodParametersLoader() {
    return methodParametersLoader;
  }
}
