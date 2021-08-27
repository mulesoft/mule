/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Arrays.stream;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;
import static org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory.getDefault;
import static org.mule.runtime.module.extension.internal.loader.java.contributor.StackableTypesParameterContributor.defaultContributor;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel.ExternalLibraryModelBuilder;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.DeclaresExternalLibraries;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.ExternalLibs;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.runtime.extension.api.annotation.license.RequiresEntitlement;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.java.contributor.InfrastructureFieldContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class provided in the constructor
 *
 * @since 4.0
 */
public class DefaultJavaModelLoaderDelegate implements ModelLoaderDelegate {

  protected Class<?> extensionType;
  protected final ExtensionElement extensionElement;
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

  public DefaultJavaModelLoaderDelegate(ExtensionElement extensionElement, String version) {
    this.version = version;
    this.typeLoader = getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
    this.extensionElement = extensionElement;

    this.fieldParametersLoader = new ParameterModelsLoaderDelegate(getParameterFieldsContributors(), typeLoader);
    this.methodParametersLoader = new ParameterModelsLoaderDelegate(getParameterMethodsContributors(), typeLoader);
  }

  private List<ParameterDeclarerContributor> getParameterMethodsContributors() {
    return ImmutableList.of(defaultContributor(typeLoader));
  }

  private ImmutableList<ParameterDeclarerContributor> getParameterFieldsContributors() {
    return ImmutableList.of(new InfrastructureFieldContributor(), defaultContributor(typeLoader));
  }

  public DefaultJavaModelLoaderDelegate(Class<?> extensionType, String version) {
    this(new ExtensionTypeWrapper<>(extensionType, getDefault().createTypeLoader(extensionType.getClassLoader())), version);
    this.extensionType = extensionType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    ExtensionDeclarer declarer =
        context.getExtensionDeclarer()
            .named(extensionElement.getName())
            .onVersion(version)
            .fromVendor(extensionElement.getVendor())
            .withCategory(extensionElement.getCategory())
            .withModelProperty(new ExtensionTypeDescriptorModelProperty(extensionElement));

    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    context.getParameter("COMPILATION_MODE")
        .ifPresent(m -> declarer.withModelProperty(new CompileTimeModelProperty()));

    extensionElement.getDeclaringClass()
        .ifPresent(extensionClass -> declarer.withModelProperty(new ImplementingTypeModelProperty(extensionClass)));

    processLicenseRequirements(declarer);
    parseExternalLibs(extensionElement, declarer);
    addExceptionEnricher(extensionElement, declarer);

    configLoaderDelegate.declareConfigurations(declarer, extensionElement, context);
    connectionProviderModelLoaderDelegate.declareConnectionProviders(declarer, extensionElement);

    if (!isEmpty(extensionElement.getConfigurations())) {
      operationLoaderDelegate
          .declareOperations(declarer, declarer, null, extensionElement.getOperations(), false, context);

      functionModelLoaderDelegate
          .declareFunctions(declarer, declarer, null, extensionElement.getFunctions(), context);

      extensionElement.getSources()
          .forEach(source -> sourceModelLoaderDelegate.declareMessageSource(declarer, declarer, source, false, context));
    }

    return declarer;
  }

  private void processLicenseRequirements(ExtensionDeclarer declarer) {

    Optional<RequiresEntitlement> requiresEntitlementOptional = extensionElement.getAnnotation(RequiresEntitlement.class);
    Optional<RequiresEnterpriseLicense> requiresEnterpriseLicenseOptional =
        extensionElement.getAnnotation(RequiresEnterpriseLicense.class);
    boolean requiresEnterpriseLicense = requiresEnterpriseLicenseOptional.isPresent();
    boolean allowsEvaluationLicense =
        requiresEnterpriseLicenseOptional.map(RequiresEnterpriseLicense::allowEvaluationLicense).orElse(true);
    Optional<String> requiredEntitlement = requiresEntitlementOptional.map(RequiresEntitlement::name);

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
    List<ExtensionParameter> configParameter = element.getParametersAnnotatedWith(Config.class);
    configParameter.addAll(element.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Config.class));
    return configParameter.stream().findFirst();
  }

  Optional<ExtensionParameter> getConnectionParameter(WithParameters element) {
    List<ExtensionParameter> connectionParameter = element.getParametersAnnotatedWith(Connection.class);
    connectionParameter.addAll(element.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Connection.class));
    return connectionParameter.stream().findFirst();
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
    return extensionElement.getDeclaringClass().orElse(null);
  }

  ExtensionElement getExtensionElement() {
    return extensionElement;
  }

  protected ParameterModelsLoaderDelegate getFieldParametersLoader() {
    return fieldParametersLoader;
  }

  protected ParameterModelsLoaderDelegate getMethodParametersLoader() {
    return methodParametersLoader;
  }
}
