/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.reset;
import static org.mule.runtime.module.artifact.activation.internal.MavenTestUtils.installArtifact;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
@Issue("W-12069164")
public class LightweightDeployableProjectModelBuilderTestCase extends AbstractMuleTestCase {


  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation",
                         discoverProvider(LightweightDeployableProjectModelBuilderTestCase.class
                             .getClassLoader()).getLocalRepositorySuppliers()
                                 .environmentMavenRepositorySupplier().get()
                                 .getAbsolutePath());

  @Before
  public void before() {
    reset();
  }

  @Test
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

    assertThat(deployableProjectModel.getDependencies(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("plugin-with-transitive-dependency")))));
    assertThat(deployableProjectModel.getDependencies().get(0).getTransitiveDependenciesList(),
               contains(hasProperty("descriptor", hasProperty("artifactId", equalTo("library")))));
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
