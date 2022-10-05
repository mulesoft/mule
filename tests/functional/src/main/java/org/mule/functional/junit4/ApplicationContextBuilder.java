/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.mock;

import static org.mule.functional.junit4.FunctionalTestCase.extensionManagerWithMuleExtModelBuilder;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.tck.config.TestPolicyProviderConfigurationBuilder;
import org.mule.tck.config.TestServicesConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ApplicationContextBuilder {

  private String contextId;
  private ArtifactContext domainArtifactContext;
  private String[] applicationResources = new String[0];
  private ArtifactCoordinates artifactCoordinates;

  private TestServicesConfigurationBuilder testServicesConfigBuilder;

  private final MuleContextBuilder muleContextBuilder = MuleContextBuilder.builder(APP);

  public ApplicationContextBuilder setContextId(String contextId) {
    this.contextId = contextId;
    return this;
  }

  public ApplicationContextBuilder setDomainArtifactContext(ArtifactContext domainArtifactContext) {
    this.domainArtifactContext = domainArtifactContext;
    return this;
  }

  public ApplicationContextBuilder setApplicationResources(String... applicationResources) {
    this.applicationResources = applicationResources;
    return this;
  }

  /**
   * Set's the application's {@link ArtifactCoordinates}
   *
   * @param artifactCoordinates the app's {@link ArtifactCoordinates}
   * @return {@code this} builder
   * @since 4.5.0
   */
  public ApplicationContextBuilder setArtifactCoordinates(ArtifactCoordinates artifactCoordinates) {
    this.artifactCoordinates = artifactCoordinates;
    return this;
  }

  public MuleContext build() throws Exception {
    // Should we set up the manager for every method?
    MuleContext context = doBuildContext();
    context.start();
    return context;
  }

  protected MuleContext doBuildContext() throws Exception {
    MuleContext context;
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builders = new ArrayList<>(3);
    builders.add(extensionManagerWithMuleExtModelBuilder(getExtensionModels()));
    ConfigurationBuilder appBuilder = getAppBuilder(this.applicationResources);
    testServicesConfigBuilder = new TestServicesConfigurationBuilder();
    appBuilder.addServiceConfigurator(testServicesConfigBuilder);
    builders.add(appBuilder);
    builders.add(new TestPolicyProviderConfigurationBuilder());
    addBuilders(builders);
    final DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
    if (contextId != null) {
      muleConfiguration.setId(contextId);
    }
    muleContextBuilder.setMuleConfiguration(muleConfiguration);
    muleContextBuilder.setArtifactCoordinates(artifactCoordinates);
    configureMuleContext(muleContextBuilder);
    context = muleContextFactory.createMuleContext(builders, muleContextBuilder);
    return context;
  }

  protected Set<ExtensionModel> getExtensionModels() {
    return singleton(getExtensionModel());
  }

  // This shouldn't be needed by Test cases but can be used by base testcases that wish to add further builders when
  // creating the MuleContext.
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    // No op
  }

  protected ConfigurationBuilder getAppBuilder(String[] configResources) throws Exception {
    ArtifactAstXmlParserConfigurationBuilder appBuilder =
        new ArtifactAstXmlParserConfigurationBuilder(emptyMap(), false, false, false, configResources, mock(ExpressionLanguageMetadataService.class));
    appBuilder.setParentArtifactContext(domainArtifactContext);
    return appBuilder;
  }

  /**
   * Override this method to set properties of the MuleContextBuilder before it is used to create the MuleContext.
   */
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {}
}
