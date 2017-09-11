/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionTypeFactory.getSoapExtensionType;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.module.extension.internal.loader.enricher.ErrorsModelFactory;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.soap.internal.loader.property.SoapExtensionModelProperty;
import org.mule.runtime.module.extension.soap.internal.loader.type.runtime.MessageDispatcherProviderTypeWrapper;
import org.mule.runtime.module.extension.soap.internal.loader.type.runtime.SoapExtensionTypeWrapper;
import org.mule.runtime.soap.api.exception.error.SoapErrors;
import java.util.List;
import java.util.Set;

/**
 * Describes a Soap Based {@link ExtensionModel} based on a set of java classes and annotations.
 *
 * @since 4.0
 */
public final class SoapModelLoaderDelegate implements ModelLoaderDelegate {

  private final SoapInvokeOperationDeclarer operationDeclarer;
  private final SoapServiceProviderDeclarer serviceProviderDeclarer;
  private final Class<?> extensionType;
  private final String version;
  private final ClassTypeLoader typeLoader;

  public SoapModelLoaderDelegate(Class<?> extensionType, String version) {
    this.extensionType = extensionType;
    this.version = version;
    this.typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    this.serviceProviderDeclarer = new SoapServiceProviderDeclarer(typeLoader);
    this.operationDeclarer = new SoapInvokeOperationDeclarer();
  }

  /**
   * {@inheritDoc}
   */
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    final SoapExtensionTypeWrapper<?> extension = getSoapExtensionType(this.extensionType);
    List<MessageDispatcherProviderTypeWrapper> customTransportProviders = extension.getDispatcherProviders();
    ExtensionDeclarer extensionDeclarer = getExtensionDeclarer(context);
    declareSubtypes(extensionDeclarer, customTransportProviders);
    Set<ErrorModel> soapErrors = getSoapErrors(extensionDeclarer);
    soapErrors.forEach(extensionDeclarer::withErrorModel);
    ConfigurationDeclarer configDeclarer = getConfigDeclarer(extensionDeclarer, extension, soapErrors);
    extension.getSoapServiceProviders()
        .forEach(provider -> serviceProviderDeclarer.declare(configDeclarer, provider, !customTransportProviders.isEmpty()));
    return extensionDeclarer;
  }

  private void declareSubtypes(ExtensionDeclarer extension, List<MessageDispatcherProviderTypeWrapper> transportProviders) {
    if (!transportProviders.isEmpty()) {
      List<MetadataType> types = transportProviders.stream().map(tp -> typeLoader.load(tp.getDeclaringClass())).collect(toList());
      extension.withSubTypes(typeLoader.load(MessageDispatcherProvider.class), types);
    }
  }

  private ExtensionDeclarer getExtensionDeclarer(ExtensionLoadingContext context) {
    Extension extension = MuleExtensionAnnotationParser.getExtension(extensionType);
    return context.getExtensionDeclarer()
        .named(extension.name())
        .onVersion(version)
        .fromVendor(extension.vendor())
        .withCategory(extension.category())
        .withModelProperty(new SoapExtensionModelProperty())
        .withModelProperty(new ImplementingTypeModelProperty(extensionType));
  }

  private ConfigurationDeclarer getConfigDeclarer(ExtensionDeclarer declarer,
                                                  SoapExtensionTypeWrapper<?> extension,
                                                  Set<ErrorModel> soapErrors) {
    Class<?> clazz = extension.getDeclaringClass();
    TypeAwareConfigurationFactory configurationFactory = new TypeAwareConfigurationFactory(clazz, clazz.getClassLoader());

    ConfigurationDeclarer configDeclarer = declarer.withConfig(DEFAULT_CONFIG_NAME)
        .describedAs(DEFAULT_CONFIG_DESCRIPTION)
        .withModelProperty(new ConfigurationFactoryModelProperty(configurationFactory))
        .withModelProperty(new ImplementingTypeModelProperty(clazz));

    operationDeclarer.declare(configDeclarer, typeLoader, soapErrors);
    return configDeclarer;
  }

  private Set<ErrorModel> getSoapErrors(ExtensionDeclarer declarer) {
    ErrorsModelFactory factory = new ErrorsModelFactory(SoapErrors.class.getEnumConstants(), declarer.getDeclaration().getName());
    return factory.getErrorModels();
  }
}
