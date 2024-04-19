/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.config;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.tooling.metadata.MetadataMediator;
import org.mule.runtime.module.extension.api.tooling.sampledata.SampleDataProviderMediator;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;

import java.util.Map;
import java.util.Optional;

/**
 * Provides a way to create resolvers, configProviders and mediators to be used in design time.
 * 
 * @since 4.8
 */
public interface ExtensionDesignTimeResolversFactory {

  ConnectionProviderValueResolver createConnectionProviderResolver(ConnectionProviderModel connectionProviderModel,
                                                                   ComponentParameterization componentParameterization,
                                                                   PoolingProfile poolingProfile,
                                                                   ReconnectionConfig reconnectionConfig,
                                                                   ExtensionModel extensionModel,
                                                                   ConfigurationProperties configurationProperties,
                                                                   String parametersOwner,
                                                                   DslSyntaxResolver dslSyntaxResolver)
      throws MuleException;

  ConfigurationProvider createConfigurationProvider(ExtensionModel extensionModel,
                                                    ConfigurationModel configurationModel,
                                                    String configName,
                                                    Map<String, Object> parameters,
                                                    Optional<ExpirationPolicy> expirationPolicy,
                                                    Optional<ConnectionProviderValueResolver> connectionProviderResolver,
                                                    ConfigurationProviderFactory configurationProviderFactory,
                                                    String parametersOwner,
                                                    DslSyntaxResolver dslSyntaxResolver,
                                                    ClassLoader extensionClassLoader);

  ValueProviderMediator createValueProviderMediator(ParameterizedModel parameterizedModel);

  ParameterValueResolver createParameterValueResolver(ComponentParameterization<?> actingParameter,
                                                      ParameterizedModel parameterizedModel)
      throws MuleException;

  ResolverSet createParametersResolverSetFromValues(Map<String, ?> values, ParameterizedModel parameterizedModel)
      throws ConfigurationException;

  SampleDataProviderMediator createSampleDataProviderMediator(ExtensionModel extensionModel,
                                                              ComponentModel componentModel,
                                                              Component component,
                                                              StreamingManager streamingManager);

  <CM extends ComponentModel> MetadataMediator createMetadataMediator(CM componentModel);

}
