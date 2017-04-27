/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.module.extension.internal.loader.java.ParameterModelsLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.contributor.InfrastructureFieldContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterTypeUnwrapperContributor;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LiteralModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterResolverTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.TypedValueTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.soap.internal.loader.type.runtime.SoapServiceProviderWrapper;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Declares a Connection Provider of {@link ForwardingSoapClient} instances given a {@link SoapServiceProvider}.
 *
 * @since 4.0
 */
public class SoapServiceProviderDeclarer {

  private final ParameterModelsLoaderDelegate parametersLoader;

  SoapServiceProviderDeclarer(ClassTypeLoader loader) {
    parametersLoader = new ParameterModelsLoaderDelegate(getContributors(loader), loader);
  }

  /**
   * Declares a new connection provider for a configuration given a {@link SoapServiceProviderWrapper} declaration.
   *
   * @param configDeclarer the configuration declarer that will own the provider
   * @param provider       a {@link SoapServiceProviderWrapper} that describes the {@link SoapServiceProvider} Type.
   */
  public void declare(ConfigurationDeclarer configDeclarer, SoapServiceProviderWrapper provider) {
    String name = getName(provider);
    String description = provider.getDescription();

    // Declares the Service Provider as a Connection Provider.
    ConnectionProviderDeclarer providerDeclarer = configDeclarer.withConnectionProvider(name).describedAs(description)
        .withModelProperty(new ConnectionTypeModelProperty(ForwardingSoapClient.class))
        .withModelProperty(new ImplementingTypeModelProperty(provider.getDeclaringClass()))
        .withConnectionManagementType(POOLING);

    ParameterDeclarationContext context = new ParameterDeclarationContext("Service Provider", providerDeclarer.getDeclaration());
    parametersLoader.declare(providerDeclarer, provider.getParameters(), context);
  }

  private List<ParameterDeclarerContributor> getContributors(ClassTypeLoader loader) {
    return ImmutableList
        .of(new InfrastructureFieldContributor(),
            new ParameterTypeUnwrapperContributor(loader, TypedValue.class, new TypedValueTypeModelProperty()),
            new ParameterTypeUnwrapperContributor(loader, ParameterResolver.class, new ParameterResolverTypeModelProperty()),
            new ParameterTypeUnwrapperContributor(loader, Literal.class, new LiteralModelProperty()));
  }

  private String getName(SoapServiceProviderWrapper provider) {
    String finalName = hyphenize(provider.getAlias()).replace("-service-provider", "").replace("-connection", "");
    return finalName + "-" + DEFAULT_CONNECTION_PROVIDER_NAME;
  }
}
