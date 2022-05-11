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
import static org.mule.runtime.extension.internal.util.ExtensionNamespaceUtils.getExtensionsNamespace;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.getExtensionInfo;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getXmlDslModel;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionTypeFactory.getSoapExtensionType;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.error.ErrorsModelFactory;
import org.mule.runtime.module.extension.internal.loader.delegate.StereotypeModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.info.ExtensionInfo;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
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

  private final Class<?> extensionType;
  private final ExtensionElement extensionElement;
  private final String version;
  private final ClassTypeLoader typeLoader;
  private SoapServiceProviderDeclarer serviceProviderDeclarer;
  private SoapInvokeOperationDeclarer operationDeclarer;
  private StereotypeModelLoaderDelegate stereotypeDelegate;

  public SoapModelLoaderDelegate(ExtensionElement extensionElement, String version) {
    this.extensionType = extensionElement.getDeclaringClass().get();
    this.extensionElement = extensionElement;
    this.version = version;
    this.typeLoader = new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionType.getClassLoader());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    JavaExtensionModelParser parser = new JavaExtensionModelParser(extensionElement, context);
    ExtensionDeclarer extensionDeclarer = getExtensionDeclarer(context);
    XmlDslModel xmlDslModel = getXmlDslModel(extensionElement, version, parser.getXmlDslConfiguration());
    extensionDeclarer.withXmlDsl(xmlDslModel);
    stereotypeDelegate = new StereotypeModelLoaderDelegate(context);
    stereotypeDelegate.setNamespace(getExtensionsNamespace(xmlDslModel));
    operationDeclarer = new SoapInvokeOperationDeclarer(stereotypeDelegate);
    serviceProviderDeclarer =
        new SoapServiceProviderDeclarer(extensionDeclarer, parser::getStereotypeLoaderDelegate, stereotypeDelegate);

    final SoapExtensionTypeWrapper<?> extension = getSoapExtensionType(this.extensionType, typeLoader);
    List<MessageDispatcherProviderTypeWrapper> customTransportProviders = extension.getDispatcherProviders();
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
      List<MetadataType> types = transportProviders.stream().map(TypeWrapper::asMetadataType).collect(toList());
      extension.withSubTypes(typeLoader.load(MessageDispatcherProvider.class), types);
    }
  }

  private ExtensionDeclarer getExtensionDeclarer(ExtensionLoadingContext context) {
    ExtensionInfo info = getExtensionInfo(extensionType);
    return context.getExtensionDeclarer()
        .named(info.getName())
        .onVersion(version)
        .fromVendor(info.getVendor())
        .withCategory(info.getCategory())
        .withModelProperty(new SoapExtensionModelProperty())
        .withModelProperty(new ExtensionTypeDescriptorModelProperty(new TypeWrapper(extensionType, typeLoader)))
        .withModelProperty(new ImplementingTypeModelProperty(extensionType));
  }

  private ConfigurationDeclarer getConfigDeclarer(ExtensionDeclarer declarer,
                                                  SoapExtensionTypeWrapper<?> extension,
                                                  Set<ErrorModel> soapErrors) {
    // TODO - MULE-14311 - Make loader work in compile time
    Class<?> clazz = extension.getDeclaringClass().get();
    TypeAwareConfigurationFactory configurationFactory = new TypeAwareConfigurationFactory(clazz, clazz.getClassLoader());

    ConfigurationDeclarer configDeclarer = declarer.withConfig(DEFAULT_CONFIG_NAME)
        .describedAs(DEFAULT_CONFIG_DESCRIPTION)
        .withStereotype(stereotypeDelegate.getDefaultConfigStereotype(DEFAULT_CONFIG_NAME))
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
