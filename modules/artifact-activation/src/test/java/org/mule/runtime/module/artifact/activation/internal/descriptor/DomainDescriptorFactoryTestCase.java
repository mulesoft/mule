/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

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
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Story(ARTIFACT_DESCRIPTORS)
public class DomainDescriptorFactoryTestCase extends AbstractDeployableArtifactDescriptorFactoryTestCase {

  private static final ArtifactClassLoaderResolver artifactClassLoaderResolver =
      ArtifactClassLoaderResolver.defaultClassLoaderResolver();

  @Test
  public void createBasicDomainDescriptor() throws Exception {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/basic");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(3));
    assertThat(domainDescriptor.getClassLoaderConfiguration().isIncludeTestDependencies(), is(false));
  }

  @Test
  public void createDomainDescriptorWithSharedLibrary() throws URISyntaxException {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/shared-lib");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(),
               everyItem(startsWith("org.apache.derby")));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedResources(),
               hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createDomainDescriptorWithTransitiveSharedLibrary() throws URISyntaxException {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/shared-lib-transitive");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(),
               hasItems(startsWith("org.springframework.context"),
                        startsWith("org.springframework.beans"),
                        startsWith("org.springframework.core")));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedResources(),
               hasItems(startsWith("org/springframework/context"),
                        startsWith("org/springframework/beans")));
  }

  @Test
  public void createDomainDescriptorWithAdditionalPluginDependency() throws URISyntaxException {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/additional-plugin-dependency");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(3));

    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    List<BundleDependency> additionalDependencies =
        domainDescriptor.getClassLoaderConfiguration().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-db-connector")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(2));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));
    assertThat(additionalDependencies.get(1).getDescriptor().getArtifactId(), is("derbyshared"));

    ArtifactPluginDescriptor dbPlugin = domainDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Database"))
        .findFirst()
        .get();

    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalPackages(), hasItems(startsWith("org.apache.derby")));
    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalResources(), hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createDomainDescriptorWithAdditionalPluginDependencyAndDependency() throws URISyntaxException {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/additional-plugin-dependency-and-dep");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(5));

    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItems(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("derbyshared"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    List<BundleDependency> additionalDependencies =
        domainDescriptor.getClassLoaderConfiguration().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-db-connector")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(2));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));
    assertThat(additionalDependencies.get(1).getDescriptor().getArtifactId(), is("derbyshared"));

    ArtifactPluginDescriptor dbPlugin = domainDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Database"))
        .findFirst()
        .get();

    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalPackages(), hasItems(startsWith("org.apache.derby")));
    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalResources(), hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createDomainDescriptorWithTransitiveAdditionalPluginDependency() throws URISyntaxException {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/additional-plugin-dependency-transitive");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(1));

    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-spring-module")))));

    List<BundleDependency> additionalDependencies =
        domainDescriptor.getClassLoaderConfiguration().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-spring-module")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(6));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("spring-context"));

    ArtifactPluginDescriptor springPlugin = domainDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Spring"))
        .findFirst()
        .get();

    assertThat(springPlugin.getClassLoaderConfiguration().getLocalPackages(),
               hasItems(startsWith("org.springframework.context"),
                        startsWith("org.springframework.beans"),
                        startsWith("org.springframework.core")));
    assertThat(springPlugin.getClassLoaderConfiguration().getLocalResources(),
               hasItems(startsWith("org/springframework/context"),
                        startsWith("org/springframework/beans")));
  }

  @Test
  public void createDomainDescriptorWithExportedPackagesAndResourcesInMuleArtifactJson() throws URISyntaxException {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/exported-packages-resources-model");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedPackages(), contains("org.exported-test"));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getExportedResources(), contains("exported-test-script.dwl"));
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

  @Test
  public void domainDescriptorWithIncludeTestDependencies() throws Exception {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/include-test-dependencies", true);

    assertThat(domainDescriptor.getClassLoaderConfiguration().isIncludeTestDependencies(), is(true));

    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(1));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-jms-connector")))));
  }

  @Test
  public void domainDescriptorWithoutIncludeTestDependencies() throws Exception {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/do-not-include-test-dependencies");

    assertThat(domainDescriptor.getClassLoaderConfiguration().isIncludeTestDependencies(), is(false));
    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(0));
  }

}
