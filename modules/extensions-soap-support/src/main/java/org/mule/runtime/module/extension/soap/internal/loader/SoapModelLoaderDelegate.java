/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionTypeFactory.getSoapExtensionType;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.soap.internal.loader.property.SoapExtensionModelProperty;
import org.mule.runtime.module.extension.soap.internal.loader.type.runtime.SoapExtensionTypeWrapper;

/**
 * Describes a Soap Based {@link ExtensionModel} based on a set of java classes and annotations.
 *
 * @since 4.0
 */
final class SoapModelLoaderDelegate implements ModelLoaderDelegate {

  private final InvokeOperationDeclarer operationDeclarer;
  private final ServiceProviderDeclarer serviceProviderDeclarer;
  private final Class<?> extensionType;
  private final String version;
  private final ClassTypeLoader typeLoader;

  SoapModelLoaderDelegate(Class<?> extensionType, String version) {
    this.extensionType = extensionType;
    this.version = version;
    this.typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    this.serviceProviderDeclarer = new ServiceProviderDeclarer(typeLoader);
    this.operationDeclarer = new InvokeOperationDeclarer();
  }

  /**
   * {@inheritDoc}
   */
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    final SoapExtensionTypeWrapper<?> extension = getSoapExtensionType(this.extensionType);
    ExtensionDeclarer extensionDeclarer = getExtensionDeclarer(context);
    ConfigurationDeclarer configDeclarer = getConfigDeclarer(extensionDeclarer, extension);
    extension.getSoapServiceProviders().forEach(provider -> serviceProviderDeclarer.declare(configDeclarer, provider));
    return extensionDeclarer;
  }

  private ExtensionDeclarer getExtensionDeclarer(ExtensionLoadingContext context) {
    Extension extension = MuleExtensionAnnotationParser.getExtension(extensionType);
    return context.getExtensionDeclarer()
        .named(extension.name())
        .onVersion(version)
        .fromVendor(extension.vendor())
        .withCategory(extension.category())
        .withMinMuleVersion(new MuleVersion(extension.minMuleVersion()))
        .describedAs(extension.description())
        .withModelProperty(new SoapExtensionModelProperty())
        .withModelProperty(new ImplementingTypeModelProperty(extensionType));
  }

  private ConfigurationDeclarer getConfigDeclarer(ExtensionDeclarer declarer, SoapExtensionTypeWrapper<?> extension) {
    Class<?> clazz = extension.getDeclaringClass();
    TypeAwareConfigurationFactory configurationFactory = new TypeAwareConfigurationFactory(clazz, clazz.getClassLoader());

    ConfigurationDeclarer configDeclarer = declarer.withConfig(DEFAULT_CONFIG_NAME)
        .describedAs(DEFAULT_CONFIG_DESCRIPTION)
        .withModelProperty(new ConfigurationFactoryModelProperty(configurationFactory))
        .withModelProperty(new ImplementingTypeModelProperty(clazz));

    OperationDeclarer operation = operationDeclarer.declare(declarer, typeLoader);
    configDeclarer.withOperation(operation);

    return configDeclarer;
  }
}
