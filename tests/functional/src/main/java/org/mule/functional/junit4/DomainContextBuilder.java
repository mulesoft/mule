/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.mule.functional.junit4.FunctionalTestCase.extensionManagerWithMuleExtModelBuilder;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.createDefaultExtensionManager;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.tck.config.TestPolicyProviderConfigurationBuilder;
import org.mule.tck.config.TestServicesConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DomainContextBuilder {

  private String contextId;
  private String[] domainConfig = new String[0];

  private TestServicesConfigurationBuilder testServicesConfigBuilder;

  private final MuleContextBuilder muleContextBuilder = MuleContextBuilder.builder(DOMAIN);

  public DomainContextBuilder setContextId(String contextId) {
    this.contextId = contextId;
    return this;
  }

  public DomainContextBuilder setDomainConfig(String... domainConfig) {
    this.domainConfig = domainConfig;
    return this;
  }

  public ArtifactContext build() throws Exception {
    List<ConfigurationBuilder> builders = new ArrayList<>(3);
    ConfigurationBuilder cfgBuilder = getDomainBuilder(domainConfig);
    builders.add(extensionManagerWithMuleExtModelBuilder(getExtensionModels()));
    builders.add(new TestPolicyProviderConfigurationBuilder());
    builders.add(cfgBuilder);
    testServicesConfigBuilder = new TestServicesConfigurationBuilder();
    addBuilders(builders);
    final DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
    if (contextId != null) {
      muleConfiguration.setId(contextId);
    }
    muleContextBuilder.setMuleConfiguration(muleConfiguration);
    DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    MuleContext domainContext = muleContextFactory.createMuleContext(builders, muleContextBuilder);
    domainContext.start();

    MuleContextNotificationListener listener = notification -> {
      try {
        testServicesConfigBuilder.stopServices();
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    };
    domainContext.getNotificationManager().addListener(listener);

    return ((ArtifactContextFactory) cfgBuilder).createArtifactContext();
  }

  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(testServicesConfigBuilder);
  }

  private ConfigurationBuilder getDomainBuilder(String[] configResources) throws Exception {
    return createConfigurationBuilder(configResources, emptyMap(), DOMAIN, false, false);
  }

  protected Set<ExtensionModel> getExtensionModels() {
    return singleton(getExtensionModel());
  }
}
