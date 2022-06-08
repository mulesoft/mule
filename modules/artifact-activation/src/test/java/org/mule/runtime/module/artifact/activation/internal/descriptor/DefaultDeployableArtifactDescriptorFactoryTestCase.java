/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolver;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class DefaultDeployableArtifactDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static final DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory =
      new DefaultDeployableArtifactDescriptorFactory();
  private static final ArtifactClassLoaderResolver artifactClassLoaderResolver =
      ArtifactClassLoaderResolver.defaultClassLoaderResolver();

  @Test
  public void createBasicApplicationDescriptor() throws Exception {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/basic");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), contains("org.test"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(), hasSize(3));
  }

  @Test
  public void createApplicationDescriptorWithSharedLibrary() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/shared-lib");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), everyItem(startsWith("org.apache.derby")));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(), hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createApplicationDescriptorWithAdditionalPluginDependency() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/additional-plugin-dependency");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(), hasSize(4));

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(),
               hasItems(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-http-connector")))));

    List<BundleDependency> additionalDependencies =
        applicationDescriptor.getClassLoaderModel().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-http-connector")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(1));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));
  }

  @Test
  public void createApplicationDescriptorWithExportedPackagesAndResourcesInMuleArtifactJson() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/exported-packages-resources-model");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), contains("org.exported-test"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(), contains("exported-test-script.dwl"));
  }

  @Test
  @Issue("W-11261035")
  @Description("Tests whenever a domain has a dependency that is also present in an application either as a dependency " +
      "or a transitive dependency, the application class loader can be correctly created.")
  public void applicationDescriptorWithDomainProvidingAPluginAllowsClassLoadersCreation() throws Exception {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/basic");

    assertThat(domainDescriptor.getClassLoaderModel().getDependencies(), hasSize(1));
    assertThat(domainDescriptor.getPlugins(), contains(hasProperty("name", equalTo("Sockets"))));

    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/with-domain", domainBundleDescriptor -> {
      assertThat(domainBundleDescriptor, equalTo(domainDescriptor.getBundleDescriptor()));
      return domainDescriptor;
    });

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), contains("org.test"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(), hasSize(4));
    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("test-domain")))));

    assertThat(applicationDescriptor.getPlugins(), hasSize(2));
    assertThat(applicationDescriptor.getPlugins(), hasItem(hasProperty("name", equalTo("HTTP"))));
    assertThat(applicationDescriptor.getPlugins(), not(hasItem(hasProperty("name", equalTo("Sockets")))));

    final MuleDeployableArtifactClassLoader domainClassLoader =
        artifactClassLoaderResolver.createDomainClassLoader(domainDescriptor);
    final MuleDeployableArtifactClassLoader applicationClassLoader =
        artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> domainClassLoader);
    final RegionClassLoader regionClassLoader = (RegionClassLoader) applicationClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(hasProperty("name", equalTo("HTTP")), hasProperty("name", equalTo("Database"))));
  }

  @Test
  @Issue("W-11261035")
  public void applicationDescriptorWithDependenciesSharingExportedPackagesAllowsClassLoaderCreation() throws Exception {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/basic");

    assertThat(applicationDescriptor.getPlugins(),
               hasItems(hasProperty("name", equalTo("HTTP")), hasProperty("name", equalTo("Sockets"))));

    // For the class loader creation to be successful, the exported packages from HTTP must not contain the ones from its
    // transitive dependency, Sockets
    final MuleDeployableArtifactClassLoader applicationClassLoader =
        artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor);
    final RegionClassLoader regionClassLoader = (RegionClassLoader) applicationClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), hasItems(hasProperty("name", equalTo("HTTP")), hasProperty("name", equalTo("Sockets"))));
  }

  @Test
  @Issue("W-11261035")
  public void domainDescriptorWithDependenciesSharingExportedPackagesAllowsClassLoaderCreation() throws Exception {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/with-http-dependency");

    assertThat(domainDescriptor.getPlugins(),
               hasItems(hasProperty("name", equalTo("HTTP")), hasProperty("name", equalTo("Sockets"))));

    // For the class loader creation to be successful, the exported packages from HTTP must not contain the ones from its
    // transitive dependency, Sockets
    final MuleDeployableArtifactClassLoader domainClassLoader =
        artifactClassLoaderResolver.createDomainClassLoader(domainDescriptor);
    final RegionClassLoader regionClassLoader = (RegionClassLoader) domainClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), hasItems(hasProperty("name", equalTo("HTTP")), hasProperty("name", equalTo("Sockets"))));
  }

  private DomainDescriptor createDomainDescriptor(String domainPath) throws URISyntaxException {
    DeployableProjectModel model = getDeployableProjectModel(domainPath);

    return deployableArtifactDescriptorFactory.createDomainDescriptor(model, emptyMap());
  }

  private ApplicationDescriptor createApplicationDescriptor(String appPath) throws URISyntaxException {
    return createApplicationDescriptor(appPath, null);
  }

  private ApplicationDescriptor createApplicationDescriptor(String appPath, DomainDescriptorResolver domainDescriptorResolver)
      throws URISyntaxException {
    DeployableProjectModel model = getDeployableProjectModel(appPath);

    return deployableArtifactDescriptorFactory.createApplicationDescriptor(model, emptyMap(),
                                                                           domainDescriptorResolver);
  }

  private DeployableProjectModel getDeployableProjectModel(String deployablePath) throws URISyntaxException {
    MavenDeployableProjectModelBuilder modelFactory =
        new MavenDeployableProjectModelBuilder(getDeployableFolder(deployablePath));

    return modelFactory.build();
  }

  protected File getDeployableFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
