/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.CLASS_LOADER_REPOSITORY_WAS_NOT_SET;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.EXECUTION_CLASSLOADER_WAS_NOT_SET;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.ONLY_APPLICATIONS_OR_POLICIES_ARE_ALLOWED_TO_HAVE_A_PARENT_ARTIFACT;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.SERVICE_CONFIGURATOR_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.SERVICE_REPOSITORY_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import static org.mule.runtime.module.deployment.impl.internal.config.DeploymentTestingFeatures.ALWAYS_ON_FEATURE;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import static java.lang.Thread.currentThread;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

public class ArtifactContextBuilderTestCase extends AbstractMuleTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() {
    // Ensure that the testing feature flags are registered.
    ALWAYS_ON_FEATURE.getClass();
  }

  @Test
  public void emptyBuilder() throws Exception {
    ArtifactConfigurationProcessor artifactConfigurationProcessor = mock(ArtifactConfigurationProcessor.class);
    when(artifactConfigurationProcessor.createArtifactContext(any()))
        .thenAnswer(inv -> {
          ArtifactContextConfiguration artifactContextConfiguration = inv.getArgument(0, ArtifactContextConfiguration.class);

          ArtifactAstConfigurationBuilder configurationBuilder =
              new ArtifactAstConfigurationBuilder(emptyArtifact(),
                                                  artifactContextConfiguration.getArtifactProperties(),
                                                  artifactContextConfiguration.getArtifactType(),
                                                  artifactContextConfiguration.isEnableLazyInitialization());

          artifactContextConfiguration.getServiceConfigurators().stream()
              .forEach(configurationBuilder::addServiceConfigurator);
          configurationBuilder.configure(artifactContextConfiguration.getMuleContext());

          return configurationBuilder.createArtifactContext();
        });

    MuleContext muleContext =
        newBuilder(new TestServicesConfigurationBuilder())
            .setExecutionClassloader(currentThread().getContextClassLoader())
            .setClassLoaderRepository(mock(ClassLoaderRepository.class))
            .setArtifactConfigurationProcessor(artifactConfigurationProcessor)
            .build().getMuleContext();
    assertThat(muleContext, notNullValue());
    assertThat(muleContext.isInitialised(), is(true));

    try {
      muleContext.start();
      assertThat(muleContext.isStarted(), is(true));
    } finally {
      muleContext.stop();
      muleContext.dispose();
    }
  }

  @Test
  public void buildWithoutClassloader() throws Exception {
    expectedException.expectMessage(EXECUTION_CLASSLOADER_WAS_NOT_SET);
    newBuilder().build();
  }

  @Test
  public void setNullArtifactProperties() {
    expectedException.expectMessage(MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL);
    newBuilder().setArtifactProperties(null);
  }

  @Test
  public void setNullClassLoaderRepository() {
    expectedException.expectMessage(CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL);
    newBuilder().setClassLoaderRepository(null);
  }

  @Test
  public void setNullServiceConfigurator() {
    expectedException.expectMessage(SERVICE_CONFIGURATOR_CANNOT_BE_NULL);
    newBuilder().withServiceConfigurator(null);
  }

  @Test
  public void buildWithoutClassloaderRepository() throws Exception {
    expectedException.expectMessage(CLASS_LOADER_REPOSITORY_WAS_NOT_SET);
    newBuilder().setExecutionClassloader(currentThread().getContextClassLoader()).build();
  }

  @Test
  public void setRegularFileInstallationLocation() throws Exception {
    expectedException.expectMessage(INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY);
    newBuilder().setArtifactInstallationDirectory(temporaryFolder.newFile());
  }

  @Test
  public void buildUsingDomainAndParentArtifact() throws Exception {
    expectedException.expectMessage(ONLY_APPLICATIONS_OR_POLICIES_ARE_ALLOWED_TO_HAVE_A_PARENT_ARTIFACT);
    newBuilder().setArtifactType(DOMAIN)
        .setExecutionClassloader(currentThread().getContextClassLoader()).setParentArtifact(mock(DeployableArtifact.class))
        .setClassLoaderRepository(mock(ClassLoaderRepository.class))
        .build();
  }

  @Test
  public void setNullServiceRepository() {
    expectedException.expectMessage(SERVICE_REPOSITORY_CANNOT_BE_NULL);
    newBuilder().setServiceRepository(null);
  }

  @Test
  @Story(FEATURE_FLAGGING)
  @Issue("MULE-19402")
  public void buildSettingLegacyFeatureFlag() throws Exception {
    ArtifactConfigurationProcessor artifactConfigurationProcessor = mock(ArtifactConfigurationProcessor.class);
    when(artifactConfigurationProcessor.createArtifactContext(any()))
        .thenAnswer(inv -> {
          ArtifactContextConfiguration configBuilder = inv.getArgument(0, ArtifactContextConfiguration.class);

          ArtifactContext artifactContext = mock(ArtifactContext.class);
          when(artifactContext.getRegistry())
              .thenReturn(((MuleContextWithRegistry) configBuilder.getMuleContext()).getRegistry().get(OBJECT_REGISTRY));
          return artifactContext;
        });

    ArtifactContext artifactContext = newBuilder(new TestServicesConfigurationBuilder())
        .setExecutionClassloader(currentThread().getContextClassLoader())
        .setClassLoaderRepository(mock(ClassLoaderRepository.class))
        .setArtifactConfigurationProcessor(artifactConfigurationProcessor)
        .build();
    FeatureFlaggingService featureFlaggingService = (FeatureFlaggingService) artifactContext.getRegistry()
        .lookupByName(FEATURE_FLAGGING_SERVICE_KEY).get();
    assertThat(featureFlaggingService.isEnabled(ALWAYS_ON_FEATURE), is(true));
  }

}
