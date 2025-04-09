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

import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.Assert.assertThrows;

import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

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

  @Before
  public void before() {
    reset();
  }


  @Test
  @Issue("W-12142901")
  public void createDeployableProjectModelWithAdditionalDependenciesInAPlugin() throws Exception {
    final File artifactFolder = getResourceFolder("apps/lightweight/application-using-additional-libraries");
    final Collection<File> listFiles = listFiles(artifactFolder, nameFileFilter("pom.xml"), TrueFileFilter.INSTANCE);
    listFiles.forEach(pom -> {
      try {
        String pomContent = readFileToString(pom, UTF_8);
        pomContent = pomContent.replaceAll("\\$\\{mule\\.maven\\.plugin\\.version}", getProperty("mule.maven.plugin.version"));
        pomContent = pomContent.replaceAll("\\$\\{derbyVersion}", getProperty("derbyVersion"));
        pomContent = pomContent.replaceAll("\\$\\{mule\\.db\\.connector\\.version}", getProperty("mule.db.connector.version"));

        FileUtils.writeStringToFile(pom, pomContent, UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    installArtifact(artifactFolder,
                    getMavenConfig().getLocalMavenRepositoryLocation());
    File artifact =
        new File(getMavenConfig().getLocalMavenRepositoryLocation(),
                 "org/mule/test/application-using-additional-libraries/1.0.0/");

    PluginFileMavenReactor pluginFileMavenReactor =
        new PluginFileMavenReactor(artifact, "org.mule.test", "application-using-additional-libraries", "1.0.0");

    DeployableProjectModel deployableProjectModel =
        getDeployableProjectModel("apps/lightweight/db-plugin-with-additional-dep", pluginFileMavenReactor);

    assertThat(deployableProjectModel.getAdditionalPluginDependencies(), aMapWithSize(1));
    List<BundleDependency> additionalBundleDependencies =
        deployableProjectModel.getAdditionalPluginDependencies().values().stream().findFirst().get();
    assertThat(additionalBundleDependencies, iterableWithSize(2));

    org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor descriptor =
        additionalBundleDependencies.get(0).getDescriptor();
    assertThat(descriptor.getGroupId(), is("org.apache.derby"));
    assertThat(descriptor.getArtifactId(), is("derby"));

    org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor derbySharedDescriptor =
        additionalBundleDependencies.get(1).getDescriptor();
    assertThat(derbySharedDescriptor.getGroupId(), is("org.apache.derby"));
    assertThat(derbySharedDescriptor.getArtifactId(), is("derbyshared"));
  }


  @Test
  @Issue("W-14998254")
  @Description("When pom has invalid GAV and no parent pom, then MuleRuntimeException will be the thrown.")
  public void createDeployableProjectModelWithInvalidGAVAndMissingParentPom() throws Exception {
    final var thrownExcpetion =
        assertThrows(MuleRuntimeException.class, () -> getDeployableProjectModel("apps/lightweight/test-app-missing-gav", null));
    assertThat(thrownExcpetion.getMessage(),
               is("Failed to retrieve version from the artifact, trying to retrieve from parent POM but parent POM is not present"));
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
                                                                                    false, mavenReactorResolver)
        .build();

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
