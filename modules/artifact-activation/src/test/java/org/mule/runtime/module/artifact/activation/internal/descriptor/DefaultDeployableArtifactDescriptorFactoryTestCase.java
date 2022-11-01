/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.net.URISyntaxException;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Story(ARTIFACT_DESCRIPTORS)
public class DefaultDeployableArtifactDescriptorFactoryTestCase extends AbstractDeployableArtifactDescriptorFactoryTestCase {

  private static final ArtifactClassLoaderResolver artifactClassLoaderResolver =
      ArtifactClassLoaderResolver.defaultClassLoaderResolver();

  @Test
  public void createBasicApplicationDescriptor() throws Exception {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/basic");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(3));
  }

  @Test
  public void createApplicationDescriptorWithSharedLibrary() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/shared-lib");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(),
               everyItem(startsWith("org.apache.derby")));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedResources(),
               hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createApplicationDescriptorWithTransitiveSharedLibrary() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/shared-lib-transitive");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(),
               hasItems(startsWith("org.springframework.context"),
                        startsWith("org.springframework.beans"),
                        startsWith("org.springframework.core")));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedResources(),
               hasItems(startsWith("org/springframework/context"),
                        startsWith("org/springframework/beans")));
  }

  @Test
  public void createApplicationDescriptorWithAdditionalPluginDependency() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/additional-plugin-dependency");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(3));

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    List<BundleDependency> additionalDependencies =
        applicationDescriptor.getClassLoaderConfiguration().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-db-connector")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(1));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));

    ArtifactPluginDescriptor dbPlugin = applicationDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Database"))
        .findFirst()
        .get();

    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalPackages(), hasItems(startsWith("org.apache.derby")));
    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalResources(), hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createApplicationDescriptorWithAdditionalPluginDependencyAndDependency() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/additional-plugin-dependency-and-dep");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(4));

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItems(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby"))),
                        hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-db-connector")))));

    List<BundleDependency> additionalDependencies =
        applicationDescriptor.getClassLoaderConfiguration().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-db-connector")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(1));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));

    ArtifactPluginDescriptor dbPlugin = applicationDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Database"))
        .findFirst()
        .get();

    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalPackages(), hasItems(startsWith("org.apache.derby")));
    assertThat(dbPlugin.getClassLoaderConfiguration().getLocalResources(), hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createApplicationDescriptorWithTransitiveAdditionalPluginDependency() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/additional-plugin-dependency-transitive");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(1));

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-spring-module")))));

    List<BundleDependency> additionalDependencies =
        applicationDescriptor.getClassLoaderConfiguration().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-spring-module")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(6));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("spring-context"));

    ArtifactPluginDescriptor springPlugin = applicationDescriptor.getPlugins()
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
  public void createApplicationDescriptorWithExportedPackagesAndResourcesInMuleArtifactJson() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/exported-packages-resources-model");

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(), contains("org.exported-test"));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedResources(), contains("exported-test-script.dwl"));
  }

  @Test
  @Issue("W-11261035")
  @Description("Tests whenever a domain has a dependency that is also present in an application either as a dependency " +
      "or a transitive dependency, the application class loader can be correctly created.")
  public void applicationDescriptorWithDomainProvidingAPluginAllowsClassLoadersCreation() throws Exception {
    DomainDescriptor domainDescriptor = createDomainDescriptor("domains/basic");

    assertThat(domainDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(1));
    assertThat(domainDescriptor.getPlugins(), contains(hasProperty("name", equalTo("Sockets"))));

    ApplicationDescriptor applicationDescriptor =
        createApplicationDescriptor("apps/with-domain", (domainName, domainBundleDescriptor) -> {
          assertThat(domainBundleDescriptor, equalTo(domainDescriptor.getBundleDescriptor()));
          return domainDescriptor;
        });

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getExportedResources(), hasSize(0));

    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(4));
    assertThat(applicationDescriptor.getClassLoaderConfiguration().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("basic")))));

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

}
