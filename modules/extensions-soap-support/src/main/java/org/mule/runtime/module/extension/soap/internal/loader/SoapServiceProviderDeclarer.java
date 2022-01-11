/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.registerType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.TRANSPORT;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.TRANSPORT_GROUP;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.module.extension.internal.loader.delegate.ParameterModelsLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.StereotypeModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext;
import org.mule.runtime.module.extension.soap.internal.loader.type.runtime.SoapServiceProviderWrapper;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;

import java.util.function.Supplier;

/**
 * Declares a Connection Provider of {@link ForwardingSoapClient} instances given a {@link SoapServiceProvider}.
 *
 * @since 4.0
 */
public class SoapServiceProviderDeclarer {

  public static final String TRANSPORT_PARAM = TRANSPORT.toLowerCase();

  private final ParameterModelsLoaderDelegate parametersLoader;
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private final StereotypeModelLoaderDelegate stereotypeDelegate;

  SoapServiceProviderDeclarer(ExtensionDeclarer extensionDeclarer,
                              Supplier<StereotypeModelLoaderDelegate> stereotypeModelLoader,
                              StereotypeModelLoaderDelegate stereotypeDelegate) {
    parametersLoader = new ParameterModelsLoaderDelegate(stereotypeModelLoader, type -> registerType(extensionDeclarer, type));
    this.stereotypeDelegate = stereotypeDelegate;
  }

  /**
   * Declares a new connection provider for a configuration given a {@link SoapServiceProviderWrapper} declaration.
   *
   * @param configDeclarer      the configuration declarer that will own the provider
   * @param provider            a {@link SoapServiceProviderWrapper} that describes the {@link SoapServiceProvider} Type.
   * @param hasCustomTransports if declares custom transport or not.
   */
  public void declare(ConfigurationDeclarer configDeclarer, SoapServiceProviderWrapper provider, boolean hasCustomTransports) {
    String description = provider.getDescription();

    // Declares the Service Provider as a Connection Provider.
    final String providerName = provider.getAlias();
    ConnectionProviderDeclarer providerDeclarer = configDeclarer.withConnectionProvider(providerName)
        .describedAs(description)
        .withModelProperty(new ConnectionTypeModelProperty(ForwardingSoapClient.class))
        // TODO - MULE-14311 - Make loader work in compile time
        .withModelProperty(new ImplementingTypeModelProperty(provider.getDeclaringClass().get()))
        .withConnectionManagementType(POOLING)
        .supportsConnectivityTesting(provider.supportsConnectivityTesting())
        .withStereotype(stereotypeDelegate.getDefaultConnectionProviderStereotype(providerName));

    ParameterDeclarationContext context = new ParameterDeclarationContext("Service Provider", providerName);

    parametersLoader.declare(providerDeclarer, getParameterGroupParsers(provider.getParameters(), context));
    if (hasCustomTransports) {
      providerDeclarer.onParameterGroup(TRANSPORT_GROUP)
          .withRequiredParameter(TRANSPORT_PARAM)
          .withDisplayModel(DisplayModel.builder().displayName(TRANSPORT_GROUP).build())
          .ofType(typeLoader.load(MessageDispatcherProvider.class))
          .withLayout(LayoutModel.builder().order(1).tabName(TRANSPORT).build())
          .withExpressionSupport(NOT_SUPPORTED);
    }
  }
}
