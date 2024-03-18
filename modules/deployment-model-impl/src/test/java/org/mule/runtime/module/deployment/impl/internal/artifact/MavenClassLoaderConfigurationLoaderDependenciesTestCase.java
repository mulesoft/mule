/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.module.deployment.impl.internal.BundleDependencyMatcher.bundleDependency;
import static org.mule.runtime.module.deployment.impl.internal.artifact.MavenClassLoaderConfigurationLoaderConfigurationTestCase.MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION;
import static org.mule.tck.MavenTestUtils.installArtifact;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION_LOADER;

import static com.google.common.io.Files.createTempDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.IsNot.not;

import org.mule.maven.pom.parser.api.model.MavenPomModel;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Stories({@Story(CLASSLOADER_CONFIGURATION_LOADER), @Story(CLASSLOADER_CONFIGURATION)})
public class MavenClassLoaderConfigurationLoaderDependenciesTestCase extends MavenClassLoaderConfigurationLoaderTestCase {

  @ClassRule
  public static SystemProperty repositoryLocation = new SystemProperty(MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION,
                                                                       createTempDir().getAbsolutePath());

  @BeforeClass
  public static void setUp() throws Exception {
    GlobalConfigLoader.reset();

    // Install all dependencies
    File dependenciesFolder =
        new File(MavenClassLoaderConfigurationLoaderDependenciesTestCase.class.getClassLoader().getResource("dependencies")
            .toURI());
    for (File dependencyFile : dependenciesFolder.listFiles()) {
      installArtifact(dependencyFile, new File(repositoryLocation.getValue()));
    }
  }

  @Test
  public void allApiDependenciesAreAddedRAML() throws Exception {
    File artifactFile = getApplicationFolder("apps/raml-api-app");
    ClassLoaderConfiguration classLoaderConfiguration = loadClassLoaderConfiguration(artifactFile);
    assertThat(classLoaderConfiguration.getDependencies(), hasItems(
                                                                    bundleDependency("raml-api-a"),
                                                                    bundleDependency("raml-api-b"),
                                                                    bundleDependency("raml-fragment", "1.0.0"),
                                                                    bundleDependency("raml-fragment", "2.0.0")));
  }

  @Test
  public void allApiDependenciesAreAddedWSDL() throws Exception {
    File artifactFile = getApplicationFolder("apps/wsdl-api-app");
    ClassLoaderConfiguration classLoaderConfiguration = loadClassLoaderConfiguration(artifactFile);
    assertThat(classLoaderConfiguration.getDependencies(), hasItems(
                                                                    bundleDependency("wsdl-api-a"),
                                                                    bundleDependency("wsdl-api-b"),
                                                                    bundleDependency("wsdl-fragment", "1.0.0"),
                                                                    bundleDependency("wsdl-fragment", "2.0.0")));
  }

  @Test
  public void allApiDependenciesAreAddedOAS() throws Exception {
    File artifactFile = getApplicationFolder("apps/oas-api-app");
    ClassLoaderConfiguration classLoaderConfiguration = loadClassLoaderConfiguration(artifactFile);
    assertThat(classLoaderConfiguration.getDependencies(), hasItems(
                                                                    bundleDependency("oas-api-a"),
                                                                    bundleDependency("oas-api-b"),
                                                                    bundleDependency("oas-fragment", "1.0.0"),
                                                                    bundleDependency("oas-fragment", "2.0.0")));
  }

  @Test
  public void apiDependsOnLibraryThatDependsOnApiThatDependsOnApi() throws Exception {
    File artifactFile = getApplicationFolder("apps/api-multiple-levels-app");
    ClassLoaderConfiguration classLoaderConfiguration = loadClassLoaderConfiguration(artifactFile);
    assertThat(classLoaderConfiguration.getDependencies(), hasItems(
                                                                    bundleDependency("raml-api-a"),
                                                                    bundleDependency("library-depends-on-api"),
                                                                    bundleDependency("api-depends-on-library"),
                                                                    bundleDependency("raml-fragment", "1.0.0"),
                                                                    bundleDependency("raml-fragment", "2.0.0")));
  }


  @Test
  public void apiTransitiveDependenciesDontOverrideMavenResolved() throws Exception {
    File artifactFile = getApplicationFolder("apps/api-app");
    ClassLoaderConfiguration classLoaderConfiguration = loadClassLoaderConfiguration(artifactFile);
    assertThat(classLoaderConfiguration.getDependencies(), hasItems(
                                                                    bundleDependency("wsdl-api-a"),
                                                                    bundleDependency("wsdl-api-b"),
                                                                    bundleDependency("wsdl-fragment", "1.0.0"),
                                                                    bundleDependency("wsdl-fragment", "2.0.0"),
                                                                    bundleDependency("library", "1.0.0")));
    assertThat(classLoaderConfiguration.getDependencies(), not(hasItem(
                                                                       bundleDependency("library", "2.0.0"))));
  }

  private ClassLoaderConfiguration loadClassLoaderConfiguration(File artifactFile) throws InvalidDescriptorLoaderException {
    MavenPomModel model = discoverProvider().createMavenPomParserClient(artifactFile.toPath()).getModel();
    Map<String, Object> attributes =
        ImmutableMap.of(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.class.getName(),
                        new org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder()
                            .setGroupId(model.getGroupId())
                            .setArtifactId(model.getArtifactId())
                            .setVersion(model.getVersion())
                            .setType("jar")
                            .setClassifier("mule-application")
                            .build());
    return mavenClassLoaderConfigurationLoader.load(artifactFile, attributes, APP);
  }
}
