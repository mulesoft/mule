/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.reset;
import static org.mule.tck.MavenTestUtils.installArtifact;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DEPLOYMENT_TYPE;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DeploymentTypeStory.LIGHTWEIGHT;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(DEPLOYMENT_TYPE)
@Story(LIGHTWEIGHT)
public class LightweightDeployableProjectModelBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation",
                         discoverProvider(LightweightDeployableProjectModelBuilderTestCase.class
                             .getClassLoader()).getLocalRepositorySuppliers()
                                 .environmentMavenRepositorySupplier().get()
                                 .getAbsolutePath());

  @Rule
  public SystemProperty settingsLocation =
      new SystemProperty("muleRuntimeConfig.maven.userSettingsLocation",
                         discoverProvider(LightweightDeployableProjectModelBuilderTestCase.class
                             .getClassLoader()).getSettingsSupplierFactory()
                                 .environmentUserSettingsSupplier().get()
                                 .getAbsolutePath());

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    reset();
  }


  @Test
  @Issue("W-12142901")
  public void createDeployableProjectModelWithAdditionalDependenciesInAPlugin() throws Exception {
    installArtifact(getResourceFolder("apps/lightweight/application-using-additional-libraries"),
                    getMavenConfig().getLocalMavenRepositoryLocation());
    File artifact =
        new File(getMavenConfig().getLocalMavenRepositoryLocation(),
                 "org/mule/test/application-using-additional-libraries/1.0.0/");

    PluginFileMavenReactor pluginFileMavenReactor =
        new PluginFileMavenReactor(artifact, "org.mule.test", "application-using-additional-libraries", "1.0.0");

    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/lightweight/db-plugin-with-additional-dep", pluginFileMavenReactor);

    assertThat(deployableProjectModel.getAdditionalPluginDependencies().size(), is(1));
    List<BundleDependency> additionalBundleDependencies =
        deployableProjectModel.getAdditionalPluginDependencies().values().stream().findFirst().get();
    assertThat(additionalBundleDependencies.size(), is(1));
    org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor descriptor =
        additionalBundleDependencies.get(0).getDescriptor();
    assertThat(descriptor.getGroupId(), is("org.apache.derby"));
    assertThat(descriptor.getArtifactId(), is("derby"));
  }


  @Test
  @Issue("W-14998254")
  @Description("When pom has invalid GAV and no parent pom, then MuleRuntimeException will be the thrown.")
  public void createDeployableProjectModelWithInvalidGAVAndMissingParentPom() throws Exception {
    expectedException.expect(MuleRuntimeException.class);
    expectedException
        .expectMessage("Failed to retrieve version from the artifact, trying to retrieve from parent POM but parent POM is not present");
    getDeployableProjectModel("apps/lightweight/test-app-missing-gav", null);
  }


  @Test
  @Issue("W-14998254")
  public void createDeployableProjectModelWithGAVPresentInParentPom() throws Exception {

    installArtifact(getResourceFolder("apps/lightweight/parent-artifact-no-dependencies"),
                    getMavenConfig().getLocalMavenRepositoryLocation());
    File artifact =
        new File(getMavenConfig().getLocalMavenRepositoryLocation(),
                 "org/mule/test/parent-artifact-no-dependencies/1.0.0/");

    PluginFileMavenReactor pluginFileMavenReactor =
        new PluginFileMavenReactor(artifact, "org.mule.test", "parent-artifact-no-dependencies", "1.0.0");

    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/lightweight/test-app-missing-gav-valid-parent", pluginFileMavenReactor);
    assertThat(deployableProjectModel.getDescriptor().getVersion(), is("1.0.0"));
    assertThat(deployableProjectModel.getDescriptor().getGroupId(), is("org.mule.test"));
    assertThat(deployableProjectModel.getDescriptor().getArtifactId(), is("test-app-missing-gav-valid-parent"));
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath, MavenReactorResolver mavenReactorResolver)
      throws URISyntaxException {
    DeployableProjectModel model = new TestLightweightDeployableProjectModelBuilder(getResourceFolder(deployablePath),
                                                                                    false, mavenReactorResolver).build();

    model.validate();

    return model;
  }

  class TestLightweightDeployableProjectModelBuilder extends LightweightDeployableProjectModelBuilder {

    private final MavenReactorResolver mavenReactorResolver;

    public TestLightweightDeployableProjectModelBuilder(File projectFolder, boolean isDomain,
                                                        MavenReactorResolver mavenReactorResolver) {
      super(projectFolder, isDomain);
      this.mavenReactorResolver = mavenReactorResolver;
    }

    @Override
    protected Optional<MavenReactorResolver> getMavenReactorResolver() {
      return Optional.of(mavenReactorResolver);
    }
  }

  protected File getResourceFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

  class PluginFileMavenReactor implements MavenReactorResolver {

    private final File project;
    private final String groupId;
    private final String artifactId;
    private final String version;


    public PluginFileMavenReactor(File project, String groupId, String artifactId, String version) {
      this.project = project;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    @Override
    public File findArtifact(BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        if (bundleDescriptor.getType().equals("pom")) {
          return new File(project, artifactId + "-" + version + ".pom");
        } else {
          return new File(project, artifactId + "-" + version + "-mule-application.jar");
        }
      }
      return null;
    }

    private boolean checkArtifact(BundleDescriptor bundleDescriptor) {
      return bundleDescriptor.getGroupId().equals(groupId)
          && bundleDescriptor.getArtifactId().equals(artifactId)
          && bundleDescriptor.getVersion().equals(version);
    }

    @Override
    public List<String> findVersions(BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        return singletonList(version);
      }
      return emptyList();
    }
  }

}
