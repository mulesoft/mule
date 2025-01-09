/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.api;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.test.runner.api.ArtifactClassificationType.APPLICATION;
import static org.mule.test.runner.utils.RunnerModuleUtils.RUNNER_PROPERTIES_MULE_VERSION;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.api.model.RemoteRepository;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Supplier;

import io.qameta.allure.Issue;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.junit.Test;

@Issue("W-17464124")
public class AetherClassPathContainerClassifierTestCase {

  public static final String MAVEN_CENTRAL_REPOSITORY_ID = "maven central";
  public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2/";
  public static final String MULESOFT_PRIVATE_REPOSITORY_ID = "mulesoft-private";
  public static final String MULESOF_PRIVATE_REPOSITORY_URL =
      "https://repository-master.mulesoft.org/nexus/content/repositories/private/";
  public static final String GROUP_ID = "org.mule.runtime";
  public static final String ARTIFACT_ID = "mule-api";

  public static final String VERSION = "1.8.0";
  public static final String EXTENSION = "jar";
  public static final String ORG_MULE_RUNTIME_MULE_API_URL_PATH = "org/mule/runtime/mule-api/";

  @Test
  public void testUrlClassification() throws Exception {
    // We create the AetherClassPathClassifier
    DependencyResolver dependencyResolver = getDependencyResolver();
    ArtifactClassificationTypeResolver artifactClassificationTypeResolver = mock(ArtifactClassificationTypeResolver.class);
    AetherClassPathClassifier aetherClassPathClassifier =
        new AetherClassPathClassifier(dependencyResolver, artifactClassificationTypeResolver);

    // We create a mocked root artifact
    ClassPathClassifierContext context = mock(ClassPathClassifierContext.class);
    Artifact rootArtifact = getMockedRootArtifact();
    when(context.getRootArtifact()).thenReturn(rootArtifact);
    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact)).thenReturn(APPLICATION);

    // We classify the urls
    ArtifactsUrlClassification artifactsUrlClassification = aetherClassPathClassifier.classify(context);

    // We verify that mule-api is in the container mule api urls
    assertThat(artifactsUrlClassification.getContainerMuleApisUrls().stream()
        .anyMatch(url -> url.getPath().contains(ORG_MULE_RUNTIME_MULE_API_URL_PATH)), equalTo(TRUE));
    // We verify that mule-api is in not in the container opt api urls
    assertThat(artifactsUrlClassification.getContainerOptUrls().stream()
        .noneMatch(url -> url.getPath().contains(ORG_MULE_RUNTIME_MULE_API_URL_PATH)), equalTo(TRUE));
  }

  private DependencyResolver getDependencyResolver() {
    MavenConfiguration mavenConfiguration = getDefaultMavenConfiguration();
    WorkspaceReader workspaceReader = mock(WorkspaceReader.class);
    DependencyResolver dependencyResolver =
        new TestDependencyResolver(mavenConfiguration, of(workspaceReader), RUNNER_PROPERTIES_MULE_VERSION);
    return dependencyResolver;
  }

  /**
   * @return a mocked root artifact. It returns mule-api to make sure that it is present.
   */
  private static Artifact getMockedRootArtifact() {
    Artifact rootArtifact = mock(Artifact.class);
    when(rootArtifact.getClassifier()).thenReturn("");
    when(rootArtifact.getGroupId()).thenReturn(GROUP_ID);
    when(rootArtifact.getArtifactId()).thenReturn(ARTIFACT_ID);
    when(rootArtifact.getVersion()).thenReturn(VERSION);
    when(rootArtifact.getExtension()).thenReturn(EXTENSION);
    when(rootArtifact.setVersion(any())).thenReturn(rootArtifact);
    return rootArtifact;
  }

  private MavenConfiguration getDefaultMavenConfiguration() {
    final MavenClientProvider mavenClientProvider =
        discoverProvider(DeployableProjectModelBuilder.class.getClassLoader());
    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final SettingsSupplierFactory settingsSupplierFactory = mavenClientProvider.getSettingsSupplierFactory();

    final Optional<File> globalSettings = settingsSupplierFactory.environmentGlobalSettingsSupplier();
    final Optional<File> userSettings = settingsSupplierFactory.environmentUserSettingsSupplier();
    final Optional<File> settingsSecurity = settingsSupplierFactory.environmentSettingsSecuritySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder = newMavenConfigurationBuilder()
        .localMavenRepositoryLocation(localMavenRepository.get())
        .forcePolicyUpdateAlways(true);

    globalSettings.ifPresent(mavenConfigurationBuilder::globalSettingsLocation);

    userSettings.ifPresent(mavenConfigurationBuilder::userSettingsLocation);

    settingsSecurity.ifPresent(mavenConfigurationBuilder::settingsSecurityLocation);

    try {
      mavenConfigurationBuilder
          .remoteRepository(RemoteRepository.newRemoteRepositoryBuilder()
              .id(MAVEN_CENTRAL_REPOSITORY_ID)
              .url(new URL(MAVEN_CENTRAL_URL))
              .build());
      mavenConfigurationBuilder
          .remoteRepository(RemoteRepository.newRemoteRepositoryBuilder()
              .id(MULESOFT_PRIVATE_REPOSITORY_ID)
              .url(new URL(MULESOF_PRIVATE_REPOSITORY_URL))
              .build());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    mavenConfigurationBuilder.forcePolicyUpdateAlways(true).forcePolicyUpdateNever(false);
    return mavenConfigurationBuilder.build();
  }

  private static class TestDependencyResolver extends DependencyResolver {

    private final String implementationVersion;

    public TestDependencyResolver(MavenConfiguration mavenConfiguration, Optional<WorkspaceReader> workspaceReader,
                                  String implementationVersion) {
      super(mavenConfiguration, workspaceReader);
      this.implementationVersion = implementationVersion;
    }

    @Override
    protected String getImplementationVersion() {
      return implementationVersion;
    }
  }
}
