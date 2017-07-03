/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static com.google.common.io.Files.createTempDir;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.tck.MuleTestUtils.testWithSystemProperties;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MavenClassLoaderModelLoaderTestCase {

  public static final String MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION = "muleRuntimeConfig.maven.repositoryLocation";
  private MavenClassLoaderModelLoader mavenClassLoaderModelLoader;

  private File artifactFile;

  @Rule
  public SystemProperty repositoryLocation = new SystemProperty(MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION,
                                                                createTempDir().getAbsolutePath());

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() throws URISyntaxException {
    mavenClassLoaderModelLoader = new MavenClassLoaderModelLoader();
    artifactFile = getApplicationFolder("apps/single-dependency");
  }

  @Test
  public void noMavenConfiguration() throws Exception {
    Map<String, String> properties = getMuleFreeSystemProperties();
    properties.put(repositoryLocation.getName(), repositoryLocation.getValue());
    expectedException.expect(RuntimeException.class);
    expectedException.expectCause(instanceOf(DependencyCollectionException.class));
    testWithSystemProperties(properties, () -> {
      GlobalConfigLoader.reset(); //Change local repository path
      mavenClassLoaderModelLoader.load(artifactFile, emptyMap(), APP);
    });
  }

  @Test
  public void changeMavenConfiguration() throws Exception {
    Map<String, String> properties = getMuleFreeSystemProperties();
    properties.put(repositoryLocation.getName(), repositoryLocation.getValue());
    try {
      testWithSystemProperties(properties, () -> {
        GlobalConfigLoader.reset(); //Change local repository path
        mavenClassLoaderModelLoader.load(artifactFile, emptyMap(), APP);
      });
      fail();
    } catch (Exception e) {
      // It is should fail
    }
    properties.put("muleRuntimeConfig.maven.repositories.mavenCentral.url", "https://repo.maven.apache.org/maven2/");
    range(1, 10).parallel().forEach(number -> {
      try {
        testWithSystemProperties(properties,
                                 () -> {
                                   GlobalConfigLoader.reset();
                                   assertThat(mavenClassLoaderModelLoader.load(artifactFile, emptyMap(), APP).getDependencies(),
                                              hasItem(hasProperty("descriptor",
                                                                  (hasProperty("artifactId", equalTo("commons-collections"))))));
                                 });
      } catch (Exception e) {
        fail(e.getMessage());
      }
    });
  }

  private Map<String, String> getMuleFreeSystemProperties() {
    // Find any System property for muleRuntimeConfig from previous executions...
    final List<String> muleRuntimeConfig =
        System.getProperties().stringPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith("muleRuntimeConfig") && !propertyName.equals(
                                                                                                         MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION))
            .collect(
                     toList());
    Map<String, String> properties = new HashMap<>();
    muleRuntimeConfig.forEach(property -> properties.put(property, null));
    return properties;
  }

  private File getApplicationFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
